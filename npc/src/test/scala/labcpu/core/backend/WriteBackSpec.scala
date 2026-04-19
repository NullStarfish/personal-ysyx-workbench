package labcpu.core.backend

import chisel3._
import chisel3.simulator.EphemeralSimulator._
import mycpu.core.bundles._
import org.scalatest.flatspec.AnyFlatSpec

class WriteBackSpec extends AnyFlatSpec {
  private def init(c: WriteBack): Unit = {
    c.io.in.valid.poke(true.B)
    c.io.in.bits.result.poke(0.U)
    c.io.in.bits.rhs.poke(0.U)
    c.io.in.bits.wb.regWen.poke(false.B)
    c.io.in.bits.wb.rd.poke(0.U)
    c.io.in.bits.mem.valid.poke(false.B)
    c.io.in.bits.mem.write.poke(false.B)
    c.io.in.bits.mem.unsigned.poke(false.B)
    c.io.in.bits.mem.subop.poke(ExecSubop.None)
    c.io.in.bits.redirect.valid.poke(false.B)
    c.io.in.bits.redirect.bits.poke(0.U)
    c.io.in.bits.retire.pc.poke(0x100.U)
    c.io.in.bits.retire.inst.poke(0.U)
    c.io.in.bits.retire.dnpc.poke(0x104.U)
    c.io.dmemRdata.poke(0.U)
  }

  "WriteBack" should "forward alu result to regfile writeback" in {
    simulate(new WriteBack) { c =>
      init(c)
      c.io.in.bits.result.poke(5.U)
      c.io.in.bits.wb.regWen.poke(true.B)
      c.io.in.bits.wb.rd.poke(1.U)
      c.clock.step()
      c.io.regWrite.wen.expect(true.B)
      c.io.regWrite.addr.expect(1.U)
      c.io.regWrite.data.expect(5.U)
      c.io.out.retire.dnpc.expect(0x104.U)
    }
  }

  it should "extract byte loads correctly" in {
    simulate(new WriteBack) { c =>
      init(c)
      c.io.in.bits.mem.valid.poke(true.B)
      c.io.in.bits.mem.write.poke(false.B)
      c.io.in.bits.mem.unsigned.poke(true.B)
      c.io.in.bits.mem.subop.poke(ExecSubop.Byte)
      c.io.in.bits.wb.regWen.poke(true.B)
      c.io.in.bits.wb.rd.poke(2.U)
      c.io.dmemRdata.poke(1.U)
      c.clock.step()
      c.io.regWrite.data.expect(1.U)
    }
  }
}
