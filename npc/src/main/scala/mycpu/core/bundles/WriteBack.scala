package mycpu.core.bundles
import chisel3._
import mycpu.common._

class WritebackCtrlBundle extends Bundle {
  val wen = Bool()
  val rd = UInt(5.W)
}


class WriteBackIO extends Bundle {
  val regWrite = new RegWriteMeta
}
