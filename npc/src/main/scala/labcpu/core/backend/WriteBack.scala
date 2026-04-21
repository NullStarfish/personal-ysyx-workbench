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
    val retire = Output(new RetireEventBundle)
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

    io.retire.pc := io.in.bits.trace.get.pc
    io.retire.dnpc := io.in.bits.trace.get.dnpc
    io.retire.inst := io.in.bits.trace.get.inst
  } else {
    io.retire.pc := 0.U
    io.retire.dnpc := 0.U
    io.retire.inst := 0.U
  }

  io.regWrite.wen := io.in.valid && io.in.bits.wb.regWen
  io.regWrite.addr := io.in.bits.wb.rd
  io.regWrite.data := wbData

  io.retire.valid := io.in.valid
  io.retire.regWen := io.in.bits.wb.regWen
  io.retire.rd := io.in.bits.wb.rd
  io.retire.data := wbData
}
