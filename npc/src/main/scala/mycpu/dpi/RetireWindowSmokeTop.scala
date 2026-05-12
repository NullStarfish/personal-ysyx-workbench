package mycpu.dpi

import _root_.circt.stage.ChiselStage
import chisel3._
import chisel3.util._
import mycpu.common._
import mycpu.common.Instructions
import mycpu.core.bundles._
import mycpu.core.components.Tracer

class RetireWindowSmokeTop extends Module {
  val io = IO(new Bundle {
    val start = Input(Bool())
    val done = Output(Bool())
  })

  val tracer = Module(new Tracer(enableDpi = true))

  val regs = RegInit(VecInit(Seq.fill(32)(0.U(XLEN.W))))
  val mtvecReg = RegInit(0.U(XLEN.W))
  val mepcReg = RegInit(0.U(XLEN.W))
  val mstatusReg = RegInit("h00001800".U(XLEN.W))
  val mcauseReg = RegInit(0.U(XLEN.W))

  val idle :: retire0 :: retire1 :: finished :: Nil = Enum(4)
  val stateReg = RegInit(idle)

  val retireTrace = WireInit(0.U.asTypeOf(Valid(new RetireTrace)))

  switch(stateReg) {
    is(idle) {
      when(io.start) {
        stateReg := retire0
      }
    }
    is(retire0) {
      retireTrace.valid := true.B
      retireTrace.bits.pc := "h30000000".U
      retireTrace.bits.dnpc := "h30000004".U
      retireTrace.bits.inst := "h00100093".U // addi x1, x0, 1
      retireTrace.bits.regWrite.wen := true.B
      retireTrace.bits.regWrite.rd := 1.U
      retireTrace.bits.regWrite.wdata := 1.U
      regs(1) := 1.U
      stateReg := retire1
    }
    is(retire1) {
      retireTrace.valid := true.B
      retireTrace.bits.pc := "h30000004".U
      retireTrace.bits.dnpc := "h30000008".U
      retireTrace.bits.inst := Instructions.EBREAK.value.U
      retireTrace.bits.regWrite.wen := false.B
      retireTrace.bits.regWrite.rd := 0.U
      retireTrace.bits.regWrite.wdata := 0.U
      stateReg := finished
    }
  }

  tracer.io.retireTrace := retireTrace
  tracer.io.gprs := regs
  tracer.io.csrs.mtvec := mtvecReg
  tracer.io.csrs.mepc := mepcReg
  tracer.io.csrs.mstatus := mstatusReg
  tracer.io.csrs.mcause := mcauseReg

  io.done := stateReg === finished
}

object GenRetireWindowSmokeTop extends App {
  ChiselStage.emitSystemVerilogFile(
    new RetireWindowSmokeTop,
    Array("--target-dir", "generated/retire_window_smoke"),
  )
}
