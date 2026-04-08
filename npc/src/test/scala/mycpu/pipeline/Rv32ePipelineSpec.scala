package mycpu.pipeline

import chisel3._
import chisel3.simulator.EphemeralSimulator._
import mycpu.common._
import org.scalatest.flatspec.AnyFlatSpec

class Rv32ePipelineSpec extends AnyFlatSpec {
  import Rv32eEncoders._

  private def runProgram(
      program: Seq[BigInt],
      mutableWords: Seq[(BigInt, BigInt)] = Seq(BigInt(0) -> BigInt(0)),
      targetCommits: Int,
  )(check: PipelineProgramHarness => Unit): Unit = {
    simulate(new PipelineProgramHarness(program, mutableWords, targetCommits)) { c =>
      c.reset.poke(true.B)
      c.clock.step()
      c.reset.poke(false.B)

      var cycles = 0
      while (c.io.done.peek().litValue == 0 && cycles < 200) {
        c.clock.step()
        cycles += 1
      }

      c.io.done.expect(true.B)
      check(c)
    }
  }

  "RV32E pipeline" should "execute upper-immediate instructions" in {
    runProgram(
      program = Seq(
        // 0x3000_0000: lui   x2, 0x12345
        encodeLui(rd = 2, imm20 = 0x12345),
        // 0x3000_0004: auipc x3, 0x1
        encodeAuipc(rd = 3, imm20 = 0x00001),
        // 0x3000_0008: nop
        nop,
      ),
      targetCommits = 2,
    ) { c =>
      c.io.regs(2).expect("h12345000".U)
      c.io.regs(3).expect((START_ADDR + 0x1000L + 4L).U)
    }
  }

  it should "execute jal and jalr" in {
    runProgram(
      program = Seq(
        // 0x3000_0000: jal   x1, +8
        encodeJal(rd = 1, imm = 8),
        // 0x3000_0004: addi  x2, x0, 1      ; skipped by jal
        encodeOpImm(funct3 = 0, rd = 2, rs1 = 0, imm = 1),
        // 0x3000_0008: auipc x4, 0
        encodeAuipc(rd = 4, imm20 = 0),
        // 0x3000_000c: jalr  x5, x4, 12
        encodeJalr(rd = 5, rs1 = 4, imm = 12),
        // 0x3000_0010: addi  x6, x0, 9      ; skipped by jalr
        encodeOpImm(funct3 = 0, rd = 6, rs1 = 0, imm = 9),
        // 0x3000_0014: addi  x7, x0, 3
        encodeOpImm(funct3 = 0, rd = 7, rs1 = 0, imm = 3),
        // 0x3000_0018: nop
        nop,
      ),
      targetCommits = 4,
    ) { c =>
      c.io.regs(1).expect((START_ADDR + 4).U)
      c.io.regs(2).expect(0.U)
      c.io.regs(4).expect((START_ADDR + 8).U)
      c.io.regs(5).expect((START_ADDR + 16).U)
      c.io.regs(6).expect(0.U)
      c.io.regs(7).expect(3.U)
    }
  }

  it should "execute taken and not-taken branches" in {
    runProgram(
      program = Seq(
        // 0x3000_0000: addi  x1, x0, 5
        encodeOpImm(0, 1, 0, 5),
        // 0x3000_0004: addi  x2, x0, 5
        encodeOpImm(0, 2, 0, 5),
        // 0x3000_0008: beq   x1, x2, +8     ; taken, skips next addi
        encodeBranch(funct3 = 0, rs1 = 1, rs2 = 2, imm = 8),
        // 0x3000_000c: addi  x3, x0, 1
        encodeOpImm(0, 3, 0, 1),
        // 0x3000_0010: addi  x4, x0, 2
        encodeOpImm(0, 4, 0, 2),
        // 0x3000_0014: bne   x1, x2, +8     ; not taken
        encodeBranch(funct3 = 1, rs1 = 1, rs2 = 2, imm = 8),
        // 0x3000_0018: addi  x5, x0, 3
        encodeOpImm(0, 5, 0, 3),
        // 0x3000_001c: blt   x0, x1, +8     ; taken, skips next addi
        encodeBranch(funct3 = 4, rs1 = 0, rs2 = 1, imm = 8),
        // 0x3000_0020: addi  x6, x0, 4
        encodeOpImm(0, 6, 0, 4),
        // 0x3000_0024: bltu  x0, x1, +8     ; taken, skips next addi
        encodeBranch(funct3 = 6, rs1 = 0, rs2 = 1, imm = 8),
        // 0x3000_0028: addi  x7, x0, 5
        encodeOpImm(0, 7, 0, 5),
        // 0x3000_002c: bge   x1, x2, +8     ; taken, skips next addi
        encodeBranch(funct3 = 5, rs1 = 1, rs2 = 2, imm = 8),
        // 0x3000_0030: addi  x8, x0, 6
        encodeOpImm(0, 8, 0, 6),
        // 0x3000_0034: bgeu  x1, x2, +8     ; taken, skips next addi
        encodeBranch(funct3 = 7, rs1 = 1, rs2 = 2, imm = 8),
        // 0x3000_0038: addi  x9, x0, 7
        encodeOpImm(0, 9, 0, 7),
        // 0x3000_003c: nop
        nop,
      ),
      targetCommits = 10,
    ) { c =>
      c.io.regs(3).expect(0.U)
      c.io.regs(4).expect(2.U)
      c.io.regs(5).expect(3.U)
      c.io.regs(6).expect(0.U)
      c.io.regs(7).expect(0.U)
      c.io.regs(8).expect(0.U)
      c.io.regs(9).expect(0.U)
    }
  }

