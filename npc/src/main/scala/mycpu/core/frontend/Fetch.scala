package mycpu.core.frontend

import chisel3._
import chisel3.util._
import mycpu.common._
import mycpu.core.bundles._
import mycpu.utils._

class Fetch extends Module {
  val io = IO(new Bundle {
    val axi          = new AXI4LiteBundle(XLEN, XLEN) // 完整接口
    val next_pc      = Input(UInt(XLEN.W))
    val pc_update_en = Input(Bool()) 
    val out          = Decoupled(new FetchPacket()) 
  })

  // 1. 实例化 ReadBridge (只有读端口)
  val readBridge = Module(new AXI4LiteReadBridge(XLEN, XLEN))

  // 2. [魔法] 使用 unapply 拆分总线
  // rBus 是一个 Wire(AXI4LiteReadBundle)
  // wBus 是一个 Wire(AXI4LiteWriteBundle)
  val AXI4Split(rBus, wBus) = io.axi

  // 3. 连接逻辑
  // 连接读通道
  readBridge.io.axi <> rBus

  // 处理未使用的写通道 (因为 Fetch 是只读 Master)
  // 必须显式置低 Wire 的输出端，以驱动 io.axi 的输出
  wBus.aw.valid := false.B
  wBus.aw.bits  := DontCare
  wBus.w.valid  := false.B
  wBus.w.bits   := DontCare
  wBus.b.ready  := false.B

  // ==============================================================================
  // 核心业务逻辑
  // ==============================================================================
  val pc = RegInit(START_ADDR.U(XLEN.W))
  val reqSent = RegInit(false.B) 

  when (io.pc_update_en) {
    pc := io.next_pc
    reqSent := false.B 
  }

  // 发送请求
  readBridge.io.req.valid     := !reqSent
  readBridge.io.req.bits.addr := pc

  when (readBridge.io.req.fire) {
    reqSent := true.B
  }

  // 接收响应
  io.out.valid            := readBridge.io.resp.valid
  io.out.bits.inst        := readBridge.io.resp.bits.rdata
  io.out.bits.pc          := pc
  io.out.bits.dnpc        := pc + 4.U
  io.out.bits.isException := readBridge.io.resp.bits.isError


  

  
  readBridge.io.resp.ready := io.out.ready
}