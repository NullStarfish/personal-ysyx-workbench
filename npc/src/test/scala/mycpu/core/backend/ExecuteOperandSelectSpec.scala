package mycpu.core.backend

import chisel3._
import chisel3.simulator.EphemeralSimulator._
import mycpu.common._
import mycpu.core.bundles._
import org.scalatest.flatspec.AnyFlatSpec

class ExecuteOperandSelectSpec extends AnyFlatSpec {
  private def initInput(c: ExecuteOperandSelect): Unit = {
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
    c.io.in.bits.pred.predictedTaken.poke(false.B)
    c.io.exForward.valid.poke(false.B)
    c.io.exForward.bits.result.poke(0.U)
    c.io.exForward.bits.rhs.poke(0.U)
    c.io.exForward.bits.wb.regWen.poke(false.B)
    c.io.exForward.bits.wb.rd.poke(0.U)
    c.io.exForward.bits.mem.valid.poke(false.B)
    c.io.exForward.bits.mem.write.poke(false.B)
    c.io.exForward.bits.mem.unsigned.poke(false.B)
    c.io.exForward.bits.mem.subop.poke(ExecSubop.None)
    c.io.exForward.bits.redirect.valid.poke(false.B)
    c.io.exForward.bits.redirect.bits.poke(0.U)
    c.io.memForward.valid.poke(false.B)
    c.io.memForward.bits.wbData.poke(0.U)
    c.io.memForward.bits.wb.regWen.poke(false.B)
    c.io.memForward.bits.wb.rd.poke(0.U)
  }

  "ExecuteOperandSelect" should "prefer EX forwarding for ALU operands" in {
    simulate(new ExecuteOperandSelect) { c =>
      initInput(c)
      c.io.in.bits.data.lhs.poke(11.U)
      c.io.in.bits.data.rhs.poke(22.U)
      c.io.in.bits.bypass.rs1Addr.poke(1.U)
      c.io.in.bits.bypass.rs2Addr.poke(1.U)
      c.io.in.bits.bypass.lhsSel.poke(OperandSelectSource.Rs1)
      c.io.in.bits.bypass.rhsSel.poke(OperandSelectSource.Rs2)
      c.io.exForward.valid.poke(true.B)
      c.io.exForward.bits.result.poke("h12345678".U)
      c.io.exForward.bits.wb.regWen.poke(true.B)
      c.io.exForward.bits.wb.rd.poke(1.U)
      c.clock.step()

      c.io.out.valid.expect(true.B)
      c.io.out.bits.data.lhs.expect("h12345678".U)
      c.io.out.bits.data.rhs.expect("h12345678".U)
    }
  }

  it should "use MEM forwarding when EX forwarding is absent" in {
    simulate(new ExecuteOperandSelect) { c =>
      initInput(c)
      c.io.in.bits.data.lhs.poke(11.U)
      c.io.in.bits.data.rhs.poke(22.U)
      c.io.in.bits.bypass.rs1Addr.poke(1.U)
      c.io.in.bits.bypass.rs2Addr.poke(1.U)
      c.io.in.bits.bypass.lhsSel.poke(OperandSelectSource.Rs1)
      c.io.in.bits.bypass.rhsSel.poke(OperandSelectSource.Rs2)
      c.io.memForward.valid.poke(true.B)
      c.io.memForward.bits.wbData.poke("h87654321".U)
      c.io.memForward.bits.wb.regWen.poke(true.B)
      c.io.memForward.bits.wb.rd.poke(1.U)
      c.clock.step()

      c.io.out.valid.expect(true.B)
      c.io.out.bits.data.lhs.expect("h87654321".U)
      c.io.out.bits.data.rhs.expect("h87654321".U)
    }
  }

  it should "forward store payload from rs2 independently" in {
    simulate(new ExecuteOperandSelect) { c =>
      initInput(c)
      c.io.in.bits.data.lhs.poke("h1000".U)
      c.io.in.bits.data.rhs.poke("haa".U)
      c.io.in.bits.data.offset.poke(4.U)
      c.io.in.bits.bypass.rs1Addr.poke(1.U)
      c.io.in.bits.bypass.rs2Addr.poke(2.U)
      c.io.in.bits.bypass.lhsSel.poke(OperandSelectSource.Rs1)
      c.io.in.bits.bypass.rhsSel.poke(OperandSelectSource.Rs2)
      c.io.exForward.valid.poke(true.B)
      c.io.exForward.bits.result.poke("h55".U)
      c.io.exForward.bits.wb.regWen.poke(true.B)
      c.io.exForward.bits.wb.rd.poke(2.U)
      c.clock.step()

      c.io.out.valid.expect(true.B)
      c.io.out.bits.data.lhs.expect("h1000".U)
      c.io.out.bits.data.rhs.expect("h55".U)
      c.io.out.bits.data.offset.expect(4.U)
    }
  }
}
