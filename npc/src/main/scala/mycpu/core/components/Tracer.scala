package mycpu.core.components

import chisel3._
import chisel3.util._
import mycpu.core.bundles._
import mycpu.dpi.{DpiApi, SimStateBundle}
import mycpu.common._

class Tracer(enableDpi: Boolean = false) extends Module {
  val io = IO(new Bundle {
    val retireTrace = Input(Valid(new RetireTrace))
    val gprs = Input(Vec(32, UInt(32.W)))
    val csrs = Input(new Bundle {
      val mtvec   = UInt(XLEN.W)
      val mepc    = UInt(XLEN.W)
      val mstatus = UInt(XLEN.W)
      val mcause  = UInt(XLEN.W)
    })
    val simState = Output(new SimStateBundle)
  })

  val trace = io.retireTrace.bits

  io.simState.valid := io.retireTrace.valid
  io.simState.pc := trace.pc
  io.simState.dnpc := trace.dnpc
  io.simState.regWen := trace.regWrite.wen
  io.simState.regAddr := trace.regWrite.rd
  io.simState.regData := trace.regWrite.wdata
  io.simState.regsFlat := Cat(io.gprs.reverse)
  io.simState.mtvec := trace.csrs.mtvec
  io.simState.mepc := trace.csrs.mepc
  io.simState.mstatus := trace.csrs.mstatus
  io.simState.mcause := trace.csrs.mcause
  io.simState.inst := trace.inst
  io.simState.instType := trace.instType

  if (enableDpi) {
    DpiApi.simState(clock, reset.asBool, io.simState, localName = "core_sim_state")
  }
}
