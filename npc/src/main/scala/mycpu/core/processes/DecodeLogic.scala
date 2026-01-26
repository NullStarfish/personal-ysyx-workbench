package mycpu.core.processes


import chisel3._
import chisel3.util._
import mycpu.common._
import mycpu.common.Instructions._
import mycpu.core.bundles._

object DecodeLogic {

  // 定义译码结果的临时容器，方便与 ListLookup 对接
  // 顺序：immType, aluOp, op1Sel, op2Sel, regWen, memEn, memWen, csrOp, isBranch, isJump, isEcall, isMret, isEbreak
  private val default = List(
    ImmType.Z, ALUOp.NOP, 0.U, 0.U, false.B, false.B, false.B, CSROp.N, false.B, false.B, false.B, false.B, false.B
  )

  private val table: Array[(BitPat, List[chisel3.Data])] = Array(
    // 控制信号映射表
    //            立即数类型  ALU算子        OP1选择  OP2选择  写回RF  访存En    访存Wen  CSR操作    分支     跳转      Ecall   Mret   Ebreak
    //            |          |             |Reg/PC  |Reg/Imm |      |         |Read/Wr |         |        |         |       |       |
    LUI   -> List(ImmType.U, ALUOp.COPY_B, 0.U,     1.U,     true.B, false.B, false.B, CSROp.N,  false.B, false.B, false.B, false.B, false.B),
    AUIPC -> List(ImmType.U, ALUOp.ADD,    1.U,     1.U,     true.B, false.B, false.B, CSROp.N,  false.B, false.B, false.B, false.B, false.B),
    
    JAL   -> List(ImmType.J, ALUOp.ADD,    1.U,     1.U,     true.B, false.B, false.B, CSROp.N,  false.B, true.B,  false.B, false.B, false.B),
    JALR  -> List(ImmType.I, ALUOp.ADD,    0.U,     1.U,     true.B, false.B, false.B, CSROp.N,  false.B, true.B,  false.B, false.B, false.B),

    BEQ   -> List(ImmType.B, ALUOp.NOP,    0.U,     0.U,     false.B, false.B, false.B, CSROp.N,  true.B,  false.B, false.B, false.B, false.B),
    BNE   -> List(ImmType.B, ALUOp.NOP,    0.U,     0.U,     false.B, false.B, false.B, CSROp.N,  true.B,  false.B, false.B, false.B, false.B),
    BLT   -> List(ImmType.B, ALUOp.NOP,    0.U,     0.U,     false.B, false.B, false.B, CSROp.N,  true.B,  false.B, false.B, false.B, false.B),
    BGE   -> List(ImmType.B, ALUOp.NOP,    0.U,     0.U,     false.B, false.B, false.B, CSROp.N,  true.B,  false.B, false.B, false.B, false.B),
    BLTU  -> List(ImmType.B, ALUOp.NOP,    0.U,     0.U,     false.B, false.B, false.B, CSROp.N,  true.B,  false.B, false.B, false.B, false.B),
    BGEU  -> List(ImmType.B, ALUOp.NOP,    0.U,     0.U,     false.B, false.B, false.B, CSROp.N,  true.B,  false.B, false.B, false.B, false.B),

    LB    -> List(ImmType.I, ALUOp.ADD,    0.U,     1.U,     true.B,  true.B,  false.B, CSROp.N,  false.B, false.B, false.B, false.B, false.B),
    LH    -> List(ImmType.I, ALUOp.ADD,    0.U,     1.U,     true.B,  true.B,  false.B, CSROp.N,  false.B, false.B, false.B, false.B, false.B),
    LW    -> List(ImmType.I, ALUOp.ADD,    0.U,     1.U,     true.B,  true.B,  false.B, CSROp.N,  false.B, false.B, false.B, false.B, false.B),
    LBU   -> List(ImmType.I, ALUOp.ADD,    0.U,     1.U,     true.B,  true.B,  false.B, CSROp.N,  false.B, false.B, false.B, false.B, false.B),
    LHU   -> List(ImmType.I, ALUOp.ADD,    0.U,     1.U,     true.B,  true.B,  false.B, CSROp.N,  false.B, false.B, false.B, false.B, false.B),

    SB    -> List(ImmType.S, ALUOp.ADD,    0.U,     1.U,     false.B, true.B,  true.B,  CSROp.N,  false.B, false.B, false.B, false.B, false.B),
    SH    -> List(ImmType.S, ALUOp.ADD,    0.U,     1.U,     false.B, true.B,  true.B,  CSROp.N,  false.B, false.B, false.B, false.B, false.B),
    SW    -> List(ImmType.S, ALUOp.ADD,    0.U,     1.U,     false.B, true.B,  true.B,  CSROp.N,  false.B, false.B, false.B, false.B, false.B),

    ADDI  -> List(ImmType.I, ALUOp.ADD,    0.U,     1.U,     true.B,  false.B, false.B, CSROp.N,  false.B, false.B, false.B, false.B, false.B),
    SLTI  -> List(ImmType.I, ALUOp.SLT,    0.U,     1.U,     true.B,  false.B, false.B, CSROp.N,  false.B, false.B, false.B, false.B, false.B),
    SLTIU -> List(ImmType.I, ALUOp.SLTU,   0.U,     1.U,     true.B,  false.B, false.B, CSROp.N,  false.B, false.B, false.B, false.B, false.B),
    XORI  -> List(ImmType.I, ALUOp.XOR,    0.U,     1.U,     true.B,  false.B, false.B, CSROp.N,  false.B, false.B, false.B, false.B, false.B),
    ORI   -> List(ImmType.I, ALUOp.OR,     0.U,     1.U,     true.B,  false.B, false.B, CSROp.N,  false.B, false.B, false.B, false.B, false.B),
    ANDI  -> List(ImmType.I, ALUOp.AND,    0.U,     1.U,     true.B,  false.B, false.B, CSROp.N,  false.B, false.B, false.B, false.B, false.B),
    SLLI  -> List(ImmType.I, ALUOp.SLL,    0.U,     1.U,     true.B,  false.B, false.B, CSROp.N,  false.B, false.B, false.B, false.B, false.B),
    SRLI  -> List(ImmType.I, ALUOp.SRL,    0.U,     1.U,     true.B,  false.B, false.B, CSROp.N,  false.B, false.B, false.B, false.B, false.B),
    SRAI  -> List(ImmType.I, ALUOp.SRA,    0.U,     1.U,     true.B,  false.B, false.B, CSROp.N,  false.B, false.B, false.B, false.B, false.B),

    ADD   -> List(ImmType.Z, ALUOp.ADD,    0.U,     0.U,     true.B,  false.B, false.B, CSROp.N,  false.B, false.B, false.B, false.B, false.B),
    SUB   -> List(ImmType.Z, ALUOp.SUB,    0.U,     0.U,     true.B,  false.B, false.B, CSROp.N,  false.B, false.B, false.B, false.B, false.B),
    SLL   -> List(ImmType.Z, ALUOp.SLL,    0.U,     0.U,     true.B,  false.B, false.B, CSROp.N,  false.B, false.B, false.B, false.B, false.B),
    SLT   -> List(ImmType.Z, ALUOp.SLT,    0.U,     0.U,     true.B,  false.B, false.B, CSROp.N,  false.B, false.B, false.B, false.B, false.B),
    SLTU  -> List(ImmType.Z, ALUOp.SLTU,   0.U,     0.U,     true.B,  false.B, false.B, CSROp.N,  false.B, false.B, false.B, false.B, false.B),
    XOR   -> List(ImmType.Z, ALUOp.XOR,    0.U,     0.U,     true.B,  false.B, false.B, CSROp.N,  false.B, false.B, false.B, false.B, false.B),
    SRL   -> List(ImmType.Z, ALUOp.SRL,    0.U,     0.U,     true.B,  false.B, false.B, CSROp.N,  false.B, false.B, false.B, false.B, false.B),
    SRA   -> List(ImmType.Z, ALUOp.SRA,    0.U,     0.U,     true.B,  false.B, false.B, CSROp.N,  false.B, false.B, false.B, false.B, false.B),
    OR    -> List(ImmType.Z, ALUOp.OR,     0.U,     0.U,     true.B,  false.B, false.B, CSROp.N,  false.B, false.B, false.B, false.B, false.B),
    AND   -> List(ImmType.Z, ALUOp.AND,    0.U,     0.U,     true.B,  false.B, false.B, CSROp.N,  false.B, false.B, false.B, false.B, false.B),

    CSRRW -> List(ImmType.I, ALUOp.COPY_A, 0.U,     0.U,     true.B,  false.B, false.B, CSROp.W,  false.B, false.B, false.B, false.B, false.B),
    CSRRS -> List(ImmType.I, ALUOp.COPY_A, 0.U,     0.U,     true.B,  false.B, false.B, CSROp.S,  false.B, false.B, false.B, false.B, false.B),
    CSRRC -> List(ImmType.I, ALUOp.COPY_A, 0.U,     0.U,     true.B,  false.B, false.B, CSROp.C,  false.B, false.B, false.B, false.B, false.B),
    CSRRWI-> List(ImmType.I, ALUOp.COPY_A, 0.U,     1.U,     true.B,  false.B, false.B, CSROp.W,  false.B, false.B, false.B, false.B, false.B),
    CSRRSI-> List(ImmType.I, ALUOp.COPY_A, 0.U,     1.U,     true.B,  false.B, false.B, CSROp.S,  false.B, false.B, false.B, false.B, false.B),
    CSRRCI-> List(ImmType.I, ALUOp.COPY_A, 0.U,     1.U,     true.B,  false.B, false.B, CSROp.C,  false.B, false.B, false.B, false.B, false.B),

    ECALL -> List(ImmType.Z, ALUOp.NOP,    0.U,     0.U,     false.B, false.B, false.B, CSROp.N,  false.B, false.B, true.B,  false.B, false.B),
    MRET  -> List(ImmType.Z, ALUOp.NOP,    0.U,     0.U,     false.B, false.B, false.B, CSROp.N,  false.B, false.B, false.B, true.B,  false.B),
    EBREAK-> List(ImmType.Z, ALUOp.NOP,    0.U,     0.U,     false.B, false.B, false.B, CSROp.N,  false.B, false.B, false.B, false.B, true.B)
  )

  /**
   * 译码核心方法
   * 返回 (ControlSignals, ImmType)
   */
  def decode(inst: UInt): (ControlSignals, ImmType) = {
    val sigs = ListLookup(inst, default, table)
    
    val ctrl = Wire(new ControlSignals)
    val immType = sigs(0).asTypeOf(ImmType())
    
    ctrl.aluOp     := sigs(1).asTypeOf(ALUOp())
    ctrl.op1Sel    := sigs(2).asUInt
    ctrl.op2Sel    := sigs(3).asUInt
    ctrl.regWen    := sigs(4).asBool
    ctrl.memEn     := sigs(5).asBool
    ctrl.memWen    := sigs(6).asBool
    ctrl.csrOp     := sigs(7).asTypeOf(CSROp())
    ctrl.isBranch  := sigs(8).asBool
    ctrl.isJump    := sigs(9).asBool
    ctrl.isEcall   := sigs(10).asBool
    ctrl.isMret    := sigs(11).asBool
    ctrl.isEbreak  := sigs(12).asBool
    
    // 固定的指令字段提取
    ctrl.memFunct3 := inst(14, 12)
    
    (ctrl, immType)
  }
}