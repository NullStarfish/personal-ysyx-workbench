package labcpu.mem

import chisel3._
import mycpu.common._

class CompatibleIMem extends BlackBox {
  val io = IO(new Bundle {
    val addr = Input(UInt(XLEN.W))
    val rdata = Output(UInt(32.W))
  })
}

class CompatibleDMem extends BlackBox {
  val io = IO(new Bundle {
    val clock = Input(Clock())

    val addr = Input(UInt(XLEN.W))
    val ren = Input(Bool())
    val wen = Input(Bool())
    val subop = Input(UInt(3.W))
    val unsignedLoad = Input(Bool())
    val wdata = Input(UInt(XLEN.W))
    val rdata = Output(UInt(XLEN.W))

    val debugAddr = Input(UInt(XLEN.W))
    val debugByte = Output(UInt(8.W))
  })
}
