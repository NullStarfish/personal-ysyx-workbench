package labcpu.core.frontend

import chisel3._
import chisel3.util._
import labcpu.core.bundles._
import mycpu.common._
import mycpu.core.bundles._
import mycpu.core.components.GShareBranchPredictor

class Fetch(
    startAddr: BigInt = START_ADDR,
    enableTraceFields: Boolean = ENABLE_TRACE_FIELDS,
    predictorEntries: Int = 8,
) extends Module {
  val io = IO(new Bundle {
    val imem = new InstMemIO
    val out = Decoupled(new FetchPacket)
    val stall = Input(Bool())
    val redirect = Flipped(Valid(UInt(XLEN.W)))
    val bpUpdate = Input(new BranchPredictUpdateBundle)
    val bpUpdateRedirect = Input(Bool())
    val debug_pc = Output(UInt(XLEN.W))
  })

  val pcReg = RegInit(startAddr.U(XLEN.W))
  val predictor = Module(new GShareBranchPredictor(entries = predictorEntries, historyLength = log2Ceil(predictorEntries)))

  predictor.io.pc := pcReg
  predictor.io.update := io.bpUpdate.valid
  predictor.io.updateIndex := io.bpUpdate.index(log2Ceil(predictorEntries) - 1, 0)
  predictor.io.actualTaken := io.bpUpdate.predictedTaken ^ io.bpUpdateRedirect
  predictor.io.predictedTaken := io.bpUpdate.predictedTaken

  val inst = io.imem.rdata
  val opcode = inst(6, 0)
  val isBranch = opcode === "b1100011".U
  val isJal = opcode === "b1101111".U
  val branchImm = Cat(Fill(20, inst(31)), inst(7), inst(30, 25), inst(11, 8), 0.U(1.W))
  val jalImm = Cat(Fill(12, inst(31)), inst(19, 12), inst(20), inst(30, 21), 0.U(1.W))
  val branchPredictedTaken = isBranch && predictor.io.predictTaken
  val predictedRedirect = isJal || branchPredictedTaken
  val predictedTarget = pcReg + Mux(isJal, jalImm, branchImm)

  io.imem.addr := pcReg
  io.out.valid := true.B
  io.out.bits.pc := pcReg
  io.out.bits.inst := inst
  io.out.bits.isException := false.B
  io.out.bits.predictedTaken := branchPredictedTaken
  io.out.bits.predictedRedirect := predictedRedirect
  io.out.bits.predictIndex := predictor.io.predictIndex
  io.debug_pc := pcReg

  when(io.redirect.valid) {
    pcReg := io.redirect.bits
  }.elsewhen(!io.stall) {
    pcReg := Mux(predictedRedirect, predictedTarget, pcReg + 4.U)
  }
}
