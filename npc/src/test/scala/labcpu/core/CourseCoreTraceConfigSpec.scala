package labcpu.core

import chisel3._
import chisel3.simulator.EphemeralSimulator._
import org.scalatest.flatspec.AnyFlatSpec
import scala.collection.mutable

class CourseCoreTraceConfigSpec extends AnyFlatSpec {
  "CourseCore" should "run the same simple program with trace disabled" in {
    simulate(new CourseCore(startAddr = 0x0L, enableTracer = false, enableTraceFields = false)) { c =>
      val imem = Map[Long, Long](
        0x00L -> 0x00500093L, // addi x1, x0, 5
        0x04L -> 0x00700113L, // addi x2, x0, 7
        0x08L -> 0x002081b3L, // add x3, x1, x2
        0x0cL -> 0x00302023L, // sw x3, 0(x0)
        0x10L -> 0x00002203L, // lw x4, 0(x0)
        0x14L -> 0x00100073L, // ebreak
      )
      val dmem = mutable.Map.empty[Long, Byte]

      c.reset.poke(true.B)
      CourseCoreTestUtils.tickMemory(c, imem, dmem)
      c.clock.step()
      c.reset.poke(false.B)

      for (_ <- 0 until 40) {
        CourseCoreTestUtils.tickMemory(c, imem, dmem)
        c.clock.step()
      }

      c.io.debug_regs(3).expect(12.U)
      c.io.debug_regs(4).expect(12.U)
      c.io.trace.retireCount.expect(0.U)
      c.io.trace.branchCount.expect(0.U)
    }
  }
}
