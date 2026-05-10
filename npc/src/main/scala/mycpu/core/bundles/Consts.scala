package mycpu.core.bundles
import chisel3._



object BranchType extends ChiselEnum {
  val None, Eq, Ne, Lt, Ge, Ltu, Geu = Value
}


object SizeSubop extends ChiselEnum {
  val None, Byte, Half, Word = Value
}

object DecodeFormat extends ChiselEnum {
  val None, Reg_Reg, Reg_Imm, Pc_Imm, Pc_Offset = Value
  val Reg_Offset, Reg_Reg_Offset, CsrReg, CsrImm, Sys = Value
}
