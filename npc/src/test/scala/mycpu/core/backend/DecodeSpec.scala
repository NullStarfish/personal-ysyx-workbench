package mycpu.core.backend

import chisel3._
import chisel3.simulator.EphemeralSimulator._
import mycpu.common._
import mycpu.core.bundles._
import org.scalatest.flatspec.AnyFlatSpec

class DecodeSpec extends AnyFlatSpec {
  "Decode" should "decode addi into an ALU packet" in {
    simulate(new Decode) { c =>
      c.reset.poke(true.B)
      c.io.in.valid.poke(false.B)
      c.io.out.ready.poke(true.B)
      c.io.regWrite.wen.poke(false.B)
      c.io.regWrite.addr.poke(0.U)
      c.io.regWrite.data.poke(0.U)
      c.io.exForward.valid.poke(false.B)
      c.io.exForward.addr.poke(0.U)
      c.io.exForward.data.poke(0.U)
      c.io.memForward.valid.poke(false.B)
      c.io.memForward.addr.poke(0.U)
      c.io.memForward.data.poke(0.U)
      c.clock.step()
      c.reset.poke(false.B)

      c.io.in.valid.poke(true.B)
      c.io.in.bits.pc.poke(START_ADDR.U)
      c.io.in.bits.inst.poke("h00500093".U) // addi x1, x0, 5
      c.io.in.bits.isException.poke(false.B)
      c.clock.step()

      c.io.out.valid.expect(true.B)
      c.io.out.bits.exec.family.expect(ExecFamily.Alu)
      c.io.out.bits.exec.op.expect(ExecOp.Add)
      c.io.out.bits.wb.rd.expect(1.U)
      c.io.out.bits.data.lhs.expect(0.U)
      c.io.out.bits.data.rhs.expect(5.U)
      c.io.out.bits.data.offset.expect(0.U)
      c.io.out.bits.wb.regWen.expect(true.B)
    }
  }

  it should "decode lw into a MEM packet" in {
    simulate(new Decode) { c =>
      c.reset.poke(true.B)
      c.io.in.valid.poke(true.B)
      c.io.in.bits.pc.poke(START_ADDR.U)
      c.io.in.bits.inst.poke("h0040a103".U) // lw x2, 4(x1)
      c.io.in.bits.isException.poke(false.B)
      c.io.out.ready.poke(true.B)
      c.io.regWrite.wen.poke(false.B)
      c.io.regWrite.addr.poke(0.U)
      c.io.regWrite.data.poke(0.U)
      c.io.exForward.valid.poke(false.B)
      c.io.exForward.addr.poke(0.U)
      c.io.exForward.data.poke(0.U)
      c.io.memForward.valid.poke(false.B)
      c.io.memForward.addr.poke(0.U)
      c.io.memForward.data.poke(0.U)
      c.clock.step()
      c.reset.poke(false.B)
      c.clock.step()

      c.io.out.valid.expect(true.B)
      c.io.out.bits.exec.family.expect(ExecFamily.Mem)
      c.io.out.bits.exec.op.expect(ExecOp.Load)
      c.io.out.bits.exec.subop.expect(ExecSubop.Word)
      c.io.out.bits.wb.rd.expect(2.U)
      c.io.out.bits.data.lhs.expect(0.U)
      c.io.out.bits.data.rhs.expect(0.U)
      c.io.out.bits.data.offset.expect(4.U)
      c.io.out.bits.mem.valid.expect(true.B)
      c.io.out.bits.mem.write.expect(false.B)
    }
  }

