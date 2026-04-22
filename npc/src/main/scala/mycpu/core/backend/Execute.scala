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
  val execRhs = Mux(data.exec.family === ExecFamily.Mem, data.data.offset, data.data.rhs)

  val alu = Module(new ALU)
  alu.io.a := data.data.lhs
  alu.io.b := execRhs
  alu.io.op := MuxLookup(data.exec.op, ALUOp.NOP)(Seq(
    ExecOp.Add  -> ALUOp.ADD,
    ExecOp.Sub  -> ALUOp.SUB,
    ExecOp.And  -> ALUOp.AND,
    ExecOp.Or   -> ALUOp.OR,
    ExecOp.Xor  -> ALUOp.XOR,
    ExecOp.Slt  -> ALUOp.SLT,
    ExecOp.Sltu -> ALUOp.SLTU,
    ExecOp.Sll  -> ALUOp.SLL,
    ExecOp.Srl  -> ALUOp.SRL,
    ExecOp.Sra  -> ALUOp.SRA,
    ExecOp.Lui  -> ALUOp.COPY_B,
    ExecOp.Auipc -> ALUOp.ADD,
    ExecOp.Load -> ALUOp.ADD,
    ExecOp.Store -> ALUOp.ADD
  ))

  val csr = Module(new CSR)
  csr.io.cmd := MuxLookup(data.exec.op, CSROp.N)(Seq(
    ExecOp.CsrRw -> CSROp.W,
    ExecOp.CsrRs -> CSROp.S,
    ExecOp.CsrRc -> CSROp.C
  ))
  csr.io.addr := data.sys.csrAddr
  csr.io.wdata := data.data.rhs
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

  val isEq = data.data.lhs === data.data.rhs
  val isLt = data.data.lhs.asSInt < data.data.rhs.asSInt
  val isLtu = data.data.lhs < data.data.rhs
  val takeBranch = MuxLookup(data.exec.op, false.B)(Seq(
    ExecOp.Beq  -> isEq,
    ExecOp.Bne  -> !isEq,
    ExecOp.Blt  -> isLt,
    ExecOp.Bge  -> !isLt,
    ExecOp.Bltu -> isLtu,
    ExecOp.Bgeu -> !isLtu
  ))

  val jumpTarget = data.data.pc + data.data.offset
  val jalrTarget = (data.data.lhs + data.data.offset) & ~1.U(XLEN.W)
  val branchActualTaken = data.exec.family === ExecFamily.Branch && takeBranch
  val branchPredictedTaken = data.pred.predictedTaken
  val branchMispredict = data.exec.family === ExecFamily.Branch && (branchActualTaken =/= branchPredictedTaken)
  val branchRecoveryTarget = Mux(branchActualTaken, jumpTarget, data.data.pc + 4.U)

  val redirectTarget = MuxCase(0.U(XLEN.W), Seq(
    branchMispredict -> branchRecoveryTarget,
    data.sys.isMret -> csr.io.epc,
    data.sys.isEcall -> csr.io.evec,
    (data.exec.family === ExecFamily.Jump && data.exec.op === ExecOp.Jalr) -> jalrTarget,
    (data.exec.family === ExecFamily.Jump) -> jumpTarget,
  ))
  val redirectValid =
    branchMispredict ||
      (data.exec.family === ExecFamily.Jump) ||
      data.sys.isEcall ||
      data.sys.isMret

  val result = MuxCase(alu.io.out, Seq(
    (data.exec.family === ExecFamily.Jump) -> (data.data.pc + 4.U),
    (data.exec.family === ExecFamily.Csr) -> csr.io.rdata
  ))
  val architecturalNextPc = MuxCase(data.data.pc + 4.U, Seq(
    (data.exec.family === ExecFamily.Branch && branchActualTaken) -> jumpTarget,
    (data.exec.family === ExecFamily.Jump && data.exec.op === ExecOp.Jalr) -> jalrTarget,
    (data.exec.family === ExecFamily.Jump) -> jumpTarget,
    data.sys.isEcall -> csr.io.evec,
    data.sys.isMret -> csr.io.epc,
  ))

  io.out.bits.result := result
  io.out.bits.rhs := data.data.rhs
  io.out.bits.wb.regWen := data.wb.regWen
  io.out.bits.wb.rd := data.wb.rd
  io.out.bits.mem.valid := data.mem.valid
  io.out.bits.mem.write := data.mem.write
  io.out.bits.mem.unsigned := data.mem.unsigned
  io.out.bits.mem.subop := data.mem.subop
  io.out.bits.redirect.valid := redirectValid
  io.out.bits.redirect.bits := redirectTarget
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
    io.out.bits.trace.get.branchResolved := io.in.fire && data.exec.family === ExecFamily.Branch
    io.out.bits.trace.get.branchCorrect := data.exec.family === ExecFamily.Branch && (branchActualTaken === branchPredictedTaken)
    io.out.bits.trace.get.redirectValid := redirectValid
    io.out.bits.trace.get.redirectTarget := redirectTarget
    io.out.bits.trace.get.actualTaken := branchActualTaken
    io.out.bits.trace.get.predictedTaken := branchPredictedTaken
  }

  io.bpUpdate.valid := io.in.fire && data.exec.family === ExecFamily.Branch
  io.bpUpdate.pc := data.data.pc
  io.bpUpdate.actualTaken := branchActualTaken
  io.bpUpdate.predictedTaken := branchPredictedTaken

  io.out.valid := io.in.valid
  io.in.ready := io.out.ready
}
