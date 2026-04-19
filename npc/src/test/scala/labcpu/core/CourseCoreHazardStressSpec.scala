package labcpu.core

import chisel3._
import chisel3.simulator.EphemeralSimulator._
import scala.collection.mutable
import org.scalatest.flatspec.AnyFlatSpec
import CourseCoreTestUtils._

class CourseCoreHazardStressSpec extends AnyFlatSpec {
  "CourseCore" should "handle dense RAW forwarding chains" in {
    simulate(new CourseCore(startAddr = 0x0L)) { c =>
      val imem = Map[Long, Long](
        0x00L -> 0x00100093L, // addi x1, x0, 1
        0x04L -> 0x00108113L, // addi x2, x1, 1
        0x08L -> 0x002101b3L, // add x3, x2, x2
        0x0cL -> 0x00318233L, // add x4, x3, x3
        0x10L -> 0x004202b3L, // add x5, x4, x4
        0x14L -> 0x00100073L, // ebreak
      )
      val dmem = mutable.Map.empty[Long, Byte]

      runProgram(c, imem, dmem)
      c.io.debug_regs(5).expect(16.U)
    }
  }

  it should "stall load-use and still forward the loaded result" in {
    simulate(new CourseCore(startAddr = 0x0L)) { c =>
      val imem = Map[Long, Long](
        0x00L -> 0x01100093L, // addi x1, x0, 17
        0x04L -> 0x00102023L, // sw x1, 0(x0)
        0x08L -> 0x00002103L, // lw x2, 0(x0)
        0x0cL -> 0x001101b3L, // add x3, x2, x1
        0x10L -> 0x00100073L, // ebreak
      )
      val dmem = mutable.Map.empty[Long, Byte]

      runProgram(c, imem, dmem)
      c.io.debug_regs(2).expect(17.U)
      c.io.debug_regs(3).expect(34.U)
    }
  }

  it should "flush wrong-path instructions after taken branch and jal" in {
    simulate(new CourseCore(startAddr = 0x0L)) { c =>
      val imem = Map[Long, Long](
        0x00L -> 0x00100093L, // addi x1, x0, 1
        0x04L -> 0x00108463L, // beq x1, x1, 8
        0x08L -> 0x0aa00113L, // addi x2, x0, 0xaa (flushed)
        0x0cL -> 0x00900113L, // addi x2, x0, 9
        0x10L -> 0x008001efL, // jal x3, 8
        0x14L -> 0x05500213L, // addi x4, x0, 0x55 (flushed)
        0x18L -> 0x00700213L, // addi x4, x0, 7
        0x1cL -> 0x00100073L, // ebreak
      )
      val dmem = mutable.Map.empty[Long, Byte]

      runProgram(c, imem, dmem)
      c.io.debug_regs(2).expect(9.U)
      c.io.debug_regs(4).expect(7.U)
      c.io.debug_regs(3).expect(0x14.U)
    }
  }
}
