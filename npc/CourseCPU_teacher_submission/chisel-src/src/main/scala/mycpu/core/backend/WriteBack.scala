package mycpu.core.backend

import chisel3._
import chisel3.util._
import mycpu.common._
import mycpu.core.bundles._

class WriteBack(enableTraceFields: Boolean = ENABLE_TRACE_FIELDS) extends Module {
  val io = IO(new Bundle {
    val in       = Flipped(Decoupled(new MemoryPacket(enableTraceFields)))
    val traceCommit = if (enableTraceFields) Some(Output(Valid(new TraceCarryBundle))) else None
    val regWrite = new WriteBackIO()
  })

  io.in.ready := true.B

  io.regWrite.wen  := io.in.valid && io.in.bits.wb.regWen
  io.regWrite.addr := io.in.bits.wb.rd
  io.regWrite.data := io.in.bits.wbData

  if (enableTraceFields) {
    io.traceCommit.get.valid := io.in.valid
    io.traceCommit.get.bits := io.in.bits.trace.get
    io.traceCommit.get.bits.regWen := io.in.bits.wb.regWen
    io.traceCommit.get.bits.rd := io.in.bits.wb.rd
    io.traceCommit.get.bits.data := io.in.bits.wbData
  }
}
