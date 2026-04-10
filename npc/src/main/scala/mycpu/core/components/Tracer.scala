package mycpu.core.components

import chisel3._
import mycpu.common._
import mycpu.core.bundles._

class Tracer extends Module {
  val io = IO(new Bundle {
    val ifValid = Input(Bool())
    val idValid = Input(Bool())
    val exValid = Input(Bool())
    val memValid = Input(Bool())
    val retire = Input(new RetireEventBundle)
    val trace = Output(new CoreTraceBundle)
  })

  val retireCountReg = RegInit(0.U(32.W))
  val lastRetireReg = RegInit(0.U.asTypeOf(new RetireEventBundle))

  when(io.retire.valid) {
    retireCountReg := retireCountReg + 1.U
    lastRetireReg := io.retire
  }

  io.trace.ifValid := io.ifValid
  io.trace.idValid := io.idValid
  io.trace.exValid := io.exValid
  io.trace.memValid := io.memValid
  io.trace.retireCount := retireCountReg
  io.trace.lastRetire := lastRetireReg
}
