package mycpu.core.backend

import chisel3._
import chisel3.util._
import mycpu.common._
import mycpu.core.bundles._
import mycpu.core.components._
import mycpu.dpi.SimEbreakDPI

class Execute(
    enableTraceFields: Boolean = ENABLE_TRACE_FIELDS,
) extends Module {
  val io = IO(new Bundle {
    val in = Flipped(Decoupled(new DecodePacket))
    val out = Decoupled(new ExecutePacket(enableTraceFields))
    val debug_csrs = Output(new Bundle {
      val mtvec   = UInt(XLEN.W)
      val mepc    = UInt(XLEN.W)
      val mstatus = UInt(XLEN.W)
      val mcause  = UInt(XLEN.W)
    })
  })

  val data = io.in.bits
  val ctrl = data.execCtrl
  val execData = data.execData

  val aluInA = Mux(ctrl.aluSrcA === ALUSrcA.Pc, execData.pc, execData.rs1)
  val aluInB = Mux(ctrl.aluSrcB === ALUSrcB.Imm, execData.imm, execData.rs2)
  val pcPlus4 = execData.pc + 4.U

  val alu = Module(new ALU)
  alu.io.a := aluInA
  alu.io.b := aluInB
  alu.io.op := ctrl.aluOp

  val csr = Module(new CSR)
  csr.io.cmd := ctrl.sys.csrOp
  csr.io.addr := ctrl.sys.csrAddr
  csr.io.wdata := execData.rs1
  csr.io.pc := execData.pc
  csr.io.isEcall := ctrl.sys.ecall && io.in.valid
  csr.io.isMret := ctrl.sys.mret && io.in.valid

  val simEbreak = Module(new SimEbreakDPI)
  simEbreak.io.valid := ctrl.sys.ebreak && io.in.valid
  simEbreak.io.is_ebreak := 0.U

  io.debug_csrs.mtvec := csr.io.debug_mtvec
  io.debug_csrs.mepc := csr.io.debug_mepc
  io.debug_csrs.mstatus := csr.io.debug_mstatus
  io.debug_csrs.mcause := csr.io.debug_mcause

  val rs1 = execData.rs1
  val rs2 = execData.rs2
  val isEq = rs1 === rs2
  val isLtu = rs1 < rs2
  val rs1Sign = rs1(XLEN - 1)
  val rs2Sign = rs2(XLEN - 1)
  val signedSignsDiffer = rs1Sign =/= rs2Sign
  val isLt = Mux(signedSignsDiffer, rs1Sign && !rs2Sign, isLtu)

  private def branchTaken(branchType: BranchType.Type): Bool = Mux1H(Seq(
    (branchType === BranchType.Eq)  -> isEq,
    (branchType === BranchType.Ne)  -> !isEq,
    (branchType === BranchType.Lt)  -> isLt,
    (branchType === BranchType.Ge)  -> !isLt,
    (branchType === BranchType.Ltu) -> isLtu,
    (branchType === BranchType.Geu) -> !isLtu,
  ))

  val isBranch = ctrl.branchType =/= BranchType.None
  val branchDirectTarget = execData.pc + execData.imm
  val jumpDirectTarget = execData.pc + execData.imm
  val indirectTarget = (execData.rs1 + execData.imm) & ~1.U(XLEN.W)
  val branchTakenNow = isBranch && branchTaken(ctrl.branchType)
  val jumpRedirectTarget = Mux(ctrl.isJalr, indirectTarget, jumpDirectTarget)
  val sysRedirectTarget = Mux(ctrl.sys.mret, csr.io.epc, csr.io.evec)

  val hasSysRedirect = ctrl.sys.ecall || ctrl.sys.mret
  val hasJumpRedirect = ctrl.isJump
  val hasBranchRedirect = branchTakenNow

  val redirectTarget = MuxCase(0.U(XLEN.W), Seq(
    hasBranchRedirect -> branchDirectTarget,
    hasSysRedirect -> sysRedirectTarget,
    hasJumpRedirect -> jumpRedirectTarget,
  ))
  val redirectValid = hasBranchRedirect || hasJumpRedirect || hasSysRedirect

  val result = MuxLookup(ctrl.wbSel, alu.io.out)(Seq(
    WBSel.Alu -> alu.io.out,
    WBSel.Csr -> csr.io.rdata,
    WBSel.PcPlus4 -> pcPlus4,
  ))

  val architecturalNextPc = MuxCase(pcPlus4, Seq(
    hasBranchRedirect -> branchDirectTarget,
    ctrl.isJump -> jumpRedirectTarget,
    ctrl.sys.ecall -> csr.io.evec,
    ctrl.sys.mret -> csr.io.epc,
  ))

  io.out.bits.lhs := Mux(data.memCtrl.write, execData.rs2, result)
  io.out.bits.rhs := Mux(data.memCtrl.en, alu.io.out, redirectTarget)

  io.out.bits.wbCtrl.wen := data.wbCtrl.wen
  io.out.bits.wbCtrl.rd := data.wbCtrl.rd

  io.out.bits.memCtrl.en := data.memCtrl.en
  io.out.bits.memCtrl.write := data.memCtrl.write
  io.out.bits.memCtrl.unsigned := data.memCtrl.unsigned
  io.out.bits.memCtrl.subop := data.memCtrl.subop

  io.out.bits.ifRedct.redirect.valid := redirectValid

  if (enableTraceFields) {
    io.out.bits.retireTrace.get := io.in.bits.retireTrace.get
    io.out.bits.retireTrace.get.dnpc := architecturalNextPc
    io.out.bits.retireTrace.get.regWrite.wen := data.wbCtrl.wen
    io.out.bits.retireTrace.get.regWrite.rd := data.wbCtrl.rd
    io.out.bits.retireTrace.get.regWrite.wdata := result
  }
  

  io.out.valid := io.in.valid
  io.in.ready := io.out.ready
}