  it should "execute ALU immediate instructions" in {
    runProgram(
      program = Seq(
        // 0x3000_0000: addi  x1, x0, 7
        encodeOpImm(0, 1, 0, 7),
        // 0x3000_0004: slti  x2, x1, 8
        encodeOpImm(2, 2, 1, 8),
        // 0x3000_0008: sltiu x3, x1, 8
        encodeOpImm(3, 3, 1, 8),
        // 0x3000_000c: xori  x4, x1, 3
        encodeOpImm(4, 4, 1, 3),
        // 0x3000_0010: ori   x5, x1, 8
        encodeOpImm(6, 5, 1, 8),
        // 0x3000_0014: andi  x6, x1, 3
        encodeOpImm(7, 6, 1, 3),
        // 0x3000_0018: slli  x7, x1, 2
        encodeShiftImm(1, 0x00, 7, 1, 2),
        // 0x3000_001c: srli  x8, x7, 1
        encodeShiftImm(5, 0x00, 8, 7, 1),
        // 0x3000_0020: srai  x9, x7, 1
        encodeShiftImm(5, 0x20, 9, 7, 1),
        // 0x3000_0024: nop
        nop,
      ),
      targetCommits = 9,
    ) { c =>
      c.io.regs(1).expect(7.U)
      c.io.regs(2).expect(1.U)
      c.io.regs(3).expect(1.U)
      c.io.regs(4).expect(4.U)
      c.io.regs(5).expect(15.U)
      c.io.regs(6).expect(3.U)
      c.io.regs(7).expect(28.U)
      c.io.regs(8).expect(14.U)
      c.io.regs(9).expect(14.U)
    }
  }

  it should "execute ALU register-register instructions" in {
    runProgram(
      program = Seq(
        // 0x3000_0000: addi  x1, x0, 12
        encodeOpImm(0, 1, 0, 12),
        // 0x3000_0004: addi  x2, x0, 5
        encodeOpImm(0, 2, 0, 5),
        // 0x3000_0008: add   x3, x1, x2
        encodeOp(0, 0x00, 3, 1, 2),
        // 0x3000_000c: sub   x4, x1, x2
        encodeOp(0, 0x20, 4, 1, 2),
        // 0x3000_0010: sll   x5, x2, x1
        encodeOp(1, 0x00, 5, 2, 1),
        // 0x3000_0014: slt   x6, x2, x1
        encodeOp(2, 0x00, 6, 2, 1),
        // 0x3000_0018: sltu  x7, x2, x1
        encodeOp(3, 0x00, 7, 2, 1),
        // 0x3000_001c: xor   x8, x1, x2
        encodeOp(4, 0x00, 8, 1, 2),
        // 0x3000_0020: srl   x9, x1, x2
        encodeOp(5, 0x00, 9, 1, 2),
        // 0x3000_0024: sra   x10, x1, x2
        encodeOp(5, 0x20, 10, 1, 2),
        // 0x3000_0028: or    x11, x1, x2
        encodeOp(6, 0x00, 11, 1, 2),
        // 0x3000_002c: and   x12, x1, x2
        encodeOp(7, 0x00, 12, 1, 2),
        // 0x3000_0030: nop
        nop,
      ),
      targetCommits = 12,
    ) { c =>
      c.io.regs(3).expect(17.U)
      c.io.regs(4).expect(7.U)
      c.io.regs(5).expect((BigInt(5) << 12).U)
      c.io.regs(6).expect(1.U)
      c.io.regs(7).expect(1.U)
      c.io.regs(8).expect(9.U)
      c.io.regs(9).expect(0.U)
      c.io.regs(10).expect(0.U)
      c.io.regs(11).expect(13.U)
      c.io.regs(12).expect(4.U)
    }
  }

