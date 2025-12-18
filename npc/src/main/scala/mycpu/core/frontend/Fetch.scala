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

  val readBridge = Module(new AXI4ReadBridge(XLEN, XLEN))

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

  val instReq = WireDefault(new AXI4BundleA(AXI_ID_WIDTH, XLEN), {
    val w = Wire(new AXI4BundleA(AXI_ID_WIDTH, XLEN))
    // 这里设置通用的“安全”默认值
    w.id    := 0.U
    w.addr  := pc
    w.len   := 0.U
    w.size  := 2.U
    w.burst := 0.U
    w.lock  := false.B
    w.cache := 0.U
    w.prot  := 0.U
    w.qos   := 0.U
    w // 返回这个 wire
  })
  

  readBridge.io.rReq.valid     := !reqSent
  readBridge.io.rReq.bits      := instReq

  when (readBridge.io.rReq.fire) { reqSent := true.B }

  

  io.out.valid            := readBridge.io.rStream.valid
  io.out.bits.inst        := readBridge.io.rStream.bits.data
  io.out.bits.pc          := pc
  io.out.bits.dnpc        := pc + 4.U
  io.out.bits.isException := readBridge.io.rStream.bits.resp =/= 0.U
  
  readBridge.io.rStream.ready := io.out.ready
}