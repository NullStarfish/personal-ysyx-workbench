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

class ForwardPacket extends Bundle with ForwardSource {
  val valid = Bool()
  val addr = UInt(5.W)
  val data = XLenU
}

class BypassCtrlBundle extends Bundle {
  val rs1Addr = UInt(5.W)
  val rs2Addr = UInt(5.W)
}


object ExceptionNumber extends ChiselEnum {
  val InstAddrMisaligned = Value(0.U)
  val InstAccessFault = Value(1.U)
  val IllegalInst = Value(2.U)
  val Breakpoint = Value(3.U)
  val LoadAddrMisaligned = Value(4.U)
  val LoadAccessFault = Value(5.U)
  val StoreAddrMisaligned = Value(6.U)
  val StoreAccessFault = Value(7.U)
  val ECallU = Value(8.U)
  val ECallS = Value(9.U)
  val ECallM = Value(11.U)
  val InstPageFault = Value(12.U)
  val LoadPageFault = Value(13.U)
  val StorePageFault = Value(15.U)
}