  it should "decode sb using base plus offset and keep store payload in mem control" in {
    simulate(new Decode) { c =>
      c.reset.poke(true.B)
      c.io.in.valid.poke(false.B)
      c.io.out.ready.poke(true.B)
      c.io.regWrite.wen.poke(true.B)
      c.io.regWrite.addr.poke(1.U)
      c.io.regWrite.data.poke("h1000".U)
      c.io.exForward.valid.poke(false.B)
      c.io.exForward.addr.poke(0.U)
      c.io.exForward.data.poke(0.U)
      c.io.memForward.valid.poke(false.B)
      c.io.memForward.addr.poke(0.U)
      c.io.memForward.data.poke(0.U)
      c.clock.step()
      c.reset.poke(false.B)

      c.io.regWrite.wen.poke(true.B)
      c.io.regWrite.addr.poke(1.U)
      c.io.regWrite.data.poke("h1000".U)
      c.clock.step()

      c.io.regWrite.addr.poke(2.U)
      c.io.regWrite.data.poke("haa".U)
      c.clock.step()

      c.io.regWrite.wen.poke(false.B)
      c.io.regWrite.addr.poke(2.U)
      c.io.regWrite.data.poke("haa".U)
      c.io.in.valid.poke(true.B)
      c.io.in.bits.pc.poke(START_ADDR.U)
      c.io.in.bits.inst.poke("h00208023".U) // sb x2, 0(x1)
      c.io.in.bits.isException.poke(false.B)
      c.clock.step()

      c.io.out.valid.expect(true.B)
      c.io.out.bits.exec.family.expect(ExecFamily.Mem)
      c.io.out.bits.exec.op.expect(ExecOp.Store)
      c.io.out.bits.exec.subop.expect(ExecSubop.Byte)
      c.io.out.bits.data.lhs.expect("h1000".U)
      c.io.out.bits.data.rhs.expect("haa".U)
      c.io.out.bits.data.offset.expect(0.U)
      c.io.out.bits.mem.write.expect(true.B)
      c.io.out.bits.wb.regWen.expect(false.B)
    }
  }

  it should "prefer EX forwarding when building lhs" in {
    simulate(new Decode) { c =>
      c.reset.poke(true.B)
      c.io.in.valid.poke(false.B)
      c.io.out.ready.poke(true.B)
      c.io.regWrite.wen.poke(false.B)
      c.io.regWrite.addr.poke(0.U)
      c.io.regWrite.data.poke(0.U)
      c.io.exForward.valid.poke(false.B)
      c.io.exForward.addr.poke(0.U)
      c.io.exForward.data.poke(0.U)
      c.io.memForward.valid.poke(false.B)
      c.io.memForward.addr.poke(0.U)
      c.io.memForward.data.poke(0.U)
      c.clock.step()
      c.reset.poke(false.B)

      c.io.exForward.valid.poke(true.B)
      c.io.exForward.addr.poke(1.U)
      c.io.exForward.data.poke("h12345678".U)
      c.io.in.valid.poke(true.B)
      c.io.in.bits.pc.poke(START_ADDR.U)
      c.io.in.bits.inst.poke("h00108133".U) // add x2, x1, x1
      c.io.in.bits.isException.poke(false.B)
      c.clock.step()

      c.io.out.valid.expect(true.B)
      c.io.out.bits.data.lhs.expect("h12345678".U)
      c.io.out.bits.data.rhs.expect("h12345678".U)
    }
  }

  it should "use MEM forwarding when EX forwarding is absent" in {
    simulate(new Decode) { c =>
      c.reset.poke(true.B)
      c.io.in.valid.poke(false.B)
      c.io.out.ready.poke(true.B)
      c.io.regWrite.wen.poke(false.B)
      c.io.regWrite.addr.poke(0.U)
      c.io.regWrite.data.poke(0.U)
      c.io.exForward.valid.poke(false.B)
      c.io.exForward.addr.poke(0.U)
      c.io.exForward.data.poke(0.U)
      c.io.memForward.valid.poke(false.B)
      c.io.memForward.addr.poke(0.U)
      c.io.memForward.data.poke(0.U)
      c.clock.step()
      c.reset.poke(false.B)

      c.io.memForward.valid.poke(true.B)
      c.io.memForward.addr.poke(1.U)
      c.io.memForward.data.poke("h87654321".U)
      c.io.in.valid.poke(true.B)
      c.io.in.bits.pc.poke(START_ADDR.U)
      c.io.in.bits.inst.poke("h00108133".U) // add x2, x1, x1
      c.io.in.bits.isException.poke(false.B)
      c.clock.step()

      c.io.out.valid.expect(true.B)
      c.io.out.bits.data.lhs.expect("h87654321".U)
      c.io.out.bits.data.rhs.expect("h87654321".U)
    }
  }

