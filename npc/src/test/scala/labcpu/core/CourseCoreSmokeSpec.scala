package labcpu.core

import chisel3._
import chisel3.simulator.EphemeralSimulator._
import scala.collection.mutable
import org.scalatest.flatspec.AnyFlatSpec
import CourseCoreTestUtils._

class CourseCoreSmokeSpec extends AnyFlatSpec {
  "CourseCore" should "run a simple arithmetic, memory, and control-flow program" in {
    simulate(new CourseCore(startAddr = 0x0L)) { c =>
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

      runProgram(c, imem, dmem, maxCycles = 80)
      c.io.debug_regs(3).expect(12.U)
      c.io.debug_regs(4).expect(12.U)
      c.io.debug_regs(5).expect(9.U)
      c.io.debug_regs(8).expect(1.U)
      assert((dmem.getOrElse(0L, 0.toByte) & 0xff) == 12)
      assert((dmem.getOrElse(1L, 0.toByte) & 0xff) == 1)
    }
  }
}
