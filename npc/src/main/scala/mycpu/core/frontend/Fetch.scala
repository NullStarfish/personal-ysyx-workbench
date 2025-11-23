package mycpu.core.frontend

import chisel3._
import chisel3.util._
import mycpu.common._
import mycpu.core.bundles._
import mycpu.memory.AXI4LiteMasterIO

class Fetch extends Module {
  val io = IO(new Bundle {
    val axi      = new AXI4LiteMasterIO()
    val redirect = Flipped(Valid(UInt(XLEN.W)))
    val out      = Decoupled(new FetchPacket())

    val debug_pc = Output(UInt(XLEN.W)) // 新增
  })

  val pc = RegInit(START_ADDR.U(XLEN.W))
  val pcNext = WireInit(pc + 4.U)

  io.debug_pc := pc // 连接内部寄存器

  // 处理跳转
  when(io.redirect.valid) {
    pc := io.redirect.bits
  }.elsewhen(io.axi.ar.fire) {
    pc := pcNext
  }

  // === 1. AXI Read Channel (AR) ===
  val reqSent = RegInit(false.B)
  
  io.axi.ar.valid     := !reqSent && !reset.asBool
  io.axi.ar.bits.addr := pc
  io.axi.ar.bits.prot := 0.U // [修复] 初始化 prot
  
  when (io.axi.ar.fire) { reqSent := true.B }

  // === 2. AXI Write Channels (AW, W, B) - Fetch 是只读的，必须置 0 ===
  io.axi.aw.valid     := false.B
  io.axi.aw.bits.addr := 0.U
  io.axi.aw.bits.prot := 0.U
  
  io.axi.w.valid      := false.B
  io.axi.w.bits.data  := 0.U
  io.axi.w.bits.strb  := 0.U
  
  io.axi.b.ready      := false.B

  // === 3. Data & Queue ===
  val queue = Module(new Queue(new FetchPacket, entries = 2))
  
  // 只要 Queue 能收，我们就 Ready 接收 AXI R 通道数据
  io.axi.r.ready := queue.io.enq.ready 
  
  queue.io.enq.valid     := io.axi.r.valid && !io.redirect.valid
  queue.io.enq.bits.inst := io.axi.r.bits.data
  queue.io.enq.bits.pc   := pc 
  // [修复] 初始化 isException
  queue.io.enq.bits.isException := false.B 
  
  when (io.axi.r.fire || io.redirect.valid) {
      reqSent := false.B 
  }
  
  // 连接 Output
  io.out <> queue.io.deq
}