  it should "decode beq with compare operands and redirect offset separated" in {
    simulate(new Decode) { c =>
      c.reset.poke(true.B)
      c.io.in.valid.poke(false.B)
      c.io.out.ready.poke(true.B)
      c.io.regWrite.wen.poke(false.B)
      c.io.regWrite.addr.poke(0.U)
      c.io.regWrite.data.poke(0.U)
      c.io.exForward.valid.poke(false.B)
      c.io.exForward.addr.poke(0.U)
      c.io.exForward.data.poke(0.U)
      c.io.memForward.valid.poke(false.B)
      c.io.memForward.addr.poke(0.U)
      c.io.memForward.data.poke(0.U)
      c.clock.step()
      c.reset.poke(false.B)

      c.io.in.valid.poke(true.B)
      c.io.in.bits.pc.poke(START_ADDR.U)
      c.io.in.bits.inst.poke("h00208463".U) // beq x1, x2, 8
      c.io.in.bits.isException.poke(false.B)
      c.clock.step()

      c.io.out.valid.expect(true.B)
      c.io.out.bits.exec.family.expect(ExecFamily.Branch)
      c.io.out.bits.exec.op.expect(ExecOp.Beq)
      c.io.out.bits.data.lhs.expect(0.U)
      c.io.out.bits.data.rhs.expect(0.U)
      c.io.out.bits.data.offset.expect(8.U)
      c.io.out.bits.wb.regWen.expect(false.B)
    }
  }

  it should "distinguish family from format for add and addi" in {
    simulate(new Decode) { c =>
      c.reset.poke(true.B)
      c.io.in.valid.poke(false.B)
      c.io.out.ready.poke(true.B)
      c.io.regWrite.wen.poke(false.B)
      c.io.regWrite.addr.poke(0.U)
      c.io.regWrite.data.poke(0.U)
      c.io.exForward.valid.poke(false.B)
      c.io.exForward.addr.poke(0.U)
      c.io.exForward.data.poke(0.U)
      c.io.memForward.valid.poke(false.B)
      c.io.memForward.addr.poke(0.U)
      c.io.memForward.data.poke(0.U)
      c.io.bpUpdate.valid.poke(false.B)
      c.io.bpUpdate.pc.poke(0.U)
      c.io.bpUpdate.actualTaken.poke(false.B)
      c.io.bpUpdate.predictedTaken.poke(false.B)
      c.clock.step()
      c.reset.poke(false.B)

      c.io.in.valid.poke(true.B)
      c.io.in.bits.pc.poke(START_ADDR.U)
      c.io.in.bits.inst.poke("h00500093".U) // addi x1, x0, 5
      c.io.in.bits.isException.poke(false.B)
      c.clock.step()
      c.io.out.bits.exec.family.expect(ExecFamily.Alu)
      c.io.out.bits.exec.op.expect(ExecOp.Add)
      c.io.out.bits.data.lhs.expect(0.U)
      c.io.out.bits.data.rhs.expect(5.U)

      c.io.in.bits.inst.poke("h002081b3".U) // add x3, x1, x2
      c.clock.step()
      c.io.out.bits.exec.family.expect(ExecFamily.Alu)
      c.io.out.bits.exec.op.expect(ExecOp.Add)
      c.io.out.bits.data.lhs.expect(0.U)
      c.io.out.bits.data.rhs.expect(0.U)
    }
  }
}
