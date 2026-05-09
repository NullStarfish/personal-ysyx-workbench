package mycpu.core.bundles
import chisel3._


object ALUSrcA {
  def apply(): UInt = UInt(1.W)
  val Rs1 = 0.U(1.W)
  val Pc  = 1.U(1.W)
}


object ALUSrcB {
  def apply(): UInt = UInt(1.W)
  val Rs2 = 0.U(1.W)
  val Imm = 1.U(1.W)
}

object WBSel {
  val Alu    = "b00".U(2.W)
  val Csr    = "b01".U(2.W)
  val PcPlus4 = "b10".U(2.W)
}

object BranchType {
  def apply(): UInt = UInt(3.W)
  val None = 0.U(3.W)
  val Eq   = 1.U(3.W)
  val Ne   = 2.U(3.W)
  val Lt   = 3.U(3.W)
  val Ge   = 4.U(3.W)
  val Ltu  = 5.U(3.W)
  val Geu  = 6.U(3.W)
}


object ExecSubop {
  val None = "b000".U(3.W)
  val Byte = "b001".U(3.W)
  val Half = "b010".U(3.W)
  val Word = "b011".U(3.W)
}

object DecodeFormat {
  val None         = "b0000".U(4.W)
  val Reg_Reg       = "b0001".U(4.W)
  val Reg_Imm       = "b0010".U(4.W)
  val Pc_Imm        = "b0011".U(4.W)
  val Pc_Offset     = "b0100".U(4.W)
  val Reg_Offset    = "b0101".U(4.W)
  val Reg_Reg_Offset = "b0110".U(4.W)
  val CsrReg       = "b0111".U(4.W)
  val CsrImm       = "b1000".U(4.W)
  val Sys          = "b1001".U(4.W)
}
