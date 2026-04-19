package labcpu.core

import chisel3._
import chisel3.simulator.EphemeralSimulator._
import scala.collection.mutable

object CourseCoreTestUtils {
  object RvEnc {
    private def mask(value: Int, bits: Int): Int = value & ((1 << bits) - 1)

    def rType(funct7: Int, rs2: Int, rs1: Int, funct3: Int, rd: Int, opcode: Int): Long =
      ((mask(funct7, 7) << 25) | (mask(rs2, 5) << 20) | (mask(rs1, 5) << 15) |
        (mask(funct3, 3) << 12) | (mask(rd, 5) << 7) | mask(opcode, 7)).toLong & 0xffffffffL

    def iType(imm: Int, rs1: Int, funct3: Int, rd: Int, opcode: Int): Long =
      ((mask(imm, 12) << 20) | (mask(rs1, 5) << 15) | (mask(funct3, 3) << 12) |
        (mask(rd, 5) << 7) | mask(opcode, 7)).toLong & 0xffffffffL

    def sType(imm: Int, rs2: Int, rs1: Int, funct3: Int, opcode: Int): Long = {
      val imm12 = mask(imm, 12)
      (((imm12 >> 5) << 25) | (mask(rs2, 5) << 20) | (mask(rs1, 5) << 15) |
        (mask(funct3, 3) << 12) | ((imm12 & 0x1f) << 7) | mask(opcode, 7)).toLong & 0xffffffffL
    }

    def bType(imm: Int, rs2: Int, rs1: Int, funct3: Int, opcode: Int): Long = {
      val imm13 = mask(imm, 13)
      (((imm13 >> 12) & 0x1) << 31 |
        ((imm13 >> 5) & 0x3f) << 25 |
        (mask(rs2, 5) << 20) |
        (mask(rs1, 5) << 15) |
        (mask(funct3, 3) << 12) |
        ((imm13 >> 1) & 0xf) << 8 |
        ((imm13 >> 11) & 0x1) << 7 |
        mask(opcode, 7)).toLong & 0xffffffffL
    }

    def uType(imm: Int, rd: Int, opcode: Int): Long =
      ((mask(imm, 20) << 12) | (mask(rd, 5) << 7) | mask(opcode, 7)).toLong & 0xffffffffL

    def jType(imm: Int, rd: Int, opcode: Int): Long = {
      val imm21 = mask(imm, 21)
      ((((imm21 >> 20) & 0x1) << 31) |
        (((imm21 >> 1) & 0x3ff) << 21) |
        (((imm21 >> 11) & 0x1) << 20) |
        (((imm21 >> 12) & 0xff) << 12) |
        (mask(rd, 5) << 7) |
        mask(opcode, 7)).toLong & 0xffffffffL
    }

    def programOf(insts: Seq[Long], startAddr: Long = 0L): Map[Long, Long] =
      insts.zipWithIndex.map { case (inst, idx) => (startAddr + idx * 4L) -> inst }.toMap

    def lui(rd: Int, imm20: Int): Long = uType(imm20, rd, 0x37)
    def auipc(rd: Int, imm20: Int): Long = uType(imm20, rd, 0x17)
    def jal(rd: Int, imm: Int): Long = jType(imm, rd, 0x6f)
    def jalr(rd: Int, rs1: Int, imm: Int): Long = iType(imm, rs1, 0x0, rd, 0x67)

    def beq(rs1: Int, rs2: Int, imm: Int): Long = bType(imm, rs2, rs1, 0x0, 0x63)
    def bne(rs1: Int, rs2: Int, imm: Int): Long = bType(imm, rs2, rs1, 0x1, 0x63)
    def blt(rs1: Int, rs2: Int, imm: Int): Long = bType(imm, rs2, rs1, 0x4, 0x63)
    def bge(rs1: Int, rs2: Int, imm: Int): Long = bType(imm, rs2, rs1, 0x5, 0x63)
    def bltu(rs1: Int, rs2: Int, imm: Int): Long = bType(imm, rs2, rs1, 0x6, 0x63)
    def bgeu(rs1: Int, rs2: Int, imm: Int): Long = bType(imm, rs2, rs1, 0x7, 0x63)

    def lb(rd: Int, rs1: Int, imm: Int): Long = iType(imm, rs1, 0x0, rd, 0x03)
    def lh(rd: Int, rs1: Int, imm: Int): Long = iType(imm, rs1, 0x1, rd, 0x03)
    def lw(rd: Int, rs1: Int, imm: Int): Long = iType(imm, rs1, 0x2, rd, 0x03)
    def lbu(rd: Int, rs1: Int, imm: Int): Long = iType(imm, rs1, 0x4, rd, 0x03)
    def lhu(rd: Int, rs1: Int, imm: Int): Long = iType(imm, rs1, 0x5, rd, 0x03)

    def sb(rs2: Int, rs1: Int, imm: Int): Long = sType(imm, rs2, rs1, 0x0, 0x23)
    def sh(rs2: Int, rs1: Int, imm: Int): Long = sType(imm, rs2, rs1, 0x1, 0x23)
    def sw(rs2: Int, rs1: Int, imm: Int): Long = sType(imm, rs2, rs1, 0x2, 0x23)

