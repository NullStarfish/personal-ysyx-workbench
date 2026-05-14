package mycpu.core.frontend

import chisel3._
import chisel3.simulator.EphemeralSimulator._
import mycpu.common._
import mycpu.core.bundles._
import org.scalatest.flatspec.AnyFlatSpec

class DecodeSim extends AnyFlatSpec {
  private val pc = BigInt("a0000000", 16)

  private def iType(imm: Int, rs1: Int, funct3: Int, rd: Int, opcode: Int): BigInt =
    ((BigInt(imm) & 0xfff) << 20) |
      (BigInt(rs1 & 0x1f) << 15) |
      (BigInt(funct3 & 0x7) << 12) |
      (BigInt(rd & 0x1f) << 7) |
      BigInt(opcode & 0x7f)

  private def rType(funct7: Int, rs2: Int, rs1: Int, funct3: Int, rd: Int, opcode: Int): BigInt =
    (BigInt(funct7 & 0x7f) << 25) |
      (BigInt(rs2 & 0x1f) << 20) |
      (BigInt(rs1 & 0x1f) << 15) |
      (BigInt(funct3 & 0x7) << 12) |
      (BigInt(rd & 0x1f) << 7) |
      BigInt(opcode & 0x7f)

  private def sType(imm: Int, rs2: Int, rs1: Int, funct3: Int, opcode: Int): BigInt = {
    val value = imm & 0xfff
    (BigInt((value >> 5) & 0x7f) << 25) |
      (BigInt(rs2 & 0x1f) << 20) |
      (BigInt(rs1 & 0x1f) << 15) |
      (BigInt(funct3 & 0x7) << 12) |
      (BigInt(value & 0x1f) << 7) |
      BigInt(opcode & 0x7f)
  }

  private def bType(imm: Int, rs2: Int, rs1: Int, funct3: Int): BigInt = {
    val value = imm & 0x1fff
    (BigInt((value >> 12) & 0x1) << 31) |
      (BigInt((value >> 5) & 0x3f) << 25) |
      (BigInt(rs2 & 0x1f) << 20) |
      (BigInt(rs1 & 0x1f) << 15) |
      (BigInt(funct3 & 0x7) << 12) |
      (BigInt((value >> 1) & 0xf) << 8) |
      (BigInt((value >> 11) & 0x1) << 7) |
      BigInt("1100011", 2)
  }

  private def uType(imm: Int, rd: Int, opcode: Int): BigInt =
    (BigInt(imm) & BigInt("fffff000", 16)) |
      (BigInt(rd & 0x1f) << 7) |
      BigInt(opcode & 0x7f)

  private def jType(imm: Int, rd: Int): BigInt = {
    val value = imm & 0x1fffff
    (BigInt((value >> 20) & 0x1) << 31) |
      (BigInt((value >> 1) & 0x3ff) << 21) |
      (BigInt((value >> 11) & 0x1) << 20) |
      (BigInt((value >> 12) & 0xff) << 12) |
      (BigInt(rd & 0x1f) << 7) |
      BigInt("1101111", 2)
  }

  private def init(c: Decode): Unit = {
    c.io.in.valid.poke(false.B)
    c.io.in.bits.pc.poke(0.U)
    c.io.in.bits.inst.poke(0.U)
    c.io.in.bits.isException.poke(false.B)
    c.io.out.ready.poke(false.B)
    c.io.regWrite.regWrite.wen.poke(false.B)
    c.io.regWrite.regWrite.rd.poke(0.U)
    c.io.regWrite.regWrite.wdata.poke(0.U)
    for (forward <- c.io.forwards) {
      forward.valid.poke(false.B)
      forward.addr.poke(0.U)
      forward.data.poke(0.U)
    }
  }

  private def resetDut(c: Decode): Unit = {
    init(c)
    c.reset.poke(true.B)
    c.clock.step()
    c.reset.poke(false.B)
  }

