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
      c.io.bpUpdate.valid.poke(false.B)
      c.io.bpUpdate.index.poke(0.U)
      c.io.bpUpdate.predictedTaken.poke(false.B)
      c.io.bpUpdateRedirect.poke(false.B)
      c.clock.step()
      c.reset.poke(false.B)

      c.io.in.valid.poke(true.B)
      c.io.in.bits.pc.poke(START_ADDR.U)
      c.io.in.bits.inst.poke("h00500093".U)
      c.io.in.bits.isException.poke(false.B)
      c.clock.step()

      c.io.out.valid.expect(true.B)
      c.io.out.bits.exec.aluOp.expect(ALUOp.ADD)
      c.io.out.bits.exec.aluSrcA.expect(ALUSrcA.Rs1)
      c.io.out.bits.exec.aluSrcB.expect(ALUSrcB.Imm)
      c.io.out.bits.wb.rd.expect(1.U)
      c.io.out.bits.data.rs1.expect(0.U)
      c.io.out.bits.data.rs2.expect(0.U)
      c.io.out.bits.data.imm.expect(5.U)
      c.io.out.bits.bypass.rs1Addr.expect(0.U)
      c.io.out.bits.bypass.rs2Addr.expect(5.U)
      c.io.out.bits.wb.regWen.expect(true.B)
    }
  }

  it should "decode lw into a MEM packet" in {
    simulate(new Decode) { c =>
      c.reset.poke(true.B)
      c.io.in.valid.poke(true.B)
      c.io.in.bits.pc.poke(START_ADDR.U)
      c.io.in.bits.inst.poke("h0040a103".U)
      c.io.in.bits.isException.poke(false.B)
      c.io.out.ready.poke(true.B)
      c.io.regWrite.wen.poke(false.B)
      c.io.regWrite.addr.poke(0.U)
      c.io.regWrite.data.poke(0.U)
      c.io.bpUpdate.valid.poke(false.B)
      c.io.bpUpdate.index.poke(0.U)
      c.io.bpUpdate.predictedTaken.poke(false.B)
      c.io.bpUpdateRedirect.poke(false.B)
      c.clock.step()
      c.reset.poke(false.B)
      c.clock.step()

      c.io.out.valid.expect(true.B)
      c.io.out.bits.exec.aluOp.expect(ALUOp.ADD)
      c.io.out.bits.exec.aluSrcA.expect(ALUSrcA.Rs1)
      c.io.out.bits.exec.aluSrcB.expect(ALUSrcB.Imm)
      c.io.out.bits.wb.rd.expect(2.U)
      c.io.out.bits.data.rs1.expect(0.U)
      c.io.out.bits.data.imm.expect(4.U)
      c.io.out.bits.bypass.rs1Addr.expect(1.U)
      c.io.out.bits.mem.subop.expect(ExecSubop.Word)
      c.io.out.bits.mem.valid.expect(true.B)
      c.io.out.bits.mem.write.expect(false.B)
    }
  }

  it should "decode sb using base plus offset and keep store payload" in {
    simulate(new Decode) { c =>
      c.reset.poke(true.B)
      c.io.in.valid.poke(false.B)
      c.io.out.ready.poke(true.B)
      c.io.regWrite.wen.poke(true.B)
      c.io.regWrite.addr.poke(1.U)
      c.io.regWrite.data.poke("h1000".U)
      c.io.bpUpdate.valid.poke(false.B)
      c.io.bpUpdate.index.poke(0.U)
      c.io.bpUpdate.predictedTaken.poke(false.B)
      c.io.bpUpdateRedirect.poke(false.B)
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
      c.io.in.valid.poke(true.B)
      c.io.in.bits.pc.poke(START_ADDR.U)
      c.io.in.bits.inst.poke("h00208023".U)
      c.io.in.bits.isException.poke(false.B)
      c.clock.step()

      c.io.out.valid.expect(true.B)
      c.io.out.bits.exec.aluOp.expect(ALUOp.ADD)
      c.io.out.bits.exec.aluSrcA.expect(ALUSrcA.Rs1)
      c.io.out.bits.exec.aluSrcB.expect(ALUSrcB.Imm)
      c.io.out.bits.data.rs1.expect("h1000".U)
      c.io.out.bits.data.rs2.expect("haa".U)
      c.io.out.bits.data.imm.expect(0.U)
      c.io.out.bits.bypass.rs1Addr.expect(1.U)
      c.io.out.bits.bypass.rs2Addr.expect(2.U)
      c.io.out.bits.mem.subop.expect(ExecSubop.Byte)
      c.io.out.bits.mem.write.expect(true.B)
      c.io.out.bits.wb.regWen.expect(false.B)
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
      c.io.bpUpdate.valid.poke(false.B)
      c.io.bpUpdate.index.poke(0.U)
      c.io.bpUpdate.predictedTaken.poke(false.B)
      c.io.bpUpdateRedirect.poke(false.B)
      c.clock.step()
      c.reset.poke(false.B)

      c.io.in.valid.poke(true.B)
      c.io.in.bits.pc.poke(START_ADDR.U)
      c.io.in.bits.inst.poke("h00208463".U)
      c.io.in.bits.isException.poke(false.B)
      c.clock.step()

      c.io.out.valid.expect(true.B)
      c.io.out.bits.exec.branchType.expect(BranchType.Eq)
      c.io.out.bits.data.rs1.expect(0.U)
      c.io.out.bits.data.rs2.expect(0.U)
      c.io.out.bits.data.imm.expect(8.U)
      c.io.out.bits.bypass.rs1Addr.expect(1.U)
      c.io.out.bits.bypass.rs2Addr.expect(2.U)
      c.io.out.bits.wb.regWen.expect(false.B)
    }
  }
}
