package labcpu.core

import chisel3._
import chisel3.simulator.EphemeralSimulator._
import org.scalatest.flatspec.AnyFlatSpec
import scala.collection.mutable

class TraceConfigEquivalenceSpec extends AnyFlatSpec {
  case class FinalState(
      regs: Seq[BigInt],
      dmem: Map[Long, Byte],
      retirePc: BigInt,
      retireInst: BigInt,
  )

  private def runAndCollect(enableTrace: Boolean): FinalState = {
    var captured: Option[FinalState] = None
    simulate(new CourseCore(
      startAddr = 0x0L,
      enableTracer = enableTrace,
      enableTraceFields = enableTrace,
    )) { c =>
      val imem = Map[Long, Long](
        0x00L -> 0x00500093L, // addi x1, x0, 5
        0x04L -> 0x00700113L, // addi x2, x0, 7
        0x08L -> 0x002081b3L, // add x3, x1, x2
        0x0cL -> 0x00302023L, // sw x3, 0(x0)
        0x10L -> 0x00002203L, // lw x4, 0(x0)
        0x14L -> 0x00100093L, // addi x1, x0, 1
        0x18L -> 0x00108463L, // beq x1, x1, 8
        0x1cL -> 0x00000293L, // addi x5, x0, 0 (flushed)
        0x20L -> 0x00900293L, // addi x5, x0, 9
        0x24L -> 0x00000013L, // nop
        0x28L -> 0x00100313L, // addi x6, x0, 1
        0x2cL -> 0x006000a3L, // sb x6, 1(x0)
        0x30L -> 0x00104403L, // lbu x8, 1(x0)
        0x34L -> 0x008004efL, // jal x9, 8
        0x38L -> 0x00000493L, // addi x9, x0, 0 (skipped)
        0x3cL -> 0x00100073L, // ebreak
      )
      val dmem = mutable.Map.empty[Long, Byte]

      c.reset.poke(true.B)
      CourseCoreTestUtils.tickMemory(c, imem, dmem)
      c.clock.step()
      c.reset.poke(false.B)

      for (_ <- 0 until 80) {
        CourseCoreTestUtils.tickMemory(c, imem, dmem)
        c.clock.step()
      }

      captured = Some(FinalState(
        regs = (0 until 32).map(i => c.io.debug_regs(i).peek().litValue),
        dmem = dmem.toMap,
        retirePc = c.io.retire_pc.peek().litValue,
        retireInst = c.io.retire_inst.peek().litValue,
      ))
    }
    captured.get
  }

  "CourseCore" should "preserve architectural results when trace fields are disabled" in {
    val traceOn = runAndCollect(enableTrace = true)
    val traceOff = runAndCollect(enableTrace = false)

    assert(traceOn.regs == traceOff.regs, s"register state diverged: on=${traceOn.regs} off=${traceOff.regs}")
    assert(traceOn.dmem == traceOff.dmem, s"data memory diverged: on=${traceOn.dmem} off=${traceOff.dmem}")
    assert(traceOn.retirePc != 0, "trace-enabled run should expose a non-zero retire pc")
    assert(traceOn.retireInst != 0, "trace-enabled run should expose a non-zero retire inst")
  }
}
