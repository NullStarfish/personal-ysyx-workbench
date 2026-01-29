package mycpu.core.components

import chisel3._
import chisel3.util._
import mycpu.common._
import mycpu.common.Instructions
import mycpu.core.bundles._

class ControlUnit extends Module {
  val io = IO(new Bundle {
    val inst = Input(UInt(32.W))
    val ctrl = Output(new CtrlSignals)
  })

  // Service Type
  val S_ALU    = ServiceType.ALU.asUInt
  val S_MEM_RD = ServiceType.MEM_RD.asUInt
  val S_MEM_WR = ServiceType.MEM_WR.asUInt
  val S_BR     = ServiceType.BRANCH.asUInt
  val S_JUMP   = ServiceType.JUMP.asUInt
  val S_CSR    = ServiceType.CSR.asUInt
  val S_TRAP   = ServiceType.TRAP.asUInt
  val S_NONE   = ServiceType.NONE.asUInt

  // ALU Op
  val OP_ADD  = ALUOp.ADD.asUInt
  val OP_SUB  = ALUOp.SUB.asUInt
  val OP_AND  = ALUOp.AND.asUInt
  val OP_OR   = ALUOp.OR.asUInt
  val OP_XOR  = ALUOp.XOR.asUInt
  val OP_SLT  = ALUOp.SLT.asUInt
  val OP_SLTU = ALUOp.SLTU.asUInt
  val OP_SLL  = ALUOp.SLL.asUInt
  val OP_SRL  = ALUOp.SRL.asUInt
  val OP_SRA  = ALUOp.SRA.asUInt
  val OP_CPB  = ALUOp.COPY_B.asUInt
  val OP_CPA  = ALUOp.COPY_A.asUInt
  val OP_NOP  = ALUOp.NOP.asUInt

  // Arg1
  val A1_REG  = Arg1Type.REG.asUInt
  val A1_PC   = Arg1Type.PC.asUInt
  val A1_ZERO = Arg1Type.ZERO.asUInt

  // Arg2
  val A2_REG  = Arg2Type.REG.asUInt
  val A2_IMM  = Arg2Type.IMM.asUInt
  val A2_4    = Arg2Type.CONST_4.asUInt

  // Imm Type
  val IM_I = ImmType.I.asUInt
  val IM_S = ImmType.S.asUInt
  val IM_B = ImmType.B.asUInt
  val IM_U = ImmType.U.asUInt
  val IM_J = ImmType.J.asUInt
  val IM_Z = ImmType.Z.asUInt

  // Bools
  val Y = 1.U(1.W)
  val N = 0.U(1.W)

  // CSR Ops
  val C_N = CSROp.N.asUInt
  val C_W = CSROp.W.asUInt
  val C_S = CSROp.S.asUInt
  val C_C = CSROp.C.asUInt

  import Instructions.{
    LUI, AUIPC, JAL, JALR, BEQ, BNE, BLT, BGE, BLTU, BGEU,
    LB, LH, LW, LBU, LHU, SB, SH, SW,
    ADDI, SLTI, SLTIU, XORI, ORI, ANDI, SLLI, SRLI, SRAI,
    ADD, SUB, SLL, SLT, SLTU, XOR, SRL, SRA, OR, AND,
    CSRRW, CSRRS, CSRRC, CSRRWI, CSRRSI, CSRRCI,
    ECALL, EBREAK, MRET
  }

  val defaultCtrl = List(S_NONE, OP_NOP, A1_REG, A2_REG, IM_I, N, 2.U, N, C_N)

