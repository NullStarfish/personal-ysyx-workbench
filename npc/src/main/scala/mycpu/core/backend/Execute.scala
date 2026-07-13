package mycpu.core.backend

import chisel3._
import chisel3.util._
import mycpu.common._
import mycpu.core.bundles._
import mycpu.core.components._

class Execute(
    enableTraceFields: Boolean = ENABLE_TRACE_FIELDS,
    enableDpi: Boolean = false,
) extends Module {
  val io = IO(new Bundle {
    val in = Flipped(Decoupled(new DecodePacket))
    val out = Decoupled(new ExecutePacket(enableTraceFields))
  })

  val data = io.in.bits
  val ctrl = data.execCtrl
  val sys = data.sys
  val execData = data.execData

  val aluInA = Mux(ctrl.aluSrcA === ALUSrcA.Pc, execData.pc, execData.rs1)
  val aluInB = Mux(ctrl.aluSrcB === ALUSrcB.Imm, execData.imm, execData.rs2)
  val pcPlus4 = execData.pc + 4.U

  val alu = Module(new ALU)
  alu.io.a := aluInA
  alu.io.b := aluInB
  alu.io.op := ctrl.aluOp

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
  val hasJumpRedirect = ctrl.isJump
  val hasBranchRedirect = branchTakenNow

  val redirectTarget = MuxCase(0.U(XLEN.W), Seq(
    hasBranchRedirect -> branchDirectTarget,
    hasJumpRedirect -> jumpRedirectTarget,
  ))
  val redirectValid = !data.inst.except.valid && (hasBranchRedirect || hasJumpRedirect)

  val result = MuxLookup(ctrl.wbSel, alu.io.out)(Seq(
    WBSel.Alu -> alu.io.out,
    WBSel.Csr -> 0.U,
    WBSel.PcPlus4 -> pcPlus4,
  ))

  val architecturalNextPc = MuxCase(pcPlus4, Seq(
    hasBranchRedirect -> branchDirectTarget,
    ctrl.isJump -> jumpRedirectTarget,
  ))

  io.out.bits.lhs := MuxCase(result, Seq(
    data.memCtrl.write -> execData.rs2,
    (sys.csr.csrOp =/= CSROp.N) -> sys.csr.wdata,
  ))
  io.out.bits.rhs := Mux(data.memCtrl.en, alu.io.out, redirectTarget)
  io.out.bits.inst.pc := data.inst.pc
  io.out.bits.inst.except.no := data.inst.except.no
  io.out.bits.inst.except.valid := data.inst.except.valid

  io.out.bits.wbCtrl.wen := data.wbCtrl.wen
  io.out.bits.wbCtrl.rd := data.wbCtrl.rd

  io.out.bits.memCtrl.en := data.memCtrl.en
  io.out.bits.memCtrl.write := data.memCtrl.write
  io.out.bits.memCtrl.unsigned := data.memCtrl.unsigned
  io.out.bits.memCtrl.subop := data.memCtrl.subop

  io.out.bits.sys.ebreak := sys.ebreak
  io.out.bits.sys.mret := sys.mret
  io.out.bits.sys.fencei := sys.fencei
  io.out.bits.sys.csr.csrOp := sys.csr.csrOp
  io.out.bits.sys.csr.csrAddr := sys.csr.csrAddr

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

  if (enableDpi) {
    val executeTrace = Module(new ExecuteTrace)
    executeTrace.io.clk := clock
    executeTrace.io.reset := reset.asBool
    executeTrace.io.finished := io.out.fire
  }
}
