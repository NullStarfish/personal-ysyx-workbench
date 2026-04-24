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

  val commitTrace = WireInit(0.U.asTypeOf(Valid(new TraceCarryBundle)))

  switch(stateReg) {
    is(idle) {
      when(io.start) {
        stateReg := retire0
      }
    }
    is(retire0) {
      commitTrace.valid := true.B
      commitTrace.bits.pc := "h30000000".U
      commitTrace.bits.dnpc := "h30000004".U
      commitTrace.bits.inst := "h00100093".U // addi x1, x0, 1
      commitTrace.bits.regWen := true.B
      commitTrace.bits.rd := 1.U
      commitTrace.bits.data := 1.U
      regs(1) := 1.U
      stateReg := retire1
    }
    is(retire1) {
      commitTrace.valid := true.B
      commitTrace.bits.pc := "h30000004".U
      commitTrace.bits.dnpc := "h30000008".U
      commitTrace.bits.inst := Instructions.EBREAK.value.U
      commitTrace.bits.regWen := false.B
      commitTrace.bits.rd := 0.U
      commitTrace.bits.data := 0.U
      stateReg := finished
    }
  }

  tracer.io.commitTrace := commitTrace
  tracer.io.regsFlat := Cat(regs.reverse)
  tracer.io.mtvec := mtvecReg
  tracer.io.mepc := mepcReg
  tracer.io.mstatus := mstatusReg
  tracer.io.mcause := mcauseReg
  tracer.io.flush := false.B

  io.done := stateReg === finished
}

object GenRetireWindowSmokeTop extends App {
  ChiselStage.emitSystemVerilogFile(
    new RetireWindowSmokeTop,
    Array("--target-dir", "generated/retire_window_smoke"),
  )
}
