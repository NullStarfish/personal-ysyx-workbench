package mycpu.core.bundles

import chisel3._
import mycpu.common._

class ControlSignals extends Bundle {
  val aluOp      = ALUOp()
  val csrOp      = CSROp()
  val regWen     = Bool()
  val memEn      = Bool()
  val memWen     = Bool() // 0: Read, 1: Write
  val memFunct3  = UInt(3.W)
  val op1Sel     = UInt(1.W) // 0: Reg, 1: PC
  val op2Sel     = UInt(1.W) // 0: Reg, 1: Imm
  val isJump     = Bool()    // JAL/JALR
  val isBranch   = Bool()
  val isEcall    = Bool()
  val isMret     = Bool()
  val isEbreak = Bool() // 新增
}