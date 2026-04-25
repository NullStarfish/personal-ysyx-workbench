package labcpu.top

import _root_.circt.stage.ChiselStage
import chisel3._
import chisel3.util.Cat
import labcpu.core.CourseCore
import labcpu.mem.{CompatibleDMem, CompatibleIMem}
import mycpu.common._

class CourseCompatibleTop extends Module {
  override val desiredName = "CourseCompatibleTop"

  val io = IO(new Bundle {
    val debug_regs_flat = Output(UInt((32 * XLEN).W))
    val debug_pc = Output(UInt(XLEN.W))
    val debug_dmem_addr = Input(UInt(XLEN.W))
    val debug_dmem_byte = Output(UInt(8.W))
    val retire_valid = Output(Bool())
    val retire_pc = Output(UInt(XLEN.W))
    val retire_inst = Output(UInt(32.W))
  })

  val core = Module(new CourseCore(
    startAddr = 0,
    enableDpi = false,
    enableTracer = false,
    enableTraceFields = true,
  ))
  val imem = Module(new CompatibleIMem)
  val dmem = Module(new CompatibleDMem)

  imem.io.addr := core.io.imem.addr
  core.io.imem.rdata := imem.io.rdata

  dmem.io.clock := clock
  dmem.io.addr := core.io.dmem.addr
  dmem.io.ren := core.io.dmem.ren
  dmem.io.wen := core.io.dmem.wen
  dmem.io.subop := core.io.dmem.subop
  dmem.io.unsignedLoad := core.io.dmem.unsigned
  dmem.io.wdata := core.io.dmem.wdata
  core.io.dmem.rdata := dmem.io.rdata
  dmem.io.debugAddr := io.debug_dmem_addr
  io.debug_dmem_byte := dmem.io.debugByte

  io.debug_regs_flat := Cat(core.io.debug_regs.reverse)
  io.debug_pc := core.io.debug_pc
  io.retire_valid := core.io.retire_valid
  io.retire_pc := core.io.retire_pc
  io.retire_inst := core.io.retire_inst
}

object GenCourseCompatibleTop extends App {
  ChiselStage.emitSystemVerilogFile(
    new CourseCompatibleTop,
    args = Array("--target-dir", "src/main/verilog/CourseCompatible"),
    firtoolOpts = Array("--disable-all-randomization"),
  )
}
