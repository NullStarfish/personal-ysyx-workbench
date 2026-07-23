package mycpu.core.backend

import chisel3._
import chisel3.util._
import mycpu.common._
import mycpu.core.bundles._
import mycpu.dpi.SimEbreakDPI

class WriteBack(
    enableTraceFields: Boolean = ENABLE_TRACE_FIELDS,
    enableDpi: Boolean = false,
) extends Module {
  val io = IO(new Bundle {
    val in = Flipped(Decoupled(new MemoryPacket))
    val retireTrace = if (enableTraceFields) Some(Output(Valid(new RetireTrace))) else None
    val regWrite = new WriteBackIO()
    val csr = new CsrWriteBackIO
    val redirect = Valid(XLenU)
    val fencei = Output(Bool())
  })

  io.in.ready := true.B

  val retire = io.in.fire
  val sys = io.in.bits.sys
  val isCsr = sys.csr.csrOp =/= CSROp.N
  val writeData = Mux(isCsr, io.csr.rdata, io.in.bits.wbData.wdata)
  val hasSysRedirect = io.in.bits.inst.except.valid || sys.mret || sys.fencei

  io.csr.cmd := Mux(retire, sys.csr.csrOp, CSROp.N)
  io.csr.addr := sys.csr.csrAddr
  io.csr.wdata := sys.csr.wdata
  io.csr.except.pc := io.in.bits.inst.except.pc
  io.csr.except.no := io.in.bits.inst.except.no
  io.csr.except.valid := retire && io.in.bits.inst.except.valid
  io.csr.isMret := retire && sys.mret

  io.redirect.valid := retire && hasSysRedirect
  io.redirect.bits := MuxCase(io.csr.evec, Seq(
    sys.mret -> io.csr.epc,
    sys.fencei -> (io.in.bits.inst.pc + 4.U),
  ))
  io.fencei := retire && sys.fencei

  io.regWrite.regWrite.wen := retire && io.in.bits.wbCtrl.wen
  io.regWrite.regWrite.rd := io.in.bits.wbCtrl.rd
  io.regWrite.regWrite.wdata := writeData

  if (enableDpi) {
    val simEbreak = Module(new SimEbreakDPI)
    simEbreak.io.clock := clock
    simEbreak.io.reset := reset.asBool
    simEbreak.io.valid := retire && sys.ebreak
    simEbreak.io.is_ebreak := 0.U
  }

  if (enableTraceFields) {
    io.retireTrace.get.valid := retire
    io.retireTrace.get.bits := io.in.bits.retireTrace.get
    when(hasSysRedirect) {
      io.retireTrace.get.bits.dnpc := io.redirect.bits
    }
    io.retireTrace.get.bits.regWrite.wen := io.in.bits.wbCtrl.wen
    io.retireTrace.get.bits.regWrite.rd := io.in.bits.wbCtrl.rd
    io.retireTrace.get.bits.regWrite.wdata := writeData
    io.retireTrace.get.bits.csrs := io.csr.retireCsrs
  }
}
