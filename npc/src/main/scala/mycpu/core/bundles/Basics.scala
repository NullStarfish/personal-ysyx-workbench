package mycpu.core.bundles
import chisel3._
import mycpu.common._

trait ValidUIntView {
    def valid: Bool
    def bits: UInt
}

class RegWriteMeta extends Bundle {
  val rd = UInt(5.W)
  val wen = Bool()
  val wdata = XLenU
}

object InstType {
  def apply()  = UInt(2.W)
  val arith = 0.U
  val mem = 1.U
  val redirect = 2.U
  val sys = 3.U
}


trait ForwardSource {
  def valid: Bool
  def addr: UInt
  def data: UInt
}

class BypassCtrlBundle extends Bundle {
  val rs1Addr = UInt(5.W)
  val rs2Addr = UInt(5.W)
}
