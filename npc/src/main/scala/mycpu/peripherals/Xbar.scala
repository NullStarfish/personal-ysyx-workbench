package mycpu.peripherals

import chisel3._
import chisel3.util._
import mycpu.utils._
import mycpu.DeviceConfig
import mycpu.common.XLEN


//Xbar实际上是一种switch
//ifu, lsu发送的数据包实际上已经带有标识符（就是内存地址本身）
//Xbar分配这些数据包，并且对这个数据包进行解码，解码成offset

class Xbar(devices: List[DeviceConfig]) extends Module {
  val numSlaves = devices.length
  require(numSlaves > 0, "Xbar must have at least one slave device")

  val io = IO(new Bundle {
    val in = Flipped(new AXI4LiteBundle(XLEN, XLEN))
    val slaves = Vec(numSlaves, new AXI4LiteBundle(XLEN, XLEN))
  })

  def isHit(addr: UInt, dev: DeviceConfig): Bool = {
    addr >= dev.startAddr.U && addr < dev.endAddr.U
  }

  // ==================================================================
  // 1. 全局默认赋值 (关键修复)
  // ==================================================================
  // 防止 "not fully initialized" 错误。先给所有 Slave 接口赋默认值。
  for (i <- 0 until numSlaves) {
    // Master -> Slave 的前向信号 (AR, AW, W)
    io.slaves(i).ar.valid := false.B
    io.slaves(i).ar.bits  := io.in.ar.bits // 数据可以直连，valid 为 false 即可
    
    io.slaves(i).aw.valid := false.B
    io.slaves(i).aw.bits  := io.in.aw.bits
    
    io.slaves(i).w.valid  := false.B
    io.slaves(i).w.bits   := io.in.w.bits

    // Master -> Slave 的反向信号 (R ready, B ready)
    // 如果不选该 slave，绝不能置高 ready，否则 slave 发的数据会被丢弃
    io.slaves(i).r.ready := false.B
    io.slaves(i).b.ready := false.B
  }

  // Master (CPU) 侧的默认响应
  io.in.ar.ready := false.B
  io.in.aw.ready := false.B
  io.in.w.ready  := false.B
  
  io.in.r.valid  := false.B
  io.in.r.bits   := DontCare
  
  io.in.b.valid  := false.B
  io.in.b.bits   := DontCare







  when(io.in.ar.fire) {
    Debug.log("[DEBUG] [Xbar]: slave ar fire\n")
  }

  when(io.in.r.fire) {
    Debug.log("[DEBUG] [Xbar]: read received: data: %x, resp: %x\n", io.in.r.bits.data, io.in.r.bits.resp)
  }


  // ==================================================================
  // 2. 读通道逻辑 (AR & R)
  // ==================================================================
  val ar_hits = VecInit(devices.map(dev => isHit(io.in.ar.bits.addr, dev)))
  val ar_req_valid = io.in.ar.valid


  when(ar_req_valid) {
    Debug.log("[DEBUG] [Xbar]: ar req: numslaves: %x  ar_hits: %x\n", numSlaves.asUInt, ar_hits.asUInt)
  }

  // 2.1 AR 分发 (覆盖上面的默认值)
  for (i <- 0 until numSlaves) {
    when(ar_hits(i) && ar_req_valid) {
      io.slaves(i).ar.valid := true.B
    }
  }

  // 2.2 AR Ready 聚合
  io.in.ar.ready := Mux1H(ar_hits, io.slaves.map(_.ar.ready))

  // 2.3 R 通道状态机
  val r_slave_sel = RegInit(0.U(log2Ceil(numSlaves).max(1).W)) 
  val r_busy      = RegInit(false.B)

  when(io.in.ar.fire) {
    r_slave_sel := OHToUInt(ar_hits)
    r_busy      := true.B
  }
  when(io.in.r.fire) {
    r_busy := false.B
  }

  // 2.4 R 通道连接
  when(r_busy) {
    // 建立双向连接。注意：
    // io.slaves(sel).r.ready := io.in.r.ready 会被生成
    // 这覆盖了上面循环中的 io.slaves(i).r.ready := false.B
    io.in.r <> io.slaves(r_slave_sel).r
  }

  // ==================================================================
  // 3. 写通道逻辑 (AW & W & B)
  // ==================================================================

  when(io.in.aw.fire) {
    Debug.log("[DEBUG] [Xbar]: write addr req: waddr: %x, proc: %x\n", io.in.aw.bits.addr, io.in.aw.bits.prot)
  }
  when(io.in.w.fire) {
    Debug.log("[DEBUG] [Xbar]: write data req: wdata: %x, strb: %x\n", io.in.w.bits.data, io.in.w.bits.strb)
  }

  when(io.in.b.fire) {
    Debug.log("[DEBUG] [Xbar]: write success: resp: %x\n", io.in.b.bits.resp)
  }




  val aw_hits = VecInit(devices.map(dev => isHit(io.in.aw.bits.addr, dev)))
  val aw_req_valid = io.in.aw.valid


  val w_slave_sel = RegInit(0.U(log2Ceil(numSlaves).max(1).W))
  val w_busy      = RegInit(false.B)

  when(io.in.aw.fire) {
    w_slave_sel := OHToUInt(aw_hits)
    w_busy      := true.B
  }
  when(io.in.b.fire) {
    w_busy := false.B
  }

  // 1. 类型统一：将 aw_hits (Vec) 转为 UInt，以便和 UIntToOH 兼容
  val aw_hits_uint = aw_hits.asUInt 
  val sel_oh_uint  = UIntToOH(w_slave_sel)

  // 2. Mux 选择：现在两个输入都是 UInt 类型了
  val w_target_oh = Mux(w_busy, sel_oh_uint, aw_hits_uint)

  // 3. 遍历 Slaves 进行连线
  // 使用你建议的 zipWithIndex foreach 写法，更加 Scala 风格
  io.slaves.zipWithIndex.foreach { case (slave, i) =>
    // 3.1 AW 分发 (AW 只看当前地址命中情况)
    when(aw_hits(i) && aw_req_valid) {
      slave.aw.valid := true.B
    }

    // 3.2 W 分发 (看路由结果)
    // w_target_oh 是 UInt，使用 (i) 进行按位提取(Bit Extraction)，得到 Bool
    when(w_target_oh(i)) {
      slave.w.valid := io.in.w.valid
    }
  }

  // 3.2 Ready 聚合 (注意这里也要用 w_target_oh)
  // map 返回的是 Seq[Bool]，Mux1H 支持接收 (UInt, Seq[Bool])
  io.in.aw.ready := Mux1H(aw_hits, io.slaves.map(_.aw.ready))
  io.in.w.ready  := Mux1H(w_target_oh, io.slaves.map(_.w.ready))

  // 3.4 B 通道连接
  when(w_busy) {
    io.in.b <> io.slaves(w_slave_sel).b
  }
}