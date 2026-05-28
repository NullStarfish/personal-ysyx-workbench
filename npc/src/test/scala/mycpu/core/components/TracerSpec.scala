package mycpu.core.components

import chisel3._
import chisel3.simulator.EphemeralSimulator._
import org.scalatest.flatspec.AnyFlatSpec

class TracerSpec extends AnyFlatSpec {
  private def init(c: Tracer): Unit = {
    c.io.retireTrace.valid.poke(false.B)
    c.io.retireTrace.bits.pc.poke(0.U)
    c.io.retireTrace.bits.inst.poke(0.U)
    c.io.retireTrace.bits.dnpc.poke(0.U)
    c.io.retireTrace.bits.regWrite.wen.poke(false.B)
    c.io.retireTrace.bits.regWrite.rd.poke(0.U)
    c.io.retireTrace.bits.regWrite.wdata.poke(0.U)
    c.io.retireTrace.bits.instType.poke(0.U)
    c.io.retireTrace.bits.icacheHit.poke(false.B)
    c.io.retireTrace.bits.csrs.mtvec.poke(0.U)
    c.io.retireTrace.bits.csrs.mepc.poke(0.U)
    c.io.retireTrace.bits.csrs.mstatus.poke(0.U)
    c.io.retireTrace.bits.csrs.mcause.poke(0.U)
    for (idx <- 0 until 32) {
      c.io.gprs(idx).poke(0.U)
    }
    c.io.csrs.mtvec.poke(0.U)
    c.io.csrs.mepc.poke(0.U)
    c.io.csrs.mstatus.poke(0.U)
    c.io.csrs.mcause.poke(0.U)
  }

  "Tracer" should "convert retire trace plus current architectural state into sim state" in {
    simulate(new Tracer(enableDpi = false)) { c =>
      init(c)
      c.io.retireTrace.valid.poke(true.B)
      c.io.retireTrace.bits.pc.poke("ha0000000".U)
      c.io.retireTrace.bits.inst.poke("h00100093".U)
      c.io.retireTrace.bits.dnpc.poke("ha0000004".U)
      c.io.retireTrace.bits.regWrite.wen.poke(true.B)
      c.io.retireTrace.bits.regWrite.rd.poke(1.U)
      c.io.retireTrace.bits.regWrite.wdata.poke("h12345678".U)
      c.io.retireTrace.bits.icacheHit.poke(true.B)
      c.io.gprs(1).poke("h12345678".U)
      c.io.gprs(10).poke("ha0001170".U)
      c.io.retireTrace.bits.csrs.mtvec.poke("h100".U)
      c.io.retireTrace.bits.csrs.mepc.poke("h200".U)
      c.io.retireTrace.bits.csrs.mstatus.poke("h300".U)
      c.io.retireTrace.bits.csrs.mcause.poke("h400".U)

      c.io.simState.valid.expect(true.B)
      c.io.simState.pc.expect("ha0000000".U)
      c.io.simState.inst.expect("h00100093".U)
      c.io.simState.dnpc.expect("ha0000004".U)
      c.io.simState.regWen.expect(true.B)
      c.io.simState.regAddr.expect(1.U)
      c.io.simState.regData.expect("h12345678".U)
      val expectedRegsFlat = (BigInt("a0001170", 16) << (10 * 32)) | (BigInt("12345678", 16) << 32)
      c.io.simState.regsFlat.expect(expectedRegsFlat.U)
      c.io.simState.mtvec.expect("h100".U)
      c.io.simState.mepc.expect("h200".U)
      c.io.simState.mstatus.expect("h300".U)
      c.io.simState.mcause.expect("h400".U)
      c.io.simState.icacheHit.expect(true.B)
    }
  }
}
