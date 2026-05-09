package mycpu.core.bundles

import chisel3._
import chisel3.util._
import mycpu.common._



class FetchPacket extends Bundle {
  val pc = XLenU
  val inst = UInt(32.W)
  val isException = Bool()

}


