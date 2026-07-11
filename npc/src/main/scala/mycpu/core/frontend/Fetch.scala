package mycpu.core.frontend

import chisel3._
import chisel3.util._
import mycpu.common._
import mycpu.core.bundles.IFPacket

class Fetch(
    enableTraceFields: Boolean = ENABLE_TRACE_FIELDS,
    enableDpi: Boolean = false,
) extends Module {
  val io = IO(new Bundle {
    val block = Input(Bool())
    val out = Decoupled(new IFPacket)
    val redirect = Input(Valid(UInt(XLEN.W)))
  })

  val pc = RegInit(START_ADDR.U(XLEN.W))

  // Fetch是组合valid。只有I$0真正接收lookup时，PC才向前移动。
  io.out.valid := !reset.asBool && !io.redirect.valid && !io.block
  io.out.bits.pc := pc

  when(io.redirect.valid) {
    pc := io.redirect.bits
  }.elsewhen(io.out.fire) {
    pc := pc + 4.U
  }
}