  val map = Array(
    // === LUI / AUIPC ===
    LUI   -> List(S_ALU,  OP_CPB, A1_ZERO, A2_IMM, IM_U, Y,  2.U, N, C_N),
    AUIPC -> List(S_ALU,  OP_ADD, A1_PC,   A2_IMM, IM_U, Y,  2.U, N, C_N),

    // === Jumps ===
    // [修复] JAL: Target = PC + ImmJ. (原先写成了 A2_4，导致只跳转 PC+4)
    JAL   -> List(S_JUMP, OP_ADD, A1_PC,   A2_IMM, IM_J, Y,  2.U, N, C_N),
    // [修复] JALR: Target = RS1 + ImmI. (原先写成了 A1_PC + A2_4)
    JALR  -> List(S_JUMP, OP_ADD, A1_REG,  A2_IMM, IM_I, Y,  2.U, N, C_N),

    // === Branch ===
    BEQ   -> List(S_BR,   OP_SUB, A1_REG,  A2_REG, IM_B, N,  2.U, N, C_N),
    BNE   -> List(S_BR,   OP_SUB, A1_REG,  A2_REG, IM_B, N,  2.U, N, C_N),
    BLT   -> List(S_BR,   OP_SLT, A1_REG,  A2_REG, IM_B, N,  2.U, N, C_N),
    BGE   -> List(S_BR,   OP_SLT, A1_REG,  A2_REG, IM_B, N,  2.U, N, C_N),
    BLTU  -> List(S_BR,   OP_SLTU,A1_REG,  A2_REG, IM_B, N,  2.U, N, C_N),
    BGEU  -> List(S_BR,   OP_SLTU,A1_REG,  A2_REG, IM_B, N,  2.U, N, C_N),

    // === Loads ===
    LB    -> List(S_MEM_RD, OP_ADD, A1_REG, A2_IMM, IM_I, Y,  0.U, Y, C_N),
    LH    -> List(S_MEM_RD, OP_ADD, A1_REG, A2_IMM, IM_I, Y,  1.U, Y, C_N),
    LW    -> List(S_MEM_RD, OP_ADD, A1_REG, A2_IMM, IM_I, Y,  2.U, Y, C_N),
    LBU   -> List(S_MEM_RD, OP_ADD, A1_REG, A2_IMM, IM_I, Y,  0.U, N, C_N),
    LHU   -> List(S_MEM_RD, OP_ADD, A1_REG, A2_IMM, IM_I, Y,  1.U, N, C_N),

    // === Stores ===
    SB    -> List(S_MEM_WR, OP_ADD, A1_REG, A2_IMM, IM_S, N,  0.U, N, C_N),
    SH    -> List(S_MEM_WR, OP_ADD, A1_REG, A2_IMM, IM_S, N,  1.U, N, C_N),
    SW    -> List(S_MEM_WR, OP_ADD, A1_REG, A2_IMM, IM_S, N,  2.U, N, C_N),

    // === ALU Immediate ===
    ADDI  -> List(S_ALU, OP_ADD, A1_REG, A2_IMM, IM_I, Y,  2.U, N, C_N),
    SLTI  -> List(S_ALU, OP_SLT, A1_REG, A2_IMM, IM_I, Y,  2.U, N, C_N),
    SLTIU -> List(S_ALU, OP_SLTU,A1_REG, A2_IMM, IM_I, Y,  2.U, N, C_N),
    XORI  -> List(S_ALU, OP_XOR, A1_REG, A2_IMM, IM_I, Y,  2.U, N, C_N),
    ORI   -> List(S_ALU, OP_OR,  A1_REG, A2_IMM, IM_I, Y,  2.U, N, C_N),
    ANDI  -> List(S_ALU, OP_AND, A1_REG, A2_IMM, IM_I, Y,  2.U, N, C_N),
    SLLI  -> List(S_ALU, OP_SLL, A1_REG, A2_IMM, IM_I, Y,  2.U, N, C_N),
    SRLI  -> List(S_ALU, OP_SRL, A1_REG, A2_IMM, IM_I, Y,  2.U, N, C_N),
    SRAI  -> List(S_ALU, OP_SRA, A1_REG, A2_IMM, IM_I, Y,  2.U, N, C_N),

    // === ALU Register ===
    ADD   -> List(S_ALU, OP_ADD, A1_REG, A2_REG, IM_I, Y,  2.U, N, C_N),
    SUB   -> List(S_ALU, OP_SUB, A1_REG, A2_REG, IM_I, Y,  2.U, N, C_N),
    SLL   -> List(S_ALU, OP_SLL, A1_REG, A2_REG, IM_I, Y,  2.U, N, C_N),
    SLT   -> List(S_ALU, OP_SLT, A1_REG, A2_REG, IM_I, Y,  2.U, N, C_N),
    SLTU  -> List(S_ALU, OP_SLTU,A1_REG, A2_REG, IM_I, Y,  2.U, N, C_N),
    XOR   -> List(S_ALU, OP_XOR, A1_REG, A2_REG, IM_I, Y,  2.U, N, C_N),
    SRL   -> List(S_ALU, OP_SRL, A1_REG, A2_REG, IM_I, Y,  2.U, N, C_N),
    SRA   -> List(S_ALU, OP_SRA, A1_REG, A2_REG, IM_I, Y,  2.U, N, C_N),
    OR    -> List(S_ALU, OP_OR,  A1_REG, A2_REG, IM_I, Y,  2.U, N, C_N),
    AND   -> List(S_ALU, OP_AND, A1_REG, A2_REG, IM_I, Y,  2.U, N, C_N),

    // === CSR ===
    CSRRW -> List(S_CSR, OP_CPA, A1_REG, A2_REG, IM_I, Y,  2.U, N, C_W),
    CSRRS -> List(S_CSR, OP_CPA, A1_REG, A2_REG, IM_I, Y,  2.U, N, C_S),
    CSRRC -> List(S_CSR, OP_CPA, A1_REG, A2_REG, IM_I, Y,  2.U, N, C_C),
    CSRRWI-> List(S_CSR, OP_CPA, A1_REG, A2_REG, IM_I, Y,  2.U, N, C_W),
    CSRRSI-> List(S_CSR, OP_CPA, A1_REG, A2_REG, IM_I, Y,  2.U, N, C_S),
    CSRRCI-> List(S_CSR, OP_CPA, A1_REG, A2_REG, IM_I, Y,  2.U, N, C_C),

    // === System ===
    ECALL -> List(S_TRAP, OP_NOP, A1_REG, A2_REG, IM_I, N, 2.U, N, C_N),
    EBREAK-> List(S_TRAP, OP_NOP, A1_REG, A2_REG, IM_I, N, 2.U, N, C_N),
    MRET  -> List(S_TRAP, OP_NOP, A1_REG, A2_REG, IM_I, N, 2.U, N, C_N)
  )

  val signals = ListLookup(io.inst, defaultCtrl, map)

  io.ctrl.service   := signals(0).asTypeOf(ServiceType())
  io.ctrl.aluOp     := signals(1).asTypeOf(ALUOp())
  io.ctrl.arg1      := signals(2).asTypeOf(Arg1Type())
  io.ctrl.arg2      := signals(3).asTypeOf(Arg2Type())
  io.ctrl.immType   := signals(4).asTypeOf(ImmType())
  io.ctrl.regWen    := signals(5).asBool
  io.ctrl.memSize   := signals(6)
  io.ctrl.memSigned := signals(7).asBool
  io.ctrl.csrOp     := signals(8).asTypeOf(CSROp())
}