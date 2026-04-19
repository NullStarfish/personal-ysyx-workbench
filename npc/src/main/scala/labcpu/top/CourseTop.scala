package labcpu.top

import _root_.circt.stage.ChiselStage
import chisel3._
import labcpu.core.CourseCore
import labcpu.mem.SRAM
import mycpu.common._

class CourseTop extends Module {
  override val desiredName = "CourseTop"

  private val CourseStartAddr = BigInt("80000000", 16)

  val io = IO(new Bundle {
    val debug_pc = Output(UInt(XLEN.W))
    val retire_valid = Output(Bool())
    val retire_pc = Output(UInt(XLEN.W))
    val retire_inst = Output(UInt(32.W))
  })

  val core = Module(new CourseCore(startAddr = CourseStartAddr, enableDpi = true))
  val sram = Module(new SRAM)

  sram.io.clock := clock

  sram.io.imem_addr := core.io.imem.addr
  core.io.imem.rdata := sram.io.imem_rdata

  sram.io.dmem_addr := core.io.dmem.addr
  sram.io.dmem_ren := core.io.dmem.ren
  sram.io.dmem_wen := core.io.dmem.wen
  sram.io.dmem_subop := core.io.dmem.subop
  sram.io.dmem_unsigned := core.io.dmem.unsigned
  sram.io.dmem_wdata := core.io.dmem.wdata
  core.io.dmem.rdata := sram.io.dmem_rdata

  io.debug_pc := core.io.debug_pc
  io.retire_valid := core.io.retire_valid
  io.retire_pc := core.io.retire_pc
  io.retire_inst := core.io.retire_inst
}

object GenCourseTop extends App {
  ChiselStage.emitSystemVerilogFile(
    new CourseTop,
    args = Array("--target-dir", "src/main/verilog/Course"),
    firtoolOpts = Array("--disable-all-randomization"),
  )
}
