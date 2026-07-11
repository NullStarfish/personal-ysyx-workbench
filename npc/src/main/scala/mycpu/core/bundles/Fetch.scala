package mycpu.core.bundles

import chisel3._
import chisel3.util._
import mycpu.common._

trait FetchRedirect {
  def redirect : ValidUIntView
}



class FetchPacket extends Bundle {
  val pc = XLenU
  val inst = UInt(32.W)
  val icacheHit = Bool()
  val isException = Bool()
}

class IFPacket extends Bundle {
  val pc = XLenU
}
