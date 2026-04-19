package labcpu.core

import chisel3._
import chisel3.simulator.EphemeralSimulator._
import org.scalatest.flatspec.AnyFlatSpec

import scala.collection.mutable

import CourseCoreTestUtils._
import CourseCoreTestUtils.RvEnc._

class CourseCoreRv32iSpec extends AnyFlatSpec {
  "CourseCore" should "cover upper, immediate, and jump instructions in a single ISA program" in {
    simulate(new CourseCore(startAddr = 0x0L)) { c =>
      val imem = programOf(
        Seq(
          lui(1, 0x12345),            // x1 = 0x12345000
          auipc(2, 0x1),             // x2 = 0x00001004
          addi(3, 0, -1),            // x3 = -1
          slti(4, 3, 0),             // x4 = 1
          sltiu(5, 0, 1),            // x5 = 1
          xori(6, 3, 0xff),          // x6 = 0xffffff00
          ori(7, 0, 0x55),           // x7 = 0x55
          andi(8, 7, 0x0f),          // x8 = 0x5
          slli(9, 7, 4),             // x9 = 0x550
          srli(10, 9, 4),            // x10 = 0x55
          srai(11, 3, 4),            // x11 = 0xffffffff
          jal(12, 12),               // x12 = 0x30, jump to 0x38
          addi(13, 0, 1),            // flushed
          addi(13, 0, 2),            // flushed
          addi(14, 0, 0x44),         // x14 = 0x44
          addi(15, 0, 0x48),         // x15 = 0x48
          jalr(16, 15, 0),           // x16 = 0x44, jump to 0x48
          addi(17, 0, 0x99),         // skipped
          ebreak,
        ),
      )
      val dmem = mutable.Map.empty[Long, Byte]

      runProgram(c, imem, dmem)
      c.io.debug_regs(1).expect("h12345000".U)
      c.io.debug_regs(2).expect("h00001004".U)
      c.io.debug_regs(3).expect("hffffffff".U)
      c.io.debug_regs(4).expect(1.U)
      c.io.debug_regs(5).expect(1.U)
      c.io.debug_regs(6).expect("hffffff00".U)
      c.io.debug_regs(7).expect("h55".U)
      c.io.debug_regs(8).expect("h5".U)
      c.io.debug_regs(9).expect("h550".U)
      c.io.debug_regs(10).expect("h55".U)
      c.io.debug_regs(11).expect("hffffffff".U)
      c.io.debug_regs(12).expect("h30".U)
      c.io.debug_regs(13).expect(0.U)
      c.io.debug_regs(14).expect("h44".U)
      c.io.debug_regs(16).expect("h44".U)
      c.io.debug_regs(17).expect(0.U)
    }
  }

  it should "cover register-register ALU operations and branch decisions" in {
    simulate(new CourseCore(startAddr = 0x0L)) { c =>
      val imem = programOf(
        Seq(
          addi(1, 0, 7),             // x1 = 7
          addi(2, 0, 3),             // x2 = 3
          addi(3, 0, -1),            // x3 = 0xffffffff
          addi(4, 0, 1),             // x4 = 1
          add(5, 1, 2),              // x5 = 10
          sub(6, 1, 2),              // x6 = 4
          sll(7, 1, 2),              // x7 = 56
          slt(8, 2, 1),              // x8 = 1
          sltu(9, 3, 4),             // x9 = 0
          xor(10, 1, 2),             // x10 = 4
          srl(11, 3, 4),             // x11 = 0x7fffffff
          sra(12, 3, 4),             // x12 = 0xffffffff
          or(13, 1, 2),              // x13 = 7
          and(14, 1, 2),             // x14 = 3
          beq(1, 2, 8),              // not taken
          addi(21, 0, 1),            // executes
          beq(1, 1, 8),              // taken
          addi(20, 0, 1),            // skipped
          bne(1, 2, 8),              // taken
          addi(22, 0, 2),            // skipped
          blt(2, 1, 8),              // taken
          addi(23, 0, 3),            // skipped
          bge(1, 2, 8),              // taken
          addi(24, 0, 4),            // skipped
          bltu(4, 3, 8),             // taken
          addi(25, 0, 5),            // skipped
          bgeu(3, 4, 8),             // taken
          addi(26, 0, 6),            // skipped
          ebreak,
        ),
      )
      val dmem = mutable.Map.empty[Long, Byte]

      runProgram(c, imem, dmem)
      c.io.debug_regs(5).expect(10.U)
      c.io.debug_regs(6).expect(4.U)
      c.io.debug_regs(7).expect(56.U)
      c.io.debug_regs(8).expect(1.U)
      c.io.debug_regs(9).expect(0.U)
      c.io.debug_regs(10).expect(4.U)
      c.io.debug_regs(11).expect("h7fffffff".U)
      c.io.debug_regs(12).expect("hffffffff".U)
      c.io.debug_regs(13).expect(7.U)
      c.io.debug_regs(14).expect(3.U)
      c.io.debug_regs(20).expect(0.U)
      c.io.debug_regs(21).expect(1.U)
      c.io.debug_regs(22).expect(0.U)
      c.io.debug_regs(23).expect(0.U)
      c.io.debug_regs(24).expect(0.U)
      c.io.debug_regs(25).expect(0.U)
      c.io.debug_regs(26).expect(0.U)
    }
  }

  it should "cover signed and unsigned load-store instructions" in {
    simulate(new CourseCore(startAddr = 0x0L)) { c =>
      val imem = programOf(
        Seq(
          addi(1, 0, 0x80),          // x1 = 0x80
          sb(1, 0, 0),               // mem[0] = 0x80
          lb(2, 0, 0),               // x2 = 0xffffff80
          lbu(3, 0, 0),              // x3 = 0x80
          lui(4, 0x8),               // x4 = 0x8000
          addi(4, 4, 1),             // x4 = 0x8001
          sh(4, 0, 2),               // mem[2:3] = 0x8001
          lh(5, 0, 2),               // x5 = 0xffff8001
          lhu(6, 0, 2),              // x6 = 0x8001
          lui(7, 0x12345),           // x7 = 0x12345000
          addi(7, 7, 0x078),         // x7 = 0x12345078
          sw(7, 0, 4),               // mem[4:7] = 0x12345078
          lw(8, 0, 4),               // x8 = 0x12345078
          ebreak,
        ),
      )
      val dmem = mutable.Map.empty[Long, Byte]

      runProgram(c, imem, dmem)
      c.io.debug_regs(2).expect("hffffff80".U)
      c.io.debug_regs(3).expect("h00000080".U)
      c.io.debug_regs(5).expect("hffff8001".U)
      c.io.debug_regs(6).expect("h00008001".U)
      c.io.debug_regs(8).expect("h12345078".U)

      assert((dmem.getOrElse(0L, 0.toByte) & 0xff) == 0x80)
      assert((dmem.getOrElse(2L, 0.toByte) & 0xff) == 0x01)
      assert((dmem.getOrElse(3L, 0.toByte) & 0xff) == 0x80)
      assert((dmem.getOrElse(4L, 0.toByte) & 0xff) == 0x78)
      assert((dmem.getOrElse(5L, 0.toByte) & 0xff) == 0x50)
      assert((dmem.getOrElse(6L, 0.toByte) & 0xff) == 0x34)
      assert((dmem.getOrElse(7L, 0.toByte) & 0xff) == 0x12)
    }
  }
}
