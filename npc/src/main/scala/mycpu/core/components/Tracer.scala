package mycpu.core.components

import chisel3._
import chisel3.util._
import mycpu.common._
import mycpu.core.bundles._
import mycpu.dpi.{DpiApi, SimStateBundle}
import mycpu.common.Instructions

class Tracer(enableDpi: Boolean = false) extends Module {
  val io = IO(new Bundle {
    val commitTrace = Input(Valid(new TraceCarryBundle))
    val retire = Input(new RetireEventBundle)
    val regsFlat = Input(UInt(1024.W))
    val mtvec = Input(UInt(XLEN.W))
    val mepc = Input(UInt(XLEN.W))
    val mstatus = Input(UInt(XLEN.W))
    val mcause = Input(UInt(XLEN.W))
    val trace = Output(new CoreTraceBundle)
  })

  val retireCountReg = RegInit(0.U(32.W))
  val lastRetireReg = RegInit(0.U.asTypeOf(new RetireEventBundle))
  val branchCountReg = RegInit(0.U(32.W))
  val branchCorrectCountReg = RegInit(0.U(32.W))

  when(io.retire.valid) {
    retireCountReg := retireCountReg + 1.U
    lastRetireReg := io.retire
  }

  when(io.commitTrace.valid && io.commitTrace.bits.branchResolved) {
    branchCountReg := branchCountReg + 1.U
    when(io.commitTrace.bits.branchCorrect) {
      branchCorrectCountReg := branchCorrectCountReg + 1.U
    }
  }

  val simState = Wire(new SimStateBundle)
  simState.valid := io.retire.valid
  simState.pc := io.retire.pc
  simState.dnpc := io.retire.dnpc
  simState.regWen := io.retire.regWen
  simState.regAddr := io.retire.rd
  simState.regData := io.retire.data
  simState.regsFlat := io.regsFlat
  simState.mtvec := io.mtvec
  simState.mepc := io.mepc
  simState.mstatus := io.mstatus
  simState.mcause := io.mcause
  simState.inst := io.retire.inst

  if (enableDpi) {
    DpiApi.simState(clock, reset.asBool, simState, localName = "core_sim_state")
    DpiApi.simEbreak(
      valid = io.retire.valid && io.retire.inst === Instructions.EBREAK.value.U,
      isEbreak = 1.U(32.W),
      localName = "core_sim_ebreak",
    )
    DpiApi.difftestSkip(clock, false.B, localName = "core_difftest_skip")
  }

  io.trace.ifValid := io.commitTrace.valid && io.commitTrace.bits.ifValid
  io.trace.idValid := io.commitTrace.valid && io.commitTrace.bits.idValid
  io.trace.exValid := io.commitTrace.valid && io.commitTrace.bits.exValid
  io.trace.memValid := io.commitTrace.valid && io.commitTrace.bits.memValid
  io.trace.retireCount := retireCountReg
  io.trace.lastRetire := lastRetireReg
  io.trace.branchCount := branchCountReg
  io.trace.branchCorrectCount := branchCorrectCountReg
}
