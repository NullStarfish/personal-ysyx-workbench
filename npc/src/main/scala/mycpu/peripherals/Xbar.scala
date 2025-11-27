package mycpu.peripherals

import chisel3._
import chisel3.util._
import mycpu.common._ // 假设这里有 AXI4LiteMasterIO 定义
import mycpu.utils._
import mycpu.{DeviceConfig, MemMap}

class Xbar(devices: List[DeviceConfig]) extends Module {
  val numSlaves = devices.length
  val io = IO(new Bundle {
    val in = Flipped(new AXI4LiteBundle())       // Master 接口 (CPU侧)
    val slaves = Vec(numSlaves, new AXI4LiteBundle()) // Slaves 接口 (外设侧)
  })

  // ==================================================================
  // 1. 读通道逻辑 (Read Address & Read Data)
  // ==================================================================

  // 生成读地址命中信号 (Vec of Bool)
  // 遍历 devices 列表，生成硬件比较逻辑
  val ar_hits = VecInit(devices.map { dev => 
    io.in.ar.bits.addr >= dev.startAddr.U && io.in.ar.bits.addr < dev.endAddr.U
  })
  
  // 如果没有命中任何 Slave，ar_valid 无效（或者可以导向一个 Error Slave，这里简化处理）
  val ar_valid_masked = io.in.ar.valid && ar_hits.asUInt.orR

  // 1.1 AR 通道分发 (Master -> Slaves)
  for (i <- 0 until numSlaves) {
    io.slaves(i).ar.valid := io.in.ar.valid && ar_hits(i)
    io.slaves(i).ar.bits  := io.in.ar.bits
    // 只有当该 slave 被选中且 valid 时，才将其 ready 传回
  }

  // 1.2 AR Ready 聚合 (Slaves -> Master)
  // 使用 Mux1H 选择当前命中的那个 Slave 的 ready 信号
  io.in.ar.ready := Mux1H(ar_hits, io.slaves.map(_.ar.ready))

  // 1.3 R 通道返回 (Slaves -> Master)
  // 我们需要记住是谁接受了 AR 请求，以便在 R 通道接收数据
  // 简单的状态机：Idle -> Reading
  val r_slave_sel = RegInit(0.U(log2Ceil(numSlaves).W))
  val r_busy      = RegInit(false.B)

  // 当握手成功 (fire) 时，记录下是哪个 slave
  when(io.in.ar.fire) {
    r_slave_sel := OHToUInt(ar_hits) // 将 OneHot 转为索引
    r_busy      := true.B
  }
  
  // 当读数据握手完成 (r.fire) 且是最后一笔数据 (axi-lite通常就是一笔)，结束忙碌
  when(io.in.r.fire) {
    r_busy := false.B
  }

  // 根据记录的索引，选择对应的 slave r 通道连接到 master
  io.in.r <> io.slaves(r_slave_sel).r

  // 安全措施：如果不在忙碌状态，不要让 master 看到 valid 信号 (可选)
  when(!r_busy) { io.in.r.valid := false.B }


  // ==================================================================
  // 2. 写通道逻辑 (Write Address, Write Data, Write Response)
  // ==================================================================
  
  // 生成写地址命中信号
  val aw_hits = VecInit(devices.map { dev => 
    io.in.aw.bits.addr >= dev.startAddr.U && io.in.aw.bits.addr < dev.endAddr.U
  })

  // 2.1 AW & W 通道分发 (Master -> Slaves)
  // AXI4-Lite 通常要求 AW 和 W 都要握手。这里简单起见，假设它们目标一致。
  for (i <- 0 until numSlaves) {
    io.slaves(i).aw.valid := io.in.aw.valid && aw_hits(i)
    io.slaves(i).aw.bits  := io.in.aw.bits
    
    // W 通道通常跟随 AW 通道的选择
    // 注意：标准的 AXI Xbar 这里会更复杂，需要处理 W 可能比 AW 早到的情况
    // 这里假设是一个简单的核，W 总是伴随 AW 发送
    io.slaves(i).w.valid  := io.in.w.valid && aw_hits(i) 
    io.slaves(i).w.bits   := io.in.w.bits
  }

  // 2.2 Ready 聚合
  io.in.aw.ready := Mux1H(aw_hits, io.slaves.map(_.aw.ready))
  io.in.w.ready  := Mux1H(aw_hits, io.slaves.map(_.w.ready))

  // 2.3 B 通道返回 (Write Response)
  val w_slave_sel = RegInit(0.U(log2Ceil(numSlaves).W))
  val w_busy      = RegInit(false.B)

  when(io.in.aw.fire) {
    w_slave_sel := OHToUInt(aw_hits)
    w_busy      := true.B
  }

  when(io.in.b.fire) {
    w_busy := false.B
  }

  io.in.b <> io.slaves(w_slave_sel).b
  when(!w_busy) { io.in.b.valid := false.B }
}