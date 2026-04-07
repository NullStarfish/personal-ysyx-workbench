package mycpu.dpi

import chisel3._

final class SimStateBundle extends Bundle {
  val valid = Bool()
  val pc = UInt(32.W)
  val dnpc = UInt(32.W)
  val regsFlat = UInt(1024.W)
  val mtvec = UInt(32.W)
  val mepc = UInt(32.W)
  val mstatus = UInt(32.W)
  val mcause = UInt(32.W)
  val inst = UInt(32.W)
}