  private def driveInst(c: Decode, inst: BigInt, at: BigInt = pc): Unit = {
    c.io.in.valid.poke(true.B)
    c.io.in.bits.pc.poke(at.U)
    c.io.in.bits.inst.poke(inst.U)
    c.io.in.bits.isException.poke(false.B)
    c.io.out.ready.poke(true.B)
  }

  "Decode" should "pass Decoupled valid and ready through" in {
    simulate(new Decode) { c =>
      resetDut(c)

      c.io.in.valid.poke(false.B)
      c.io.out.ready.poke(false.B)
      c.io.out.valid.expect(false.B)
      c.io.in.ready.expect(false.B)

      c.io.in.valid.poke(true.B)
      c.io.out.ready.poke(false.B)
      c.io.out.valid.expect(true.B)
      c.io.in.ready.expect(false.B)

      c.io.out.ready.poke(true.B)
      c.io.in.ready.expect(true.B)
    }
  }

  it should "decode ADDI as rs1 plus sign-extended immediate" in {
    simulate(new Decode) { c =>
      resetDut(c)

      c.io.regWrite.regWrite.wen.poke(true.B)
      c.io.regWrite.regWrite.rd.poke(1.U)
      c.io.regWrite.regWrite.wdata.poke("h12345678".U)
      driveInst(c, iType(-4, rs1 = 1, funct3 = 0, rd = 2, opcode = BigInt("0010011", 2).toInt))

      c.io.out.valid.expect(true.B)
      c.io.out.bits.execData.pc.expect(pc.U)
      c.io.out.bits.rs1.valid.expect(true.B)
      c.io.out.bits.rs1.bits.addr.expect(1.U)
      c.io.out.bits.rs1.bits.rdata.expect("h12345678".U)
      c.io.out.bits.rs2.valid.expect(false.B)
      c.io.out.bits.execData.imm.expect("hfffffffc".U)
      c.io.out.bits.execCtrl.aluOp.expect(ALUOp.ADD)
      c.io.out.bits.execCtrl.aluSrcA.expect(ALUSrcA.Rs1)
      c.io.out.bits.execCtrl.aluSrcB.expect(ALUSrcB.Imm)
      c.io.out.bits.execCtrl.wbSel.expect(WBSel.Alu)
      c.io.out.bits.wbCtrl.wen.expect(true.B)
      c.io.out.bits.wbCtrl.rd.expect(2.U)
      c.io.out.bits.memCtrl.en.expect(false.B)
      c.io.out.bits.retireTrace.get.instType.expect(InstType.arith)
    }
  }

  it should "prefer the newest forwarded source register value" in {
    simulate(new Decode) { c =>
      resetDut(c)

      c.io.regWrite.regWrite.wen.poke(true.B)
      c.io.regWrite.regWrite.rd.poke(1.U)
      c.io.regWrite.regWrite.wdata.poke("h11111111".U)
      c.io.forwards(0).valid.poke(true.B)
      c.io.forwards(0).addr.poke(1.U)
      c.io.forwards(0).data.poke("h22222222".U)
      c.io.forwards(1).valid.poke(true.B)
      c.io.forwards(1).addr.poke(1.U)
      c.io.forwards(1).data.poke("h33333333".U)

      driveInst(c, iType(4, rs1 = 1, funct3 = 0, rd = 2, opcode = BigInt("0010011", 2).toInt))

      c.io.out.bits.rs1.valid.expect(true.B)
      c.io.out.bits.rs1.bits.addr.expect(1.U)
      c.io.out.bits.rs1.bits.rdata.expect("h22222222".U)
    }
  }

