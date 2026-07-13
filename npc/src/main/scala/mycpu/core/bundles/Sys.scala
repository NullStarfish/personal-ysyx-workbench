package mycpu.core.bundles

import chisel3._
import mycpu.common._

trait ExceptionInfo {
  def pc: UInt
  def no: ExceptionNumber.Type
  def valid: Bool
}

trait InstInfo {
  def pc: UInt
  def except: ExceptionInfo
}

trait CsrInfo {
  def csrOp: CSROp.Type
  def csrAddr: UInt
  def wdata: UInt
}

trait SysInfo {
  def ebreak: Bool
  def mret: Bool
  def fencei: Bool
  def csr: CsrInfo
}

class ExceptionBundle extends Bundle with ExceptionInfo {
  val pc = XLenU
  val no = ExceptionNumber()
  val valid = Bool()
}

class InstPacket extends Bundle with InstInfo {
  val pc = XLenU
  val except = new Bundle with ExceptionInfo {
    def pc = InstPacket.this.pc
    val no = ExceptionNumber()
    val valid = Bool()
  }
}
