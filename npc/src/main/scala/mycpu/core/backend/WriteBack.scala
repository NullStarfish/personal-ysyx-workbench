package mycpu.core.backend

import chisel3._
import chisel3.util._
import mycpu.common._
import mycpu.core.bundles._

class WriteBack(enableTraceFields: Boolean = ENABLE_TRACE_FIELDS) extends Module {
  val io = IO(new Bundle {
    val in = Flipped(Decoupled(new MemoryPacket))
    val traceCommit = if (enableTraceFields) Some(Output(Valid(new RetireTrace))) else None
    val regWrite = new WriteBackIO()
  })

  io.in.ready := true.B

  io.regWrite.regWrite.wen := io.in.valid && io.in.bits.wbCtrl.wen
  io.regWrite.regWrite.rd := io.in.bits.wbCtrl.rd
  io.regWrite.regWrite.wdata := io.in.bits.wbData.wdata

  if (enableTraceFields) {
    io.traceCommit.get.valid := io.in.valid
    io.traceCommit.get.bits := io.in.bits.retireTrace.get
    io.traceCommit.get.bits.regWrite.wen := io.in.bits.wbCtrl.wen
    io.traceCommit.get.bits.regWrite.rd := io.in.bits.wbCtrl.rd
    io.traceCommit.get.bits.regWrite.wdata := io.in.bits.wbData.wdata
  }
}
