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

class CsrWriteBackIO extends Bundle {
  val cmd = Output(CSROp())
  val addr = Output(UInt(12.W))
  val wdata = Output(XLenU)
  val except = Output(new ExceptionBundle)
  val isMret = Output(Bool())

  val rdata = Input(XLenU)
  val evec = Input(XLenU)
  val epc = Input(XLenU)
  val retireCsrs = Input(new CsrDebugBundle)
}