  it should "execute load instructions" in {
    runProgram(
      program = Seq(
        // 0x3000_0000: lb    x1, 0(x0)
        encodeLoad(funct3 = 0, rd = 1, rs1 = 0, imm = 0),
        // 0x3000_0004: lbu   x2, 0(x0)
        encodeLoad(funct3 = 4, rd = 2, rs1 = 0, imm = 0),
        // 0x3000_0008: lh    x3, 0(x0)
        encodeLoad(funct3 = 1, rd = 3, rs1 = 0, imm = 0),
        // 0x3000_000c: lhu   x4, 0(x0)
        encodeLoad(funct3 = 5, rd = 4, rs1 = 0, imm = 0),
        // 0x3000_0010: lw    x5, 4(x0)
        encodeLoad(funct3 = 2, rd = 5, rs1 = 0, imm = 4),
        // 0x3000_0014: nop
        nop,
      ),
      mutableWords = Seq(
        BigInt(0) -> BigInt("000080ff", 16),
        BigInt(4) -> BigInt("11223344", 16),
      ),
      targetCommits = 5,
    ) { c =>
      c.io.regs(1).expect("hffffffff".U)
      c.io.regs(2).expect("h000000ff".U)
      c.io.regs(3).expect("hffff80ff".U)
      c.io.regs(4).expect("h000080ff".U)
      c.io.regs(5).expect("h11223344".U)
    }
  }

  it should "execute store instructions" in {
    runProgram(
      program = Seq(
        // 0x3000_0000: addi  x1, x0, 0x7f
        encodeOpImm(0, 1, 0, 0x7f),
        // 0x3000_0004: sb    x1, 0(x0)
        encodeStore(funct3 = 0, rs2 = 1, rs1 = 0, imm = 0),
        // 0x3000_0008: addi  x2, x0, 0x123
        encodeOpImm(0, 2, 0, 0x123),
        // 0x3000_000c: sh    x2, 4(x0)
        encodeStore(funct3 = 1, rs2 = 2, rs1 = 0, imm = 4),
        // 0x3000_0010: lui   x3, 0x11223
        encodeLui(rd = 3, imm20 = 0x11223),
        // 0x3000_0014: addi  x3, x3, 0x344
        encodeOpImm(0, 3, 3, 0x344),
        // 0x3000_0018: sw    x3, 8(x0)
        encodeStore(funct3 = 2, rs2 = 3, rs1 = 0, imm = 8),
        // 0x3000_001c: nop
        nop,
      ),
      mutableWords = Seq(
        BigInt(0) -> BigInt(0),
        BigInt(4) -> BigInt(0),
        BigInt(8) -> BigInt(0),
      ),
      targetCommits = 7,
    ) { c =>
      c.io.memWords(0).expect("h0000007f".U)
      c.io.memWords(1).expect("h00000123".U)
      c.io.memWords(2).expect("h11223344".U)
    }
  }

  it should "run the original mixed arithmetic and memory chain" in {
    runProgram(
      program = Seq(
        // 0x3000_0000: lw    x1, 0(x0)
        encodeLoad(funct3 = 2, rd = 1, rs1 = 0, imm = 0),
        // 0x3000_0004: addi  x2, x1, 1
        encodeOpImm(0, 2, 1, 1),
        // 0x3000_0008: sw    x2, 4(x0)
        encodeStore(funct3 = 2, rs2 = 2, rs1 = 0, imm = 4),
        // 0x3000_000c: lw    x3, 4(x0)
        encodeLoad(funct3 = 2, rd = 3, rs1 = 0, imm = 4),
        // 0x3000_0010: addi  x4, x3, 2
        encodeOpImm(0, 4, 3, 2),
        // 0x3000_0014: nop
        nop,
      ),
      mutableWords = Seq(
        BigInt(0) -> BigInt("11223344", 16),
        BigInt(4) -> BigInt(0),
      ),
      targetCommits = 5,
    ) { c =>
      c.io.regs(1).expect("h11223344".U)
      c.io.regs(2).expect("h11223345".U)
      c.io.regs(3).expect("h11223345".U)
      c.io.regs(4).expect("h11223347".U)
      c.io.memWords(0).expect("h11223344".U)
      c.io.memWords(1).expect("h11223345".U)
    }
  }
}
