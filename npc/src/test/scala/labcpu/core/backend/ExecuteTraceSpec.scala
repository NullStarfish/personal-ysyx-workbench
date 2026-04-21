package labcpu.core.backend

import chisel3._
import chisel3.simulator.EphemeralSimulator._
import mycpu.common._
import mycpu.core.backend.Execute
import mycpu.core.bundles._
import org.scalatest.flatspec.AnyFlatSpec

class ExecuteTraceSpec extends AnyFlatSpec {
  "Execute" should "report taken branch targets through trace sideband even when prediction is correct" in {
    simulate(new Execute) { c =>
      c.io.in.valid.poke(true.B)
      c.io.in.bits.data.pc.poke(0x100.U)
      c.io.in.bits.data.lhs.poke(1.U)
      c.io.in.bits.data.rhs.poke(1.U)
      c.io.in.bits.data.offset.poke(8.U)
      c.io.in.bits.exec.family.poke(ExecFamily.Branch)
      c.io.in.bits.exec.op.poke(ExecOp.Beq)
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
      c.io.in.bits.pred.predictedTaken.poke(true.B)
      c.io.in.bits.trace.get.pc.poke(0x100.U)
      c.io.in.bits.trace.get.inst.poke("h00108463".U)
      c.io.in.bits.trace.get.dnpc.poke(0x104.U)
      c.io.out.ready.poke(true.B)
      c.reset.poke(true.B)
      c.clock.step()
      c.reset.poke(false.B)
      c.clock.step()

      c.io.out.bits.trace.get.dnpc.expect(0x108.U)
      c.io.out.bits.trace.get.actualTaken.expect(true.B)
      c.io.out.bits.trace.get.predictedTaken.expect(true.B)
      c.io.out.bits.trace.get.redirectValid.expect(false.B)
      c.io.out.bits.trace.get.exValid.expect(true.B)
      c.io.out.bits.trace.get.branchResolved.expect(true.B)
      c.io.out.bits.trace.get.branchCorrect.expect(true.B)
    }
  }
}
