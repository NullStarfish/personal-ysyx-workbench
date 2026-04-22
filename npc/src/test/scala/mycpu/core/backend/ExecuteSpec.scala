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
    c.io.in.bits.data.lhs.poke(0.U)
    c.io.in.bits.data.rhs.poke(0.U)
    c.io.in.bits.data.offset.poke(0.U)
    c.io.in.bits.bypass.rs1Addr.poke(0.U)
    c.io.in.bits.bypass.rs2Addr.poke(0.U)
    c.io.in.bits.bypass.lhsSel.poke(OperandSelectSource.None)
    c.io.in.bits.bypass.rhsSel.poke(OperandSelectSource.None)
    c.io.in.bits.exec.family.poke(ExecFamily.Alu)
    c.io.in.bits.exec.op.poke(ExecOp.Nop)
    c.io.in.bits.exec.subop.poke(ExecSubop.None)
    c.io.in.bits.wb.regWen.poke(false.B)
    c.io.in.bits.wb.rd.poke(0.U)
    c.io.in.bits.mem.valid.poke(false.B)
    c.io.in.bits.mem.write.poke(false.B)
    c.io.in.bits.mem.unsigned.poke(false.B)
    c.io.in.bits.mem.subop.poke(ExecSubop.None)
    c.io.in.bits.sys.csrAddr.poke(0.U)
    c.io.in.bits.sys.isEcall.poke(false.B)
    c.io.in.bits.sys.isMret.poke(false.B)
    c.io.in.bits.sys.isEbreak.poke(false.B)
  }

  "Execute" should "compute an ALU add in one cycle" in {
    simulate(new Execute) { c =>
      initInput(c)
      c.io.in.bits.data.lhs.poke(2.U)
      c.io.in.bits.data.rhs.poke(3.U)
      c.io.in.bits.exec.family.poke(ExecFamily.Alu)
      c.io.in.bits.exec.op.poke(ExecOp.Add)
      c.io.in.bits.wb.regWen.poke(true.B)
      c.io.in.bits.wb.rd.poke(1.U)
      c.clock.step()

      c.io.out.valid.expect(true.B)
      c.io.out.bits.result.expect(5.U)
      c.io.out.bits.wb.rd.expect(1.U)
      c.io.out.bits.redirect.valid.expect(false.B)
    }
  }

  it should "produce redirect for jal" in {
    simulate(new Execute) { c =>
      initInput(c)
      c.io.in.bits.data.pc.poke(START_ADDR.U)
      c.io.in.bits.data.lhs.poke(START_ADDR.U)
      c.io.in.bits.data.rhs.poke(0.U)
      c.io.in.bits.exec.family.poke(ExecFamily.Jump)
      c.io.in.bits.exec.op.poke(ExecOp.Jal)
      c.io.in.bits.wb.regWen.poke(true.B)
      c.io.in.bits.wb.rd.poke(1.U)
      c.io.in.bits.data.offset.poke(8.U)
      c.clock.step()

      c.io.out.valid.expect(true.B)
      c.io.out.bits.result.expect((START_ADDR + 4).U)
      c.io.out.bits.redirect.valid.expect(true.B)
      c.io.out.bits.redirect.bits.expect((START_ADDR + 8).U)
    }
  }

  it should "take branch when compare result matches op semantics" in {
    simulate(new Execute) { c =>
      initInput(c)
      c.io.in.bits.data.pc.poke(START_ADDR.U)
      c.io.in.bits.data.lhs.poke(7.U)
      c.io.in.bits.data.rhs.poke(7.U)
      c.io.in.bits.exec.family.poke(ExecFamily.Branch)
      c.io.in.bits.exec.op.poke(ExecOp.Beq)
      c.io.in.bits.data.offset.poke(8.U)
      c.clock.step()

      c.io.out.valid.expect(true.B)
      c.io.out.bits.redirect.valid.expect(true.B)
      c.io.out.bits.redirect.bits.expect((START_ADDR + 8).U)
      c.io.out.bits.wb.regWen.expect(false.B)
    }
  }

  it should "mark mem request in EX for store" in {
    simulate(new Execute) { c =>
      initInput(c)
      c.io.in.bits.data.lhs.poke("h30000010".U)
      c.io.in.bits.data.rhs.poke("h000000aa".U)
      c.io.in.bits.data.offset.poke("h00000004".U)
      c.io.in.bits.exec.family.poke(ExecFamily.Mem)
      c.io.in.bits.exec.op.poke(ExecOp.Store)
      c.io.in.bits.exec.subop.poke(ExecSubop.Byte)
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
