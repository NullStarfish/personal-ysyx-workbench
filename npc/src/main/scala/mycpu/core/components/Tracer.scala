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
    val decodeAccepted = Input(Bool())
    val exuComplete = Input(Bool())
    val loadUseStall = Input(Bool())
    val fetchPerf = Input(new FetchPerfBundle)
    val lsuPerf = Input(new LsuPerfBundle)
    val trace = Output(new CoreTraceBundle)
  })

  val cycleCountReg = RegInit(0.U(32.W))
  val retireCountReg = RegInit(0.U(32.W))
  val lastRetireReg = RegInit(0.U.asTypeOf(Valid(new TraceCarryBundle)))
  val branchCountReg = RegInit(0.U(32.W))
  val branchCorrectCountReg = RegInit(0.U(32.W))
  val ifuReqCountReg = RegInit(0.U(32.W))
  val ifuRespCountReg = RegInit(0.U(32.W))
  val ifuWaitCycleCountReg = RegInit(0.U(32.W))
  val ifuBlockedByPendingCountReg = RegInit(0.U(32.W))
  val ifuBlockedByOutValidCountReg = RegInit(0.U(32.W))
  val ifuBlockedByStallCountReg = RegInit(0.U(32.W))
  val ifuBlockedByRedirectCountReg = RegInit(0.U(32.W))
  val decodeAcceptedCountReg = RegInit(0.U(32.W))
  val exuCompleteCountReg = RegInit(0.U(32.W))
  val lsuLoadReqCountReg = RegInit(0.U(32.W))
  val lsuLoadRespCountReg = RegInit(0.U(32.W))
  val lsuLoadWaitCycleCountReg = RegInit(0.U(32.W))
  val lsuStoreReqCountReg = RegInit(0.U(32.W))
  val lsuStoreRespCountReg = RegInit(0.U(32.W))
  val lsuStoreWaitCycleCountReg = RegInit(0.U(32.W))
  val lsuPassThroughCountReg = RegInit(0.U(32.W))
  val loadUseStallCountReg = RegInit(0.U(32.W))
  val redirectFlushCountReg = RegInit(0.U(32.W))
  val computeInstCountReg = RegInit(0.U(32.W))
  val memoryInstCountReg = RegInit(0.U(32.W))
  val loadInstCountReg = RegInit(0.U(32.W))
  val storeInstCountReg = RegInit(0.U(32.W))
  val branchInstCountReg = RegInit(0.U(32.W))
  val jumpInstCountReg = RegInit(0.U(32.W))
  val csrInstCountReg = RegInit(0.U(32.W))
  val systemInstCountReg = RegInit(0.U(32.W))

  cycleCountReg := cycleCountReg + 1.U

  when(io.fetchPerf.reqFire) { ifuReqCountReg := ifuReqCountReg + 1.U }
  when(io.fetchPerf.respFire) { ifuRespCountReg := ifuRespCountReg + 1.U }
  when(io.fetchPerf.waitCycle) { ifuWaitCycleCountReg := ifuWaitCycleCountReg + 1.U }
  when(io.fetchPerf.blockedByPending) { ifuBlockedByPendingCountReg := ifuBlockedByPendingCountReg + 1.U }
  when(io.fetchPerf.blockedByOutValid) { ifuBlockedByOutValidCountReg := ifuBlockedByOutValidCountReg + 1.U }
  when(io.fetchPerf.blockedByStall) { ifuBlockedByStallCountReg := ifuBlockedByStallCountReg + 1.U }
  when(io.fetchPerf.blockedByRedirect) { ifuBlockedByRedirectCountReg := ifuBlockedByRedirectCountReg + 1.U }
  when(io.decodeAccepted) { decodeAcceptedCountReg := decodeAcceptedCountReg + 1.U }
  when(io.exuComplete) { exuCompleteCountReg := exuCompleteCountReg + 1.U }
  when(io.lsuPerf.loadReqFire) { lsuLoadReqCountReg := lsuLoadReqCountReg + 1.U }
  when(io.lsuPerf.loadRespFire) { lsuLoadRespCountReg := lsuLoadRespCountReg + 1.U }
  when(io.lsuPerf.loadWaitCycle) { lsuLoadWaitCycleCountReg := lsuLoadWaitCycleCountReg + 1.U }
  when(io.lsuPerf.storeReqFire) { lsuStoreReqCountReg := lsuStoreReqCountReg + 1.U }
  when(io.lsuPerf.storeRespFire) { lsuStoreRespCountReg := lsuStoreRespCountReg + 1.U }
  when(io.lsuPerf.storeWaitCycle) { lsuStoreWaitCycleCountReg := lsuStoreWaitCycleCountReg + 1.U }
  when(io.lsuPerf.passThroughFire) { lsuPassThroughCountReg := lsuPassThroughCountReg + 1.U }
  when(io.loadUseStall) { loadUseStallCountReg := loadUseStallCountReg + 1.U }
  when(io.flush) { redirectFlushCountReg := redirectFlushCountReg + 1.U }

  when(io.commitTrace.valid) {
    retireCountReg := retireCountReg + 1.U
    lastRetireReg := io.commitTrace

    val inst = io.commitTrace.bits.inst
    val opcode = inst(6, 0)
    val funct3 = inst(14, 12)
    val isLoad = opcode === "b0000011".U
    val isStore = opcode === "b0100011".U
    val isBranch = opcode === "b1100011".U
    val isJal = opcode === "b1101111".U
    val isJalr = opcode === "b1100111".U
    val isJump = isJal || isJalr
    val isSystem = opcode === "b1110011".U
    val isEcall = isSystem && funct3 === 0.U && inst === Instructions.ECALL.value.U
    val isMret = isSystem && funct3 === 0.U && inst === Instructions.MRET.value.U
    val isEbreak = isSystem && funct3 === 0.U && inst === Instructions.EBREAK.value.U
    val isSystemControl = isEcall || isMret || isEbreak
    val isCsr = isSystem && !isSystemControl
    val isMemory = isLoad || isStore
    val isCompute = !(isMemory || isBranch || isJump || isSystem)

    when(isCompute) { computeInstCountReg := computeInstCountReg + 1.U }
    when(isMemory) { memoryInstCountReg := memoryInstCountReg + 1.U }
    when(isLoad) { loadInstCountReg := loadInstCountReg + 1.U }
    when(isStore) { storeInstCountReg := storeInstCountReg + 1.U }
    when(isBranch) { branchInstCountReg := branchInstCountReg + 1.U }
    when(isJump) { jumpInstCountReg := jumpInstCountReg + 1.U }
    when(isCsr) { csrInstCountReg := csrInstCountReg + 1.U }
    when(isSystemControl) { systemInstCountReg := systemInstCountReg + 1.U }
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
  io.trace.cycleCount := cycleCountReg
  io.trace.retireCount := retireCountReg
  io.trace.lastRetire := lastRetireReg
  io.trace.branchCount := branchCountReg
  io.trace.branchCorrectCount := branchCorrectCountReg
  io.trace.ifuReqCount := ifuReqCountReg
  io.trace.ifuRespCount := ifuRespCountReg
  io.trace.ifuWaitCycleCount := ifuWaitCycleCountReg
  io.trace.ifuBlockedByPendingCount := ifuBlockedByPendingCountReg
  io.trace.ifuBlockedByOutValidCount := ifuBlockedByOutValidCountReg
  io.trace.ifuBlockedByStallCount := ifuBlockedByStallCountReg
  io.trace.ifuBlockedByRedirectCount := ifuBlockedByRedirectCountReg
  io.trace.decodeAcceptedCount := decodeAcceptedCountReg
  io.trace.exuCompleteCount := exuCompleteCountReg
  io.trace.lsuLoadReqCount := lsuLoadReqCountReg
  io.trace.lsuLoadRespCount := lsuLoadRespCountReg
  io.trace.lsuLoadWaitCycleCount := lsuLoadWaitCycleCountReg
  io.trace.lsuStoreReqCount := lsuStoreReqCountReg
  io.trace.lsuStoreRespCount := lsuStoreRespCountReg
  io.trace.lsuStoreWaitCycleCount := lsuStoreWaitCycleCountReg
  io.trace.lsuPassThroughCount := lsuPassThroughCountReg
  io.trace.loadUseStallCount := loadUseStallCountReg
  io.trace.redirectFlushCount := redirectFlushCountReg
  io.trace.computeInstCount := computeInstCountReg
  io.trace.memoryInstCount := memoryInstCountReg
  io.trace.loadInstCount := loadInstCountReg
  io.trace.storeInstCount := storeInstCountReg
  io.trace.branchInstCount := branchInstCountReg
  io.trace.jumpInstCount := jumpInstCountReg
  io.trace.csrInstCount := csrInstCountReg
  io.trace.systemInstCount := systemInstCountReg
}
