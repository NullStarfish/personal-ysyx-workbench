package mycpu.core

import chisel3._
import chisel3.simulator.EphemeralSimulator._
import mycpu.common._
import mycpu.pipeline.Rv32eEncoders._
import org.scalatest.flatspec.AnyFlatSpec

import scala.collection.mutable

class CoreProgramSpec extends AnyFlatSpec with CoreProgramSupport {
  private def runReadOnlyProgram(
      program: Seq[BigInt],
      cycles: Int = 80,
      extraWords: Seq[(BigInt, BigInt)] = Seq.empty,
  )(check: Core => Unit): Unit = {
    simulate(new Core) { c =>
      val memory = (program.zipWithIndex.map { case (inst, idx) =>
        BigInt(START_ADDR + idx * 4L) -> inst
      } ++ extraWords).toMap

      c.reset.poke(true.B)
      initBus(c)
      c.clock.step()
      c.reset.poke(false.B)

      var pending: List[ReadTxn] = Nil
      var i = 0
      while (i < cycles) {
        pending = serviceReadBus(c, memory, pending)
        c.clock.step()
        i += 1
      }

      check(c)
    }
  }

  private def runMutableProgram(
      program: Seq[BigInt],
      mutableWords: Seq[(BigInt, BigInt)],
      cycles: Int = 100,
  )(check: (Core, mutable.Map[BigInt, BigInt]) => Unit): Unit = {
    simulate(new Core) { c =>
      val memory = mutable.Map[BigInt, BigInt](
        program.zipWithIndex.map { case (inst, idx) =>
          BigInt(START_ADDR + idx * 4L) -> inst
        } ++ mutableWords: _*
      )

      c.reset.poke(true.B)
      initBus(c)
      c.clock.step()
      c.reset.poke(false.B)

      var pendingRead: List[ReadTxn] = Nil
      var pendingWriteResp: Option[WriteResp] = None
      var i = 0
      while (i < cycles) {
        val next = serviceBus(c, memory, pendingRead, pendingWriteResp)
        pendingRead = next._1
        pendingWriteResp = next._2
        c.clock.step()
        i += 1
      }

      check(c, memory)
    }
  }

  "Core" should "flush wrong-path instructions after a taken branch" in {
    simulate(new Core) { c =>
      val memory = Map[BigInt, BigInt](
        BigInt(START_ADDR) -> BigInt("00100093", 16),      // addi x1, x0, 1
        BigInt(START_ADDR + 4) -> BigInt("00100113", 16),  // addi x2, x0, 1
        BigInt(START_ADDR + 8) -> BigInt("00208463", 16),  // beq x1, x2, 8
        BigInt(START_ADDR + 12) -> BigInt("00100193", 16), // addi x3, x0, 1 (wrong path)
        BigInt(START_ADDR + 16) -> BigInt("00200213", 16), // addi x4, x0, 2 (target)
        BigInt(START_ADDR + 20) -> BigInt("00100073", 16)  // ebreak
      )

      c.reset.poke(true.B)
      initBus(c)
      c.clock.step()
      c.reset.poke(false.B)

      var pending: List[ReadTxn] = Nil
      var cycles = 0
      while (cycles < 50) {
        pending = serviceReadBus(c, memory, pending)
        c.clock.step()
        cycles += 1
      }

      c.io.debug_regs(1).expect(1.U)
      c.io.debug_regs(2).expect(1.U)
      c.io.debug_regs(3).expect(0.U)
      c.io.debug_regs(4).expect(2.U)
    }
  }

  it should "handle forwarding, load-use stall, and redirect in one short program" in {
    simulate(new Core) { c =>
      val memory = Map[BigInt, BigInt](
        BigInt(START_ADDR) -> BigInt("00500093", 16),      // addi x1, x0, 5
        BigInt(START_ADDR + 4) -> BigInt("00108133", 16),  // add x2, x1, x1
        BigInt(START_ADDR + 8) -> BigInt("10002183", 16),  // lw x3, 256(x0)
        BigInt(START_ADDR + 12) -> BigInt("00118213", 16), // addi x4, x3, 1
        BigInt(START_ADDR + 16) -> BigInt("00020463", 16), // beq x4, x0, 8
        BigInt(START_ADDR + 20) -> BigInt("00100293", 16), // addi x5, x0, 1
        BigInt(START_ADDR + 24) -> BigInt("0080006f", 16), // jal x0, 8
        BigInt(START_ADDR + 28) -> BigInt("00100313", 16), // addi x6, x0, 1 (wrong path)
        BigInt(START_ADDR + 32) -> BigInt("00200393", 16), // addi x7, x0, 2 (target)
        BigInt(START_ADDR + 36) -> BigInt("00100073", 16), // ebreak
        BigInt(0x100) -> BigInt(0x20)
      )

      c.reset.poke(true.B)
      initBus(c)
      c.clock.step()
      c.reset.poke(false.B)

      var pending: List[ReadTxn] = Nil
      var cycles = 0
      while (cycles < 80) {
        pending = serviceReadBus(c, memory, pending)
        c.clock.step()
        cycles += 1
      }

      c.io.debug_regs(1).expect(5.U)
      c.io.debug_regs(2).expect(10.U)
      c.io.debug_regs(3).expect("h20".U)
      c.io.debug_regs(4).expect("h21".U)
      c.io.debug_regs(5).expect(1.U)
      c.io.debug_regs(6).expect(0.U)
      c.io.debug_regs(7).expect(2.U)
    }
  }

