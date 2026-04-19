package labcpu.core

import chisel3._
import chisel3.simulator.EphemeralSimulator._
import scala.collection.mutable
import org.scalatest.flatspec.AnyFlatSpec
import CourseCoreTestUtils._

class CourseCoreProgramSpec extends AnyFlatSpec {
  "CourseCore" should "pass an ISA-style arithmetic and upper-immediate program" in {
    simulate(new CourseCore(startAddr = 0x0L)) { c =>
      val imem = Map[Long, Long](
        0x00L -> 0x123450b7L, // lui x1, 0x12345
        0x04L -> 0x01010113L, // addi x2, x2, 16
        0x08L -> 0x0020e1b3L, // or x3, x1, x2
        0x0cL -> 0x40218233L, // sub x4, x3, x2
        0x10L -> 0x002092b3L, // sll x5, x1, x2
        0x14L -> 0x0012d313L, // srli x6, x5, 1
        0x18L -> 0x40135393L, // srai x7, x6, 1
        0x1cL -> 0x00402023L, // sw x4, 0(x0)
        0x20L -> 0x00002403L, // lw x8, 0(x0)
        0x24L -> 0x00100073L, // ebreak
      )
      val dmem = mutable.Map.empty[Long, Byte]

      runProgram(c, imem, dmem)
      c.io.debug_regs(1).expect("h12345000".U)
      c.io.debug_regs(2).expect(16.U)
      c.io.debug_regs(4).expect("h12345000".U)
      c.io.debug_regs(8).expect("h12345000".U)
    }
  }

  it should "pass an ISA-style byte and halfword memory program" in {
    simulate(new CourseCore(startAddr = 0x0L)) { c =>
      val imem = Map[Long, Long](
        0x00L -> 0x0ab00093L, // addi x1, x0, 0xab
        0x04L -> 0x001000a3L, // sb x1, 1(x0)
        0x08L -> 0x00104103L, // lbu x2, 1(x0)
        0x0cL -> 0x23400193L, // addi x3, x0, 0x234
        0x10L -> 0x00301123L, // sh x3, 2(x0)
        0x14L -> 0x00205183L, // lhu x3, 2(x0)
        0x18L -> 0x00100073L, // ebreak
      )
      val dmem = mutable.Map.empty[Long, Byte]

      runProgram(c, imem, dmem)
      c.io.debug_regs(2).expect("hab".U)
      c.io.debug_regs(3).expect("h234".U)
      assert((dmem.getOrElse(1L, 0.toByte) & 0xff) == 0xab)
      assert((dmem.getOrElse(2L, 0.toByte) & 0xff) == 0x34)
      assert((dmem.getOrElse(3L, 0.toByte) & 0xff) == 0x02)
    }
  }
}
