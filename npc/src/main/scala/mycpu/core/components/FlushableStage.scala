package mycpu.core.components

import chisel3._
import chisel3.util._

private[components] class PayloadReg[T <: Data](gen: T) extends Module {
  val io = IO(new Bundle {
    val en = Input(Bool())
    val in = Input(gen)
    val out = Output(gen)
  })

  val bitsReg = Reg(gen)
  when(io.en) {
    bitsReg := io.in
  }
  io.out := bitsReg
}

private[components] class ValidReg extends Module {
  val io = IO(new Bundle {
    val flush = Input(Bool())
    val enqFire = Input(Bool())
    val deqFire = Input(Bool())
    val out = Output(Bool())
  })

  val validReg = RegInit(false.B)

  when(io.flush) {
    validReg := false.B
  }.elsewhen(io.enqFire) {
    validReg := true.B
  }.elsewhen(io.deqFire) {
    validReg := false.B
  }

  io.out := validReg
}

class FlushableStage[T <: Data](gen: T) extends Module {
  val io = IO(new Bundle {
    val enq = Flipped(Decoupled(gen))
    val deq = Decoupled(gen)
    val flush = Input(Bool())
  })

  val payload = Module(new PayloadReg(gen))
  val valid = Module(new ValidReg)
  val canAccept = !valid.io.out || io.deq.ready

  io.enq.ready := canAccept
  io.deq.valid := valid.io.out
  io.deq.bits := payload.io.out
  payload.io.en := io.enq.fire
  payload.io.in := io.enq.bits
  valid.io.flush := io.flush
  valid.io.enqFire := io.enq.fire
  valid.io.deqFire := io.deq.fire
}
