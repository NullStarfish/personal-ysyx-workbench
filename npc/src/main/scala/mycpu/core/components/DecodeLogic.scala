package mycpu.core.backend

import chisel3._
import chisel3.util._
import mycpu.common._
import mycpu.common.Instructions._
import mycpu.core.bundles._
import mycpu.core.os.ResourceHandle

object DecodeLogic {
  def apply(inst: UInt, csr: ResourceHandle, mem: ResourceHandle): CtrlSignals = {
    val ctrl = Wire(new CtrlSignals)
    val funct3 = inst(14, 12)
    val funct7 = inst(31, 25)
    val opcode = inst(6, 0)

    // --- 1. 主动配置驱动 (Driver Side-Effects) ---
    // CSR 配置：映射 funct3 到 CSROp
    when(opcode === "b1110011".U) {
      val csrOp = MuxLookup(funct3(1, 0), CSROp.N.asUInt)(Seq(
        1.U -> CSROp.W.asUInt,
        2.U -> CSROp.S.asUInt,
        3.U -> CSROp.C.asUInt
      ))
      csr.ioctl(csrOp, 0.U)
    }

    // [修复] 移除 MEM ioctl，现在通过 CtrlSignals 传递 size
    // mem.ioctl(funct3, 0.U) // <--- 已移除

    // --- 2. 意图分发表 ---
    val S_ALU = ServiceType.ALU;   val S_MR = ServiceType.MEM_RD; val S_MW = ServiceType.MEM_WR
    val S_BR  = ServiceType.BRANCH; val S_CSR = ServiceType.CSR;   val S_NO = ServiceType.NONE
    
    val A1_R = Arg1Type.REG;  val A1_P = Arg1Type.PC;  val A1_Z = Arg1Type.ZERO; val A1_ZM = Arg1Type.ZIMM
    val A2_R = Arg2Type.REG;  val A2_I = Arg2Type.IMM; val A2_4 = Arg2Type.CONST_4
    
    val Y = true.B; val N = false.B

    val map = Array(
      // 算术 (I-Type)
      ADDI  -> List(S_ALU, ALUOp.ADD,  A1_R,  A2_I, ImmType.I, Y),
      SLTI  -> List(S_ALU, ALUOp.SLT,  A1_R,  A2_I, ImmType.I, Y),
      SLTIU -> List(S_ALU, ALUOp.SLTU, A1_R,  A2_I, ImmType.I, Y),
      XORI  -> List(S_ALU, ALUOp.XOR,  A1_R,  A2_I, ImmType.I, Y),
      ORI   -> List(S_ALU, ALUOp.OR,   A1_R,  A2_I, ImmType.I, Y),
      ANDI  -> List(S_ALU, ALUOp.AND,  A1_R,  A2_I, ImmType.I, Y),
      SLLI  -> List(S_ALU, ALUOp.SLL,  A1_R,  A2_I, ImmType.I, Y),
      SRLI  -> List(S_ALU, ALUOp.SRL,  A1_R,  A2_I, ImmType.I, Y),
      SRAI  -> List(S_ALU, ALUOp.SRA,  A1_R,  A2_I, ImmType.I, Y),

      // 算术 (R-Type)
      ADD   -> List(S_ALU, ALUOp.ADD,  A1_R,  A2_R, ImmType.Z, Y),
      SUB   -> List(S_ALU, ALUOp.SUB,  A1_R,  A2_R, ImmType.Z, Y),
      SLL   -> List(S_ALU, ALUOp.SLL,  A1_R,  A2_R, ImmType.Z, Y),
      SLT   -> List(S_ALU, ALUOp.SLT,  A1_R,  A2_R, ImmType.Z, Y),
      SLTU  -> List(S_ALU, ALUOp.SLTU, A1_R,  A2_R, ImmType.Z, Y),
      XOR   -> List(S_ALU, ALUOp.XOR,  A1_R,  A2_R, ImmType.Z, Y),
      SRL   -> List(S_ALU, ALUOp.SRL,  A1_R,  A2_R, ImmType.Z, Y),
      SRA   -> List(S_ALU, ALUOp.SRA,  A1_R,  A2_R, ImmType.Z, Y),
      OR    -> List(S_ALU, ALUOp.OR,   A1_R,  A2_R, ImmType.Z, Y),
      AND   -> List(S_ALU, ALUOp.AND,  A1_R,  A2_R, ImmType.Z, Y),

      // 其他 ALU 相关
      LUI   -> List(S_ALU, ALUOp.COPY_B, A1_Z, A2_I, ImmType.U, Y),
      AUIPC -> List(S_ALU, ALUOp.ADD,    A1_P, A2_I, ImmType.U, Y),

      // 访存 (地址计算使用 ADD)
      LW    -> List(S_MR,  ALUOp.ADD,  A1_R,  A2_I, ImmType.I, Y),
      SW    -> List(S_MW,  ALUOp.ADD,  A1_R,  A2_I, ImmType.S, N),
      // [新增] 补充其他访存指令的解码
      LB    -> List(S_MR,  ALUOp.ADD,  A1_R,  A2_I, ImmType.I, Y),
      LH    -> List(S_MR,  ALUOp.ADD,  A1_R,  A2_I, ImmType.I, Y),
      LBU   -> List(S_MR,  ALUOp.ADD,  A1_R,  A2_I, ImmType.I, Y),
      LHU   -> List(S_MR,  ALUOp.ADD,  A1_R,  A2_I, ImmType.I, Y),
      SB    -> List(S_MW,  ALUOp.ADD,  A1_R,  A2_I, ImmType.S, N),
      SH    -> List(S_MW,  ALUOp.ADD,  A1_R,  A2_I, ImmType.S, N),

      // 跳转
      JAL   -> List(S_BR,  ALUOp.ADD,  A1_P,  A2_I, ImmType.J, Y),
      JALR  -> List(S_BR,  ALUOp.ADD,  A1_R,  A2_I, ImmType.I, Y),
      BEQ   -> List(S_BR,  ALUOp.SUB,  A1_R,  A2_R, ImmType.B, N),
      BNE   -> List(S_BR,  ALUOp.SUB,  A1_R,  A2_R, ImmType.B, N),
      BLT   -> List(S_BR,  ALUOp.SLT,  A1_R,  A2_R, ImmType.B, N),
      BGE   -> List(S_BR,  ALUOp.SLT,  A1_R,  A2_R, ImmType.B, N),
      BLTU  -> List(S_BR,  ALUOp.SLTU, A1_R,  A2_R, ImmType.B, N),
      BGEU  -> List(S_BR,  ALUOp.SLTU, A1_R,  A2_R, ImmType.B, N),

      // CSR
      CSRRW -> List(S_CSR, ALUOp.COPY_A, A1_R,  A2_R, ImmType.Z, Y),
      CSRRWI-> List(S_CSR, ALUOp.COPY_A, A1_ZM, A2_R, ImmType.Z, Y)
    )

    val decoded = ListLookup(inst, List(S_NO, ALUOp.NOP, A1_R, A2_R, ImmType.Z, N), map)

    ctrl.service  := decoded(0).asTypeOf(ServiceType())
    ctrl.aluOp    := decoded(1).asTypeOf(ALUOp())
    ctrl.arg1     := decoded(2).asTypeOf(Arg1Type())
    ctrl.arg2     := decoded(3).asTypeOf(Arg2Type())
    ctrl.immType  := decoded(4).asTypeOf(ImmType())
    ctrl.regWen   := decoded(5).asTypeOf(Bool())

    // [新增] 解析内存访问宽度和符号扩展
    // funct3: 000=Byte, 001=Half, 010=Word, 100=ByteU, 101=HalfU
    ctrl.memSize   := funct3(1, 0)
    ctrl.memSigned := !funct3(2) // Bit 2 为 1 表示 Unsigned (LBU, LHU)

    ctrl
  }
}