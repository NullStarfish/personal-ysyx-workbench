package mycpu.core.backend

import chisel3._
import chisel3.simulator.EphemeralSimulator._
import mycpu.common._
import mycpu.core.bundles._
import org.scalatest.flatspec.AnyFlatSpec

class WriteBackSpec extends AnyFlatSpec {
  private def init(c: WriteBack): Unit = {
    c.io.in.valid.poke(false.B)
    c.io.in.bits.wbData.wdata.poke(0.U)
    c.io.in.bits.wbCtrl.wen.poke(false.B)
    c.io.in.bits.wbCtrl.rd.poke(0.U)
    c.io.in.bits.sys.ebreak.poke(false.B)
    c.io.in.bits.sys.mret.poke(false.B)
    c.io.in.bits.sys.fencei.poke(false.B)
    c.io.in.bits.inst.except.valid.poke(false.B)
    c.io.in.bits.inst.except.no.poke(ExceptionNumber.ECallM)
    c.io.in.bits.inst.pc.poke(0.U)
    c.io.in.bits.sys.csr.csrOp.poke(CSROp.N)
    c.io.in.bits.sys.csr.csrAddr.poke(0.U)
    c.io.csr.rdata.poke(0.U)
    c.io.csr.evec.poke(0.U)
    c.io.csr.epc.poke(0.U)
    c.io.csr.retireCsrs.mtvec.poke(0.U)
    c.io.csr.retireCsrs.mepc.poke(0.U)
    c.io.csr.retireCsrs.mstatus.poke(0.U)
    c.io.csr.retireCsrs.mcause.poke(0.U)
  }

  "WriteBack" should "commit CSR reads and writes at writeback" in {
    simulate(new WriteBack) { c =>
      init(c)

      c.io.in.valid.poke(true.B)
      c.io.in.bits.wbCtrl.wen.poke(true.B)
      c.io.in.bits.wbCtrl.rd.poke(6.U)
      c.io.in.bits.sys.csr.csrOp.poke(CSROp.W)
      c.io.in.bits.sys.csr.csrAddr.poke("h305".U)
      c.io.in.bits.wbData.wdata.poke("h81234567".U)
      c.io.csr.rdata.poke("ha0000040".U)

      c.io.csr.cmd.expect(CSROp.W)
      c.io.csr.addr.expect("h305".U)
      c.io.csr.wdata.expect("h81234567".U)
      c.io.regWrite.regWrite.wen.expect(true.B)
      c.io.regWrite.regWrite.wdata.expect("ha0000040".U)

      c.io.in.valid.poke(false.B)
      c.io.csr.cmd.expect(CSROp.N)
      c.io.regWrite.regWrite.wen.expect(false.B)
    }
  }

  it should "redirect system instructions from writeback" in {
    simulate(new WriteBack) { c =>
      init(c)

      c.io.in.valid.poke(true.B)
      c.io.in.bits.inst.except.valid.poke(true.B)
      c.io.in.bits.inst.except.no.poke(ExceptionNumber.ECallM)
      c.io.in.bits.inst.pc.poke("ha0000080".U)
      c.io.csr.evec.poke("ha0000100".U)

      c.io.csr.except.valid.expect(true.B)
      c.io.csr.except.no.expect(ExceptionNumber.ECallM)
      c.io.csr.except.pc.expect("ha0000080".U)
      c.io.redirect.valid.expect(true.B)
      c.io.redirect.bits.expect("ha0000100".U)
    }
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

  it should "emit retire trace with final writeback data" in {
    simulate(new WriteBack) { c =>
      init(c)

      c.io.in.valid.poke(true.B)
      c.io.in.bits.wbData.wdata.poke("h12345678".U)
      c.io.in.bits.wbCtrl.wen.poke(true.B)
      c.io.in.bits.wbCtrl.rd.poke(2.U)
      c.io.in.bits.retireTrace.get.pc.poke("ha0000000".U)
      c.io.in.bits.retireTrace.get.inst.poke("h00000013".U)

      c.io.regWrite.regWrite.wen.expect(true.B)
      c.io.regWrite.regWrite.rd.expect(2.U)
      c.io.regWrite.regWrite.wdata.expect("h12345678".U)
      c.io.retireTrace.get.valid.expect(true.B)
      c.io.retireTrace.get.bits.pc.expect("ha0000000".U)
      c.io.retireTrace.get.bits.inst.expect("h00000013".U)
      c.io.retireTrace.get.bits.regWrite.wen.expect(true.B)
      c.io.retireTrace.get.bits.regWrite.rd.expect(2.U)
      c.io.retireTrace.get.bits.regWrite.wdata.expect("h12345678".U)
    }
  }
}
