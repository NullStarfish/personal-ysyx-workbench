package mycpu.core.frontend

import chisel3._
import chisel3.util._
import mycpu.common._
import mycpu.core.bundles._
import mycpu.utils._

class Fetch extends Module {
  val io = IO(new Bundle {
    val axi          = new AXI4LiteBundle(XLEN, XLEN) // 外部接口统一为 Lite Bundle
    val next_pc      = Input(UInt(XLEN.W))
    val pc_update_en = Input(Bool()) 
    val out          = Decoupled(new FetchPacket()) 
  })

  val readBridge = Module(new AXI4LiteReadBridge(XLEN, XLEN))

  // [魔法拆分] 
  val AXI4Split(rBus, wBus) = io.axi

  // 连接读通道
  readBridge.io.axi <> rBus

  // 屏蔽写通道 (Fetch 不写内存)
  wBus.aw.valid := false.B
  wBus.aw.bits  := DontCare
  wBus.w.valid  := false.B
  wBus.w.bits   := DontCare
  wBus.b.ready  := false.B

  // ... 剩余 Fetch 逻辑保持不变 ...
  val pc = RegInit(START_ADDR.U(XLEN.W))
  val reqSent = RegInit(false.B) 

  when (io.pc_update_en) {
    pc := io.next_pc
    reqSent := false.B 
  }

  readBridge.io.req.valid     := !reqSent
  readBridge.io.req.bits.addr := pc

  when (readBridge.io.req.fire) { reqSent := true.B }

  io.out.valid            := readBridge.io.resp.valid
  io.out.bits.inst        := readBridge.io.resp.bits.rdata
  io.out.bits.pc          := pc
  io.out.bits.dnpc        := pc + 4.U
  io.out.bits.isException := readBridge.io.resp.bits.isError
  
  readBridge.io.resp.ready := io.out.ready
}