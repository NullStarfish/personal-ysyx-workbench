package mycpu.core.backend

import chisel3._
import chisel3.simulator.EphemeralSimulator._
import mycpu.common._
import mycpu.core.bundles._
import org.scalatest.flatspec.AnyFlatSpec

class ExecuteSim extends AnyFlatSpec {
  private val pc = BigInt("a0000000", 16)

  private def init(c: Execute): Unit = {
    c.io.in.valid.poke(false.B)
    c.io.out.ready.poke(false.B)

    c.io.in.bits.rs1.valid.poke(false.B)
    c.io.in.bits.rs1.bits.addr.poke(0.U)
    c.io.in.bits.rs1.bits.rdata.poke(0.U)
    c.io.in.bits.rs2.valid.poke(false.B)
    c.io.in.bits.rs2.bits.addr.poke(0.U)
    c.io.in.bits.rs2.bits.rdata.poke(0.U)
    c.io.in.bits.rd.poke(0.U)

    c.io.in.bits.execCtrl.aluOp.poke(ALUOp.NOP)
    c.io.in.bits.execCtrl.aluSrcA.poke(ALUSrcA.Rs1)
    c.io.in.bits.execCtrl.aluSrcB.poke(ALUSrcB.Rs2)
    c.io.in.bits.execCtrl.wbSel.poke(WBSel.Alu)
    c.io.in.bits.execCtrl.branchType.poke(BranchType.None)
    c.io.in.bits.execCtrl.isJump.poke(false.B)
    c.io.in.bits.execCtrl.isJalr.poke(false.B)
    c.io.in.bits.execCtrl.sys.csrOp.poke(CSROp.N)
    c.io.in.bits.execCtrl.sys.csrAddr.poke(0.U)
    c.io.in.bits.execCtrl.sys.ecall.poke(false.B)
    c.io.in.bits.execCtrl.sys.mret.poke(false.B)
    c.io.in.bits.execCtrl.sys.ebreak.poke(false.B)

    c.io.in.bits.execData.pc.poke(0.U)
    c.io.in.bits.execData.imm.poke(0.U)

    c.io.in.bits.memCtrl.en.poke(false.B)
    c.io.in.bits.memCtrl.write.poke(false.B)
    c.io.in.bits.memCtrl.unsigned.poke(false.B)
    c.io.in.bits.memCtrl.subop.poke(SizeSubop.None)

    c.io.in.bits.wbCtrl.wen.poke(false.B)
  }

  private def resetDut(c: Execute): Unit = {
    init(c)
    c.reset.poke(true.B)
    c.clock.step()
    c.reset.poke(false.B)
  }

  private def drive(c: Execute): Unit = {
    c.io.in.valid.poke(true.B)
    c.io.out.ready.poke(true.B)
  }

  "Execute" should "compute an ALU writeback result" in {
    simulate(new Execute) { c =>
      resetDut(c)
      drive(c)

      c.io.in.bits.execCtrl.aluOp.poke(ALUOp.ADD)
      c.io.in.bits.execCtrl.aluSrcA.poke(ALUSrcA.Rs1)
      c.io.in.bits.execCtrl.aluSrcB.poke(ALUSrcB.Imm)
      c.io.in.bits.execData.pc.poke(pc.U)
      c.io.in.bits.rs1.bits.rdata.poke(10.U)
      c.io.in.bits.execData.imm.poke(32.U)
      c.io.in.bits.wbCtrl.wen.poke(true.B)
      c.io.in.bits.rd.poke(5.U)

      c.io.out.valid.expect(true.B)
      c.io.in.ready.expect(true.B)
      c.io.out.bits.wbData.wdata.expect(42.U)
      c.io.out.bits.wbCtrl.wen.expect(true.B)
      c.io.out.bits.wbCtrl.rd.expect(5.U)
      c.io.out.bits.memCtrl.en.expect(false.B)
      c.io.out.bits.ifRedct.redirect.valid.expect(false.B)
      c.io.out.bits.retireTrace.get.dnpc.expect((pc + 4).U)
      c.io.out.bits.retireTrace.get.regWrite.wdata.expect(42.U)
    }
  }

  it should "carry store metadata and store data" in {
    simulate(new Execute) { c =>
      resetDut(c)
      drive(c)

      c.io.in.bits.execCtrl.aluOp.poke(ALUOp.ADD)
      c.io.in.bits.execCtrl.aluSrcA.poke(ALUSrcA.Rs1)
      c.io.in.bits.execCtrl.aluSrcB.poke(ALUSrcB.Imm)
      c.io.in.bits.rs1.bits.rdata.poke("h1000".U)
      c.io.in.bits.rs2.bits.rdata.poke("hdeadbeef".U)
      c.io.in.bits.execData.imm.poke(12.U)
      c.io.in.bits.memCtrl.en.poke(true.B)
      c.io.in.bits.memCtrl.write.poke(true.B)
      c.io.in.bits.memCtrl.unsigned.poke(false.B)
      c.io.in.bits.memCtrl.subop.poke(SizeSubop.Word)

      c.io.out.bits.memCtrl.en.expect(true.B)
      c.io.out.bits.memCtrl.write.expect(true.B)
      c.io.out.bits.memCtrl.subop.expect(SizeSubop.Word)
      c.io.out.bits.memData.addr.expect("h100c".U)
      c.io.out.bits.memData.data.expect("hdeadbeef".U)
      c.io.out.bits.wbCtrl.wen.expect(false.B)
    }
  }

  it should "redirect only taken branches" in {
    simulate(new Execute) { c =>
      resetDut(c)
      drive(c)

      c.io.in.bits.execData.pc.poke(pc.U)
      c.io.in.bits.execData.imm.poke(16.U)
      c.io.in.bits.execCtrl.branchType.poke(BranchType.Eq)
      c.io.in.bits.rs1.bits.rdata.poke(7.U)
      c.io.in.bits.rs2.bits.rdata.poke(7.U)

      c.io.out.bits.ifRedct.redirect.valid.expect(true.B)
      c.io.out.bits.ifRedct.redirect.bits.expect((pc + 16).U)
      c.io.out.bits.retireTrace.get.dnpc.expect((pc + 16).U)

      c.io.in.bits.rs2.bits.rdata.poke(8.U)
      c.io.out.bits.ifRedct.redirect.valid.expect(false.B)
      c.io.out.bits.retireTrace.get.dnpc.expect((pc + 4).U)
    }
  }

  it should "redirect jumps and jalr targets" in {
    simulate(new Execute) { c =>
      resetDut(c)
      drive(c)

      c.io.in.bits.execData.pc.poke(pc.U)
      c.io.in.bits.execData.imm.poke(32.U)
      c.io.in.bits.execCtrl.isJump.poke(true.B)
      c.io.in.bits.execCtrl.isJalr.poke(false.B)
      c.io.in.bits.execCtrl.wbSel.poke(WBSel.PcPlus4)

      c.io.out.bits.ifRedct.redirect.valid.expect(true.B)
      c.io.out.bits.ifRedct.redirect.bits.expect((pc + 32).U)
      c.io.out.bits.wbData.wdata.expect((pc + 4).U)

      c.io.in.bits.execCtrl.isJalr.poke(true.B)
      c.io.in.bits.rs1.bits.rdata.poke("h123".U)
      c.io.in.bits.execData.imm.poke(5.U)
      c.io.out.bits.ifRedct.redirect.bits.expect("h128".U)
    }
  }
}
