package labcpu.core.bundles

import chisel3._
import mycpu.common._

class InstMemIO extends Bundle {
  val addr = Output(UInt(XLEN.W))
  val rdata = Input(UInt(32.W))
}

class DataMemIO extends Bundle {
  val addr = Output(UInt(XLEN.W))
  val ren = Output(Bool())
  val wen = Output(Bool())
  val subop = Output(UInt(3.W))
  val unsigned = Output(Bool())
  val wdata = Output(UInt(XLEN.W))
  val rdata = Input(UInt(XLEN.W))
}
