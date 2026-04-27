package labcpu.core.backend

import chisel3._
import chisel3.util._
import mycpu.common._
import mycpu.core.bundles._

class WriteBack(enableTraceFields: Boolean = ENABLE_TRACE_FIELDS) extends Module {
  val io = IO(new Bundle {
    val in = Input(Valid(new ExecutePacket(enableTraceFields)))
    val traceCommit = if (enableTraceFields) Some(Output(Valid(new TraceCarryBundle))) else None
    val dmemRdata = Input(UInt(XLEN.W))
    val out = Output(new MemoryPacket(enableTraceFields))
    val regWrite = Output(new WriteBackIO)
  })

  val wbData = Mux(
    io.in.bits.mem.valid && !io.in.bits.mem.write,
    io.dmemRdata,
    io.in.bits.result,
  )
  io.out.wb := io.in.bits.wb
  io.out.wbData := wbData
  if (enableTraceFields) {
    io.out.trace.get := io.in.bits.trace.get
    io.out.trace.get.memValid := io.in.valid

    io.traceCommit.get.valid := io.in.valid
    io.traceCommit.get.bits := io.out.trace.get
    io.traceCommit.get.bits.regWen := io.in.bits.wb.regWen
    io.traceCommit.get.bits.rd := io.in.bits.wb.rd
    io.traceCommit.get.bits.data := wbData
  }

  io.regWrite.wen := io.in.valid && io.in.bits.wb.regWen
  io.regWrite.addr := io.in.bits.wb.rd
  io.regWrite.data := wbData
}
