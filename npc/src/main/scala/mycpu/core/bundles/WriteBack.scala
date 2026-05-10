package mycpu.core.bundles
import chisel3._
import mycpu.common._

trait WriteBackCtrl {
  def wen : Bool
  def  rd : UInt
}

trait WriteBackData {
  def wdata: UInt
}



class WriteBackIO extends Bundle {
  val regWrite = new RegWriteMeta
}
