package mycpu.core.components

import chisel3._
import chisel3.util._

class FlushableStage[T <: Data](gen: T) extends Module {
  val io = IO(new Bundle {
    val enq = Flipped(Decoupled(gen))
    val deq = Decoupled(gen)
    val flush = Input(Bool())
  })

  val validReg = RegInit(false.B)
  val bitsReg = Reg(gen)

  io.enq.ready := !validReg || io.deq.ready
  io.deq.valid := validReg
  io.deq.bits := bitsReg

  when(io.flush) {
    validReg := false.B
  }.elsewhen(io.enq.fire) {
    bitsReg := io.enq.bits
    validReg := true.B
  }.elsewhen(io.deq.fire) {
    validReg := false.B
  }
}