  it should "handle store then load then use through the full pipeline" in {
    runMutableProgram(
      program = Seq(
        BigInt("05500093", 16),      // addi x1, x0, 0x55
        BigInt("10102023", 16),      // sw x1, 256(x0)
        BigInt("10002103", 16),      // lw x2, 256(x0)
        BigInt("00110193", 16),      // addi x3, x2, 1
        BigInt("00100073", 16),      // ebreak
      ),
      mutableWords = Seq(BigInt(0x100) -> BigInt(0)),
    ) { (c, memory) =>
      c.io.debug_regs(1).expect("h55".U)
      c.io.debug_regs(2).expect("h55".U)
      c.io.debug_regs(3).expect("h56".U)
      assert(memory(BigInt(0x100)) == BigInt(0x55))
    }
  }

  it should "execute upper-immediate instructions" in {
    runReadOnlyProgram(
      program = Seq(
        encodeLui(rd = 2, imm20 = 0x12345),
        encodeAuipc(rd = 3, imm20 = 0x00001),
        nop,
      ),
    ) { c =>
      c.io.debug_regs(2).expect("h12345000".U)
      c.io.debug_regs(3).expect((START_ADDR + 0x1000L + 4L).U)
    }
  }

  it should "execute jal and jalr" in {
    runReadOnlyProgram(
      program = Seq(
        encodeJal(rd = 1, imm = 8),
        encodeOpImm(funct3 = 0, rd = 2, rs1 = 0, imm = 1),
        encodeAuipc(rd = 4, imm20 = 0),
        encodeJalr(rd = 5, rs1 = 4, imm = 12),
        encodeOpImm(funct3 = 0, rd = 6, rs1 = 0, imm = 9),
        encodeOpImm(funct3 = 0, rd = 7, rs1 = 0, imm = 3),
        nop,
      ),
    ) { c =>
      c.io.debug_regs(1).expect((START_ADDR + 4).U)
      c.io.debug_regs(2).expect(0.U)
      c.io.debug_regs(4).expect((START_ADDR + 8).U)
      c.io.debug_regs(5).expect((START_ADDR + 16).U)
      c.io.debug_regs(6).expect(0.U)
      c.io.debug_regs(7).expect(3.U)
    }
  }

  it should "execute ALU immediate and register-register programs" in {
    runReadOnlyProgram(
      program = Seq(
        encodeOpImm(0, 1, 0, 7),
        encodeOpImm(2, 2, 1, 8),
        encodeOpImm(3, 3, 1, 8),
        encodeOpImm(4, 4, 1, 3),
        encodeOpImm(6, 5, 1, 8),
        encodeOpImm(7, 6, 1, 3),
        encodeShiftImm(1, 0x00, 7, 1, 2),
        encodeShiftImm(5, 0x00, 8, 7, 1),
        encodeShiftImm(5, 0x20, 9, 7, 1),
        encodeOpImm(0, 10, 0, 12),
        encodeOpImm(0, 11, 0, 5),
        encodeOp(0, 0x00, 12, 10, 11),
        encodeOp(0, 0x20, 13, 10, 11),
        encodeOp(6, 0x00, 14, 10, 11),
        encodeOp(7, 0x00, 15, 10, 11),
        nop,
      ),
      cycles = 100,
    ) { c =>
      c.io.debug_regs(1).expect(7.U)
      c.io.debug_regs(2).expect(1.U)
      c.io.debug_regs(3).expect(1.U)
      c.io.debug_regs(4).expect(4.U)
      c.io.debug_regs(5).expect(15.U)
      c.io.debug_regs(6).expect(3.U)
      c.io.debug_regs(7).expect(28.U)
      c.io.debug_regs(8).expect(14.U)
      c.io.debug_regs(9).expect(14.U)
      c.io.debug_regs(10).expect(12.U)
      c.io.debug_regs(11).expect(5.U)
      c.io.debug_regs(12).expect(17.U)
      c.io.debug_regs(13).expect(7.U)
      c.io.debug_regs(14).expect(13.U)
      c.io.debug_regs(15).expect(4.U)
    }
  }

  it should "execute load and store programs" in {
    runMutableProgram(
      program = Seq(
        encodeLoad(funct3 = 0, rd = 1, rs1 = 0, imm = 0),
        encodeLoad(funct3 = 4, rd = 2, rs1 = 0, imm = 0),
        encodeLoad(funct3 = 1, rd = 3, rs1 = 0, imm = 0),
        encodeLoad(funct3 = 5, rd = 4, rs1 = 0, imm = 0),
        encodeLoad(funct3 = 2, rd = 5, rs1 = 0, imm = 4),
        encodeOpImm(0, 6, 0, 0x7f),
        encodeStore(funct3 = 0, rs2 = 6, rs1 = 0, imm = 8),
        encodeOpImm(0, 7, 0, 0x123),
        encodeStore(funct3 = 1, rs2 = 7, rs1 = 0, imm = 12),
        encodeLui(rd = 8, imm20 = 0x11223),
        encodeOpImm(0, 8, 8, 0x344),
        encodeStore(funct3 = 2, rs2 = 8, rs1 = 0, imm = 16),
        nop,
      ),
      mutableWords = Seq(
        BigInt(0) -> BigInt("000080ff", 16),
        BigInt(4) -> BigInt("11223344", 16),
        BigInt(8) -> BigInt(0),
        BigInt(12) -> BigInt(0),
        BigInt(16) -> BigInt(0),
      ),
      cycles = 140,
    ) { (c, memory) =>
      c.io.debug_regs(1).expect("hffffffff".U)
      c.io.debug_regs(2).expect("h000000ff".U)
      c.io.debug_regs(3).expect("hffff80ff".U)
      c.io.debug_regs(4).expect("h000080ff".U)
      c.io.debug_regs(5).expect("h11223344".U)
      assert(memory(BigInt(8)) == BigInt("0000007f", 16))
      assert(memory(BigInt(12)) == BigInt("00000123", 16))
      assert(memory(BigInt(16)) == BigInt("11223344", 16))
    }
  }
}
