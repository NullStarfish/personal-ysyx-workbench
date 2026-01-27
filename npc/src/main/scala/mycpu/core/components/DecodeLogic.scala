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

    // MEM 配置：将位宽和符号扩展信息直接“注入”内存驱动
    when(opcode === "b0000011".U || opcode === "b0100011".U) {
      mem.ioctl(funct3, 0.U) 
    }

    // --- 2. 意图分发表 ---
    val S_ALU = ServiceType.ALU;   val S_MR = ServiceType.MEM_RD; val S_MW = ServiceType.MEM_WR
    val S_BR  = ServiceType.BRANCH; val S_CSR = ServiceType.CSR;   val S_NO = ServiceType.NONE
    
    val A1_R = Arg1Type.REG;  val A1_P = Arg1Type.PC;  val A1_Z = Arg1Type.ZERO; val A1_ZM = Arg1Type.ZIMM
    val A2_R = Arg2Type.REG;  val A2_I = Arg2Type.IMM; val A2_4 = Arg2Type.CONST_4
    
    val Y = true.B; val N = false.B

    val map = Array(
      // 算术
      ADDI  -> List(S_ALU, A1_R,  A2_I, ImmType.I, Y),
      ADD   -> List(S_ALU, A1_R,  A2_R, ImmType.Z, Y),
      LUI   -> List(S_ALU, A1_Z,  A2_I, ImmType.U, Y),
      AUIPC -> List(S_ALU, A1_P,  A2_I, ImmType.U, Y),
      // 访存
      LW    -> List(S_MR,  A1_R,  A2_I, ImmType.I, Y),
      SW    -> List(S_MW,  A1_R,  A2_I, ImmType.S, N),
      // 跳转 (JAL/JALR 意图为 BRANCH，且写回 PC+4 到 RD)
      JAL   -> List(S_BR,  A1_P,  A2_I, ImmType.J, Y),
      JALR  -> List(S_BR,  A1_R,  A2_I, ImmType.I, Y),
      BEQ   -> List(S_BR,  A1_R,  A2_R, ImmType.B, N),
      // CSR
      CSRRW -> List(S_CSR, A1_R,  A2_R, ImmType.Z, Y),
      CSRRWI-> List(S_CSR, A1_ZM, A2_R, ImmType.Z, Y)
    )

    val decoded = ListLookup(inst, List(S_NO, A1_R, A2_R, ImmType.Z, N), map)

    ctrl.service  := decoded(0).asTypeOf(ServiceType())
    ctrl.arg1     := decoded(1).asTypeOf(Arg1Type())
    ctrl.arg2     := decoded(2).asTypeOf(Arg2Type())
    ctrl.immType  := decoded(3).asTypeOf(ImmType())
    ctrl.regWen   := decoded(4).asTypeOf(Bool())
    ctrl
  }
}