    def addi(rd: Int, rs1: Int, imm: Int): Long = iType(imm, rs1, 0x0, rd, 0x13)
    def slti(rd: Int, rs1: Int, imm: Int): Long = iType(imm, rs1, 0x2, rd, 0x13)
    def sltiu(rd: Int, rs1: Int, imm: Int): Long = iType(imm, rs1, 0x3, rd, 0x13)
    def xori(rd: Int, rs1: Int, imm: Int): Long = iType(imm, rs1, 0x4, rd, 0x13)
    def ori(rd: Int, rs1: Int, imm: Int): Long = iType(imm, rs1, 0x6, rd, 0x13)
    def andi(rd: Int, rs1: Int, imm: Int): Long = iType(imm, rs1, 0x7, rd, 0x13)
    def slli(rd: Int, rs1: Int, shamt: Int): Long = iType(shamt, rs1, 0x1, rd, 0x13)
    def srli(rd: Int, rs1: Int, shamt: Int): Long = iType(shamt, rs1, 0x5, rd, 0x13)
    def srai(rd: Int, rs1: Int, shamt: Int): Long = iType((0x20 << 5) | mask(shamt, 5), rs1, 0x5, rd, 0x13)

    def add(rd: Int, rs1: Int, rs2: Int): Long = rType(0x00, rs2, rs1, 0x0, rd, 0x33)
    def sub(rd: Int, rs1: Int, rs2: Int): Long = rType(0x20, rs2, rs1, 0x0, rd, 0x33)
    def sll(rd: Int, rs1: Int, rs2: Int): Long = rType(0x00, rs2, rs1, 0x1, rd, 0x33)
    def slt(rd: Int, rs1: Int, rs2: Int): Long = rType(0x00, rs2, rs1, 0x2, rd, 0x33)
    def sltu(rd: Int, rs1: Int, rs2: Int): Long = rType(0x00, rs2, rs1, 0x3, rd, 0x33)
    def xor(rd: Int, rs1: Int, rs2: Int): Long = rType(0x00, rs2, rs1, 0x4, rd, 0x33)
    def srl(rd: Int, rs1: Int, rs2: Int): Long = rType(0x00, rs2, rs1, 0x5, rd, 0x33)
    def sra(rd: Int, rs1: Int, rs2: Int): Long = rType(0x20, rs2, rs1, 0x5, rd, 0x33)
    def or(rd: Int, rs1: Int, rs2: Int): Long = rType(0x00, rs2, rs1, 0x6, rd, 0x33)
    def and(rd: Int, rs1: Int, rs2: Int): Long = rType(0x00, rs2, rs1, 0x7, rd, 0x33)

    val ebreak: Long = 0x00100073L
  }

  def tickMemory(c: CourseCore, imem: Map[Long, Long], dmem: mutable.Map[Long, Byte]): Unit = {
    val iaddr = c.io.imem.addr.peek().litValue.toLong
    c.io.imem.rdata.poke(imem.getOrElse(iaddr, 0x00000013L).U)

    val daddr = c.io.dmem.addr.peek().litValue.toLong
    val subop = c.io.dmem.subop.peek().litValue.toInt
    val isUnsigned = c.io.dmem.unsigned.peek().litToBoolean
    val loadData = subop match {
      case 1 =>
        val byteVal = dmem.getOrElse(daddr, 0.toByte) & 0xff
        if (isUnsigned) BigInt(byteVal)
        else BigInt(byteVal.toByte.toInt & 0xffffffffL)
      case 2 =>
        val halfVal =
          (dmem.getOrElse(daddr, 0.toByte) & 0xff) |
            ((dmem.getOrElse(daddr + 1, 0.toByte) & 0xff) << 8)
        if (isUnsigned) BigInt(halfVal)
        else BigInt(halfVal.toShort.toInt & 0xffffffffL)
      case _ =>
        (0 until 4).map(i => BigInt(dmem.getOrElse(daddr + i, 0.toByte) & 0xff) << (i * 8)).reduce(_ | _)
    }
    c.io.dmem.rdata.poke(loadData.U)

    if (c.io.dmem.wen.peek().litToBoolean) {
      val data = c.io.dmem.wdata.peek().litValue.toLong
      subop match {
        case 1 =>
          dmem(daddr) = (data & 0xff).toByte
        case 2 =>
          dmem(daddr) = (data & 0xff).toByte
          dmem(daddr + 1) = ((data >> 8) & 0xff).toByte
        case _ =>
          for (i <- 0 until 4) {
            dmem(daddr + i) = ((data >> (i * 8)) & 0xff).toByte
          }
      }
    }
  }

  def runProgram(
    c: CourseCore,
    imem: Map[Long, Long],
    dmem: mutable.Map[Long, Byte],
    maxCycles: Int = 120,
  ): Int = {
    c.reset.poke(true.B)
    tickMemory(c, imem, dmem)
    c.clock.step()
    c.reset.poke(false.B)

    var cycles = 0
    var sawEbreakRetire = false
    while (!sawEbreakRetire && cycles < maxCycles) {
      tickMemory(c, imem, dmem)
      c.clock.step()
      sawEbreakRetire =
        c.io.retire_valid.peek().litToBoolean &&
          c.io.retire_inst.peek().litValue == 0x00100073L
      cycles += 1
    }
    assert(sawEbreakRetire, s"core should retire ebreak within $maxCycles cycles")
    cycles
  }
}
