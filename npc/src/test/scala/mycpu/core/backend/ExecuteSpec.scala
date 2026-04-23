package mycpu.core.backend

import chisel3._
import chisel3.simulator.EphemeralSimulator._
import mycpu.common._
import mycpu.core.bundles._
import org.scalatest.flatspec.AnyFlatSpec

class ExecuteSpec extends AnyFlatSpec {
  private def initInput(c: Execute): Unit = {
    c.io.in.valid.poke(true.B)
    c.io.out.ready.poke(true.B)
    c.io.in.bits.data.pc.poke(START_ADDR.U)
    c.io.in.bits.data.rs1.poke(0.U)
    c.io.in.bits.data.rs2.poke(0.U)
    c.io.in.bits.data.imm.poke(0.U)
    c.io.in.bits.bypass.rs1Addr.poke(0.U)
    c.io.in.bits.bypass.rs2Addr.poke(0.U)
    c.io.in.bits.exec.aluOp.poke(ALUOp.NOP)
    c.io.in.bits.exec.aluSrcA.poke(ALUSrcA.Rs1)
    c.io.in.bits.exec.aluSrcB.poke(ALUSrcB.Rs2)
    c.io.in.bits.exec.wbSel.poke(WBSel.Alu)
    c.io.in.bits.exec.branchType.poke(BranchType.None)
    c.io.in.bits.exec.isJump.poke(false.B)
    c.io.in.bits.exec.isJalr.poke(false.B)
    c.io.in.bits.wb.regWen.poke(false.B)
    c.io.in.bits.wb.rd.poke(0.U)
    c.io.in.bits.mem.valid.poke(false.B)
    c.io.in.bits.mem.write.poke(false.B)
    c.io.in.bits.mem.unsigned.poke(false.B)
    c.io.in.bits.mem.subop.poke(ExecSubop.None)
    c.io.in.bits.sys.csrOp.poke(CSROp.N)
    c.io.in.bits.sys.csrAddr.poke(0.U)
    c.io.in.bits.sys.isEcall.poke(false.B)
    c.io.in.bits.sys.isMret.poke(false.B)
    c.io.in.bits.sys.isEbreak.poke(false.B)
  }

  "Execute" should "compute an ALU add in one cycle" in {
    simulate(new Execute) { c =>
      initInput(c)
      c.io.in.bits.data.rs1.poke(2.U)
      c.io.in.bits.data.rs2.poke(3.U)
      c.io.in.bits.exec.aluOp.poke(ALUOp.ADD)
      c.io.in.bits.wb.regWen.poke(true.B)
      c.io.in.bits.wb.rd.poke(1.U)
      c.clock.step()

      c.io.out.valid.expect(true.B)
      c.io.out.bits.result.expect(5.U)
      c.io.out.bits.wb.rd.expect(1.U)
      c.io.out.bits.redirect.expect(false.B)
    }
  }

  it should "produce redirect for jal" in {
    simulate(new Execute) { c =>
      initInput(c)
      c.io.in.bits.data.pc.poke(START_ADDR.U)
      c.io.in.bits.exec.aluOp.poke(ALUOp.ADD)
      c.io.in.bits.exec.aluSrcA.poke(ALUSrcA.Pc)
      c.io.in.bits.exec.aluSrcB.poke(ALUSrcB.Imm)
      c.io.in.bits.exec.wbSel.poke(WBSel.PcPlus4)
      c.io.in.bits.exec.isJump.poke(true.B)
      c.io.in.bits.wb.regWen.poke(true.B)
      c.io.in.bits.wb.rd.poke(1.U)
      c.io.in.bits.data.imm.poke(8.U)
      c.clock.step()

      c.io.out.valid.expect(true.B)
      c.io.out.bits.result.expect((START_ADDR + 4).U)
      c.io.out.bits.redirect.expect(true.B)
      c.io.out.bits.rhs.expect((START_ADDR + 8).U)
    }
  }

  it should "take branch when compare result matches op semantics" in {
    simulate(new Execute) { c =>
      initInput(c)
      c.io.in.bits.data.pc.poke(START_ADDR.U)
      c.io.in.bits.data.rs1.poke(7.U)
      c.io.in.bits.data.rs2.poke(7.U)
      c.io.in.bits.exec.branchType.poke(BranchType.Eq)
      c.io.in.bits.data.imm.poke(8.U)
      c.clock.step()

      c.io.out.valid.expect(true.B)
      c.io.out.bits.redirect.expect(true.B)
      c.io.out.bits.rhs.expect((START_ADDR + 8).U)
      c.io.out.bits.wb.regWen.expect(false.B)
    }
  }

  it should "mark mem request in EX for store" in {
    simulate(new Execute) { c =>
      initInput(c)
      c.io.in.bits.data.rs1.poke("h30000010".U)
      c.io.in.bits.data.rs2.poke("h000000aa".U)
      c.io.in.bits.data.imm.poke("h00000004".U)
      c.io.in.bits.exec.aluOp.poke(ALUOp.ADD)
      c.io.in.bits.exec.aluSrcA.poke(ALUSrcA.Rs1)
      c.io.in.bits.exec.aluSrcB.poke(ALUSrcB.Imm)
      c.io.in.bits.mem.valid.poke(true.B)
      c.io.in.bits.mem.write.poke(true.B)
      c.io.in.bits.mem.subop.poke(ExecSubop.Byte)
      c.clock.step()

      c.io.out.valid.expect(true.B)
      c.io.out.bits.result.expect("h30000014".U)
      c.io.out.bits.rhs.expect("h000000aa".U)
      c.io.out.bits.mem.valid.expect(true.B)
      c.io.out.bits.mem.write.expect(true.B)
      c.io.out.bits.mem.subop.expect(ExecSubop.Byte)
    }
  }
}
