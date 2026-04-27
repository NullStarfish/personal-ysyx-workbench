package labcpu.top

import _root_.circt.stage.ChiselStage
import chisel3._
import labcpu.core.CourseCore
import labcpu.core.bundles.{DataMemIO, InstMemIO}
import mycpu.common._

class CourseSynthTop extends Module {
  override val desiredName = "CourseSynthTop"

  private val CourseStartAddr = BigInt("80000000", 16)

  val io = IO(new Bundle {
    val imem = new InstMemIO
    val dmem = new DataMemIO
  })

  val core = Module(
    new CourseCore(
      startAddr = CourseStartAddr,
      enableDpi = false,
      enableTracer = false,
      enableTraceFields = false,
      enableSys = false,
      enableSimEbreak = false,
    ),
  )

  io.imem.addr := core.io.imem.addr
  core.io.imem.rdata := io.imem.rdata

  io.dmem.addr := core.io.dmem.addr
  io.dmem.ren := core.io.dmem.ren
  io.dmem.wen := core.io.dmem.wen
  io.dmem.subop := core.io.dmem.subop
  io.dmem.unsigned := core.io.dmem.unsigned
  io.dmem.wdata := core.io.dmem.wdata
  core.io.dmem.rdata := io.dmem.rdata
}

object GenCourseSynthTop extends App {
  ChiselStage.emitSystemVerilogFile(
    new CourseSynthTop,
    args = Array("--target-dir", "src/main/verilog/CourseSynth"),
    firtoolOpts = Array("--disable-all-randomization"),
  )
}
