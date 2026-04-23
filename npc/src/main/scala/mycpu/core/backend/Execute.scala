package mycpu.core.backend

import chisel3._
import chisel3.util._
import mycpu.common._
import mycpu.core.bundles._
import mycpu.core.components._

class Execute(enableTraceFields: Boolean = ENABLE_TRACE_FIELDS) extends Module {
  val io = IO(new Bundle {
    val in = Flipped(Decoupled(new DecodePacket(enableTraceFields)))
    val out = Decoupled(new ExecutePacket(enableTraceFields))
    val bpUpdate = Output(new BranchPredictUpdateBundle)
    val debug_csrs = Output(new Bundle {
      val mtvec   = UInt(XLEN.W)
      val mepc    = UInt(XLEN.W)
      val mstatus = UInt(XLEN.W)
      val mcause  = UInt(XLEN.W)
    })
  })

  val data = io.in.bits
  val aluInA = Mux(data.exec.aluSrcA === ALUSrcA.Pc, data.data.pc, data.data.rs1)
  val aluInB = Mux(data.exec.aluSrcB === ALUSrcB.Imm, data.data.imm, data.data.rs2)
  val pcPlus4 = data.data.pc + 4.U

  val alu = Module(new ALU)
  alu.io.a := aluInA
  alu.io.b := aluInB
  alu.io.op := data.exec.aluOp

  val csr = Module(new CSR)
  csr.io.cmd := data.sys.csrOp
  csr.io.addr := data.sys.csrAddr
  csr.io.wdata := data.data.rs1
  csr.io.pc := data.data.pc
  csr.io.isEcall := data.sys.isEcall && io.in.valid
  csr.io.isMret := data.sys.isMret && io.in.valid
  io.debug_csrs.mtvec := csr.io.debug_mtvec
  io.debug_csrs.mepc := csr.io.debug_mepc
  io.debug_csrs.mstatus := csr.io.debug_mstatus
  io.debug_csrs.mcause := csr.io.debug_mcause

  val simEbreak = Module(new SimEbreak)
  simEbreak.io.valid := data.sys.isEbreak && io.in.valid
  simEbreak.io.is_ebreak := 0.U

  val rs1 = data.data.rs1
  val rs2 = data.data.rs2
  val isEq = rs1 === rs2
  val isLtu = rs1 < rs2
  val rs1Sign = rs1(XLEN - 1)
  val rs2Sign = rs2(XLEN - 1)
  val signedSignsDiffer = rs1Sign =/= rs2Sign
  val isLt = Mux(signedSignsDiffer, rs1Sign && !rs2Sign, isLtu)
  private def branchTaken(branchType: UInt): Bool = Mux1H(Seq(
    (branchType === BranchType.Eq)  -> isEq,
    (branchType === BranchType.Ne)  -> !isEq,
    (branchType === BranchType.Lt)  -> isLt,
    (branchType === BranchType.Ge)  -> !isLt,
    (branchType === BranchType.Ltu) -> isLtu,
    (branchType === BranchType.Geu) -> !isLtu,
  ))
  val takeBranchForRedirect = branchTaken(data.exec.branchType)
  val takeBranchForUpdate = branchTaken(data.exec.branchType)

  val branchDirectTarget = data.data.pc + data.data.imm
  val jumpDirectTarget = data.data.pc + data.data.imm
  val indirectTarget = (data.data.rs1 + data.data.imm) & ~1.U(XLEN.W)
  val isBranch = data.exec.branchType =/= BranchType.None
  val branchActualTakenForRedirect = isBranch && takeBranchForRedirect
  val branchActualTakenForUpdate = isBranch && takeBranchForUpdate
  val branchPredictedTaken = data.pred.predictedTaken
  val branchMispredictForRedirect = isBranch && (branchActualTakenForRedirect =/= branchPredictedTaken)
  // On a branch mispredict, the recovery target is fully determined by the original prediction:
  // predicted taken  -> recover to fallthrough
  // predicted not-taken -> recover to branch target
  val branchRecoveryTarget = Mux(branchPredictedTaken, pcPlus4, branchDirectTarget)
  val jumpRedirectTarget = Mux(data.exec.isJalr, indirectTarget, jumpDirectTarget)
  val sysRedirectTarget = Mux(data.sys.isMret, csr.io.epc, csr.io.evec)
  val hasSysRedirect = data.sys.isEcall || data.sys.isMret
  val hasJumpRedirect = data.exec.isJump
  val hasBranchRecovery = isBranch

