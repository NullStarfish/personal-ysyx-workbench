package mycpu.peripherals

import chisel3._
import chisel3.util._
import mycpu._
import mycpu.utils._
import mycpu.common.XLEN

class Serial extends Peripheral(MemMap.devices(1)) { // 假设 Index 1 是 Serial
  
  // 1. 拆分 AXI 总线
  // AXI4Split 会创建 Wire 并进行连接，io.bus 被接管
  val AXI4Split(rBus, wBus) = io.bus

  // 2. 实例化更新后的 Slave Bridges
  val readBridge  = Module(new AXI4LiteReadSlaveBridge(XLEN, XLEN))
  val writeBridge = Module(new AXI4LiteWriteSlaveBridge(XLEN, XLEN))

  // 3. 连接
  readBridge.io.axi  <> rBus
  writeBridge.io.axi <> wBus

  // ==============================================================================
  // 读逻辑
  // ==============================================================================
  readBridge.io.req.ready := true.B
  
  val rValid = RegNext(readBridge.io.req.valid, init = false.B)

  readBridge.io.resp.valid        := rValid
  readBridge.io.resp.bits.rdata   := 0.U      // 串口读为0
  readBridge.io.resp.bits.isError := false.B

  // ==============================================================================
  // 写逻辑
  // ==============================================================================
  writeBridge.io.req.ready := true.B

  val reqValid = writeBridge.io.req.valid
  val reqAddr  = writeBridge.io.req.bits.addr
  val reqData  = writeBridge.io.req.bits.wdata
  val reqStrb  = writeBridge.io.req.bits.wstrb
  
  // 使用 Bridge 输出的 reqAddr 计算局部偏移
  val offset = reqAddr(localAddrWidth - 1, 0)

  when(reqValid) {
    Debug.log("[Serial] Write: addr=%x data=%x\n", reqAddr, reqData)
    // 偏移量0为数据寄存器
    when(offset === 0.U) {
      val maskedBytes = VecInit(Seq.tabulate(4) { i =>
        Mux(reqStrb(i), reqData(i * 8 + 7, i * 8), 0.U(8.W))
      })
      val finalData = maskedBytes.asUInt
      
      // 打印字符
      printf("%c", finalData(7, 0))
    }
  }

  val wValid = RegNext(reqValid, init = false.B)
  writeBridge.io.resp.valid        := wValid
  writeBridge.io.resp.bits.isError := false.B
}