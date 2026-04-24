package mycpu.core.components

import chisel3._
import chisel3.util._
import mycpu.common._
import mycpu.core.bundles._
import mycpu.dpi.{DpiApi, SimStateBundle}
import mycpu.common.Instructions

class Tracer(enableDpi: Boolean = false, enableFlushDpi: Boolean = false) extends Module {
  val io = IO(new Bundle {
    val commitTrace = Input(Valid(new TraceCarryBundle))
    val regsFlat = Input(UInt(1024.W))
    val mtvec = Input(UInt(XLEN.W))
    val mepc = Input(UInt(XLEN.W))
    val mstatus = Input(UInt(XLEN.W))
    val mcause = Input(UInt(XLEN.W))
    val flush = Input(Bool())
    val trace = Output(new CoreTraceBundle)
  })

  val retireCountReg = RegInit(0.U(32.W))
  val lastRetireReg = RegInit(0.U.asTypeOf(Valid(new TraceCarryBundle)))
  val branchCountReg = RegInit(0.U(32.W))
  val branchCorrectCountReg = RegInit(0.U(32.W))

  when(io.commitTrace.valid) {
    retireCountReg := retireCountReg + 1.U
    lastRetireReg := io.commitTrace
  }

  when(io.commitTrace.valid && io.commitTrace.bits.branchResolved) {
    branchCountReg := branchCountReg + 1.U
    when(io.commitTrace.bits.branchCorrect) {
      branchCorrectCountReg := branchCorrectCountReg + 1.U
    }
  }

  val simState = Wire(new SimStateBundle)
  simState.valid := io.commitTrace.valid
  simState.pc := io.commitTrace.bits.pc
  simState.dnpc := io.commitTrace.bits.dnpc
  simState.regWen := io.commitTrace.bits.regWen
  simState.regAddr := io.commitTrace.bits.rd
  simState.regData := io.commitTrace.bits.data
  simState.regsFlat := io.regsFlat
  simState.mtvec := io.mtvec
  simState.mepc := io.mepc
  simState.mstatus := io.mstatus
  simState.mcause := io.mcause
  simState.inst := io.commitTrace.bits.inst

  if (enableDpi) {
    DpiApi.simState(clock, reset.asBool, simState, localName = "core_sim_state")
    DpiApi.simEbreak(
      valid = io.commitTrace.valid && io.commitTrace.bits.inst === Instructions.EBREAK.value.U,
      isEbreak = 1.U(32.W),
      localName = "core_sim_ebreak",
    )
    DpiApi.difftestSkip(clock, false.B, localName = "core_difftest_skip")
    if (enableFlushDpi) {
      DpiApi.recordFlush(clock, reset.asBool, io.flush, localName = "core_flush")
    }
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