  val redirectTarget = Mux(
    hasBranchRecovery,
    branchRecoveryTarget,
    Mux(
      hasSysRedirect,
      sysRedirectTarget,
      Mux(hasJumpRedirect, jumpRedirectTarget, 0.U(XLEN.W))
    )
  )
  val redirectValid =
    branchMispredictForRedirect ||
      hasJumpRedirect ||
      hasSysRedirect

  val result = MuxLookup(data.exec.wbSel, alu.io.out)(Seq(
    WBSel.Alu -> alu.io.out,
    WBSel.Csr -> csr.io.rdata,
    WBSel.PcPlus4 -> pcPlus4,
  ))
  val architecturalNextPc = MuxCase(data.data.pc + 4.U, Seq(
    (isBranch && branchActualTakenForRedirect) -> branchDirectTarget,
    (data.exec.isJump && data.exec.isJalr) -> indirectTarget,
    data.exec.isJump -> jumpDirectTarget,
    data.sys.isEcall -> csr.io.evec,
    data.sys.isMret -> csr.io.epc,
  ))

  io.out.bits.result := result
  io.out.bits.rhs := Mux(data.mem.write, data.data.rs2, redirectTarget)
  io.out.bits.wb.regWen := data.wb.regWen
  io.out.bits.wb.rd := data.wb.rd
  io.out.bits.mem.valid := data.mem.valid
  io.out.bits.mem.write := data.mem.write
  io.out.bits.mem.unsigned := data.mem.unsigned
  io.out.bits.mem.subop := data.mem.subop
  io.out.bits.redirect := redirectValid
  io.out.bits.bpUpdate.valid := io.in.valid && isBranch
  io.out.bits.bpUpdate.index := data.pred.index
  io.out.bits.bpUpdate.actualTaken := branchActualTakenForUpdate
  io.out.bits.bpUpdate.predictedTaken := branchPredictedTaken
  if (enableTraceFields) {
    io.out.bits.trace.get.pc := io.in.bits.trace.get.pc
    io.out.bits.trace.get.inst := io.in.bits.trace.get.inst
    io.out.bits.trace.get.dnpc := architecturalNextPc
    io.out.bits.trace.get.regWen := io.in.bits.trace.get.regWen
    io.out.bits.trace.get.rd := io.in.bits.trace.get.rd
    io.out.bits.trace.get.data := io.in.bits.trace.get.data
    io.out.bits.trace.get.ifValid := io.in.bits.trace.get.ifValid
    io.out.bits.trace.get.idValid := io.in.bits.trace.get.idValid
    io.out.bits.trace.get.exValid := io.in.valid
    io.out.bits.trace.get.memValid := false.B
    io.out.bits.trace.get.branchResolved := io.in.fire && isBranch
    io.out.bits.trace.get.branchCorrect := isBranch && (branchActualTakenForRedirect === branchPredictedTaken)
    io.out.bits.trace.get.redirectValid := redirectValid
    io.out.bits.trace.get.redirectTarget := redirectTarget
    io.out.bits.trace.get.actualTaken := branchActualTakenForRedirect
    io.out.bits.trace.get.predictedTaken := branchPredictedTaken
  }

  io.bpUpdate.valid := io.in.fire && isBranch
  io.bpUpdate.index := data.pred.index
  io.bpUpdate.actualTaken := branchActualTakenForUpdate
  io.bpUpdate.predictedTaken := branchPredictedTaken

  io.out.valid := io.in.valid
  io.in.ready := io.out.ready
}