  it should "decode SW with both source registers and store metadata" in {
    simulate(new Decode) { c =>
      resetDut(c)

      driveInst(c, sType(12, rs2 = 6, rs1 = 5, funct3 = 2, opcode = BigInt("0100011", 2).toInt))

      c.io.out.bits.rs1.valid.expect(true.B)
      c.io.out.bits.rs1.bits.addr.expect(5.U)
      c.io.out.bits.rs2.valid.expect(true.B)
      c.io.out.bits.rs2.bits.addr.expect(6.U)
      c.io.out.bits.execData.imm.expect(12.U)
      c.io.out.bits.execCtrl.aluOp.expect(ALUOp.ADD)
      c.io.out.bits.execCtrl.aluSrcA.expect(ALUSrcA.Rs1)
      c.io.out.bits.execCtrl.aluSrcB.expect(ALUSrcB.Imm)
      c.io.out.bits.wbCtrl.wen.expect(false.B)
      c.io.out.bits.memCtrl.en.expect(true.B)
      c.io.out.bits.memCtrl.write.expect(true.B)
      c.io.out.bits.memCtrl.unsigned.expect(false.B)
      c.io.out.bits.memCtrl.subop.expect(SizeSubop.Word)
      c.io.out.bits.retireTrace.get.instType.expect(InstType.mem)
    }
  }

  it should "decode BEQ as a branch with register operands and B immediate" in {
    simulate(new Decode) { c =>
      resetDut(c)

      driveInst(c, bType(16, rs2 = 4, rs1 = 3, funct3 = 0))

      c.io.out.bits.rs1.valid.expect(true.B)
      c.io.out.bits.rs1.bits.addr.expect(3.U)
      c.io.out.bits.rs2.valid.expect(true.B)
      c.io.out.bits.rs2.bits.addr.expect(4.U)
      c.io.out.bits.execData.imm.expect(16.U)
      c.io.out.bits.execCtrl.branchType.expect(BranchType.Eq)
      c.io.out.bits.execCtrl.isJump.expect(false.B)
      c.io.out.bits.wbCtrl.wen.expect(false.B)
      c.io.out.bits.memCtrl.en.expect(false.B)
      c.io.out.bits.retireTrace.get.instType.expect(InstType.redirect)
    }
  }

  it should "decode JAL as PC plus offset and write pc plus 4" in {
    simulate(new Decode) { c =>
      resetDut(c)

      driveInst(c, jType(32, rd = 1))

      c.io.out.bits.rs1.valid.expect(false.B)
      c.io.out.bits.rs2.valid.expect(false.B)
      c.io.out.bits.execData.imm.expect(32.U)
      c.io.out.bits.execCtrl.aluOp.expect(ALUOp.ADD)
      c.io.out.bits.execCtrl.aluSrcA.expect(ALUSrcA.Pc)
      c.io.out.bits.execCtrl.aluSrcB.expect(ALUSrcB.Imm)
      c.io.out.bits.execCtrl.wbSel.expect(WBSel.PcPlus4)
      c.io.out.bits.execCtrl.isJump.expect(true.B)
      c.io.out.bits.execCtrl.isJalr.expect(false.B)
      c.io.out.bits.wbCtrl.wen.expect(true.B)
      c.io.out.bits.wbCtrl.rd.expect(1.U)
      c.io.out.bits.retireTrace.get.instType.expect(InstType.redirect)
    }
  }

  it should "decode CSRRSI using zimm as rs1 data and CSR writeback" in {
    simulate(new Decode) { c =>
      resetDut(c)

      val inst = iType(0x305, rs1 = 7, funct3 = 6, rd = 10, opcode = BigInt("1110011", 2).toInt)
      driveInst(c, inst)

      c.io.out.bits.rs1.valid.expect(false.B)
      c.io.out.bits.rs2.valid.expect(false.B)
      c.io.out.bits.rs1.bits.rdata.expect(7.U)
      c.io.out.bits.execCtrl.wbSel.expect(WBSel.Csr)
      c.io.out.bits.wbCtrl.wen.expect(true.B)
      c.io.out.bits.wbCtrl.rd.expect(10.U)
      c.io.out.bits.execCtrl.sys.csrOp.expect(CSROp.S)
      c.io.out.bits.execCtrl.sys.csrAddr.expect("h305".U)
      c.io.out.bits.execCtrl.sys.ecall.expect(false.B)
      c.io.out.bits.execCtrl.sys.mret.expect(false.B)
      c.io.out.bits.execCtrl.sys.ebreak.expect(false.B)
      c.io.out.bits.retireTrace.get.instType.expect(InstType.sys)
    }
  }
}
