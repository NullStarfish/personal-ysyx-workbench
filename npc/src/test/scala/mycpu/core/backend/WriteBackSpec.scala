package mycpu.core.backend

import chisel3._
import chisel3.simulator.EphemeralSimulator._
import mycpu.core.bundles._
import org.scalatest.flatspec.AnyFlatSpec

class WriteBackSpec extends AnyFlatSpec {
  private def init(c: WriteBack): Unit = {
    c.io.in.valid.poke(false.B)
    c.io.in.bits.wbData.wdata.poke(0.U)
    c.io.in.bits.wbCtrl.wen.poke(false.B)
    c.io.in.bits.wbCtrl.rd.poke(0.U)
  }

  "WriteBack" should "drive register write only when wbCtrl.wen is set" in {
    simulate(new WriteBack) { c =>
      init(c)

      c.io.in.valid.poke(true.B)
      c.io.in.bits.wbData.wdata.poke("hdeadbeef".U)
      c.io.in.bits.wbCtrl.wen.poke(true.B)
      c.io.in.bits.wbCtrl.rd.poke(7.U)

      c.io.in.ready.expect(true.B)
      c.io.regWrite.regWrite.wen.expect(true.B)
      c.io.regWrite.regWrite.rd.expect(7.U)
      c.io.regWrite.regWrite.wdata.expect("hdeadbeef".U)

      c.io.in.bits.wbCtrl.wen.poke(false.B)
      c.io.regWrite.regWrite.wen.expect(false.B)
    }
  }

  it should "commit retire trace with final writeback data" in {
    simulate(new WriteBack) { c =>
      init(c)

      c.io.in.valid.poke(true.B)
      c.io.in.bits.wbData.wdata.poke("h12345678".U)
      c.io.in.bits.wbCtrl.wen.poke(true.B)
      c.io.in.bits.wbCtrl.rd.poke(2.U)
      c.io.in.bits.retireTrace.get.pc.poke("ha0000000".U)
      c.io.in.bits.retireTrace.get.inst.poke("h00000013".U)

      c.io.traceCommit.get.valid.expect(true.B)
      c.io.traceCommit.get.bits.pc.expect("ha0000000".U)
      c.io.traceCommit.get.bits.inst.expect("h00000013".U)
      c.io.traceCommit.get.bits.regWrite.wen.expect(true.B)
      c.io.traceCommit.get.bits.regWrite.rd.expect(2.U)
      c.io.traceCommit.get.bits.regWrite.wdata.expect("h12345678".U)
    }
  }
}
