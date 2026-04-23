package mycpu.core.backend

import chisel3._
import chisel3.simulator.EphemeralSimulator._
import mycpu.common._
import mycpu.core.bundles._
import org.scalatest.flatspec.AnyFlatSpec

class LSUSpec extends AnyFlatSpec {
  private def initAxi(c: LSU): Unit = {
    c.io.axi.aw.ready.poke(false.B)
    c.io.axi.w.ready.poke(false.B)
    c.io.axi.b.valid.poke(false.B)
    c.io.axi.b.bits.id.poke(0.U)
    c.io.axi.b.bits.resp.poke(0.U)
    c.io.axi.ar.ready.poke(false.B)
    c.io.axi.r.valid.poke(false.B)
    c.io.axi.r.bits.id.poke(0.U)
    c.io.axi.r.bits.data.poke(0.U)
    c.io.axi.r.bits.resp.poke(0.U)
    c.io.axi.r.bits.last.poke(false.B)
  }

  private def initInput(c: LSU): Unit = {
    c.io.in.valid.poke(false.B)
    c.io.in.bits.result.poke(0.U)
    c.io.in.bits.rhs.poke(0.U)
    c.io.in.bits.wb.regWen.poke(false.B)
    c.io.in.bits.wb.rd.poke(0.U)
    c.io.in.bits.mem.valid.poke(false.B)
    c.io.in.bits.mem.write.poke(false.B)
    c.io.in.bits.mem.unsigned.poke(false.B)
    c.io.in.bits.mem.subop.poke(ExecSubop.None)
    c.io.in.bits.redirect.poke(false.B)
    c.io.out.ready.poke(true.B)
    initAxi(c)
  }

  "LSU" should "pass through non-memory execute results" in {
    simulate(new LSU) { c =>
      c.reset.poke(true.B)
      initInput(c)
      c.io.out.ready.poke(false.B)
      c.clock.step()
      c.reset.poke(false.B)

      c.io.in.valid.poke(true.B)
      c.io.in.bits.result.poke("h12345678".U)
      c.io.in.bits.wb.regWen.poke(true.B)
      c.io.in.bits.wb.rd.poke(5.U)
      c.clock.step()

      c.io.in.valid.poke(false.B)
      c.clock.step()

      c.io.out.valid.expect(true.B)
      c.io.out.bits.wb.rd.expect(5.U)
      c.io.out.bits.wb.regWen.expect(true.B)
      c.io.out.bits.wbData.expect("h12345678".U)
      c.io.status.pendingLoad.expect(false.B)

      c.io.out.ready.poke(true.B)
      c.clock.step()
    }
  }

  it should "sign extend byte loads using mem control subop" in {
    simulate(new LSU) { c =>
      c.reset.poke(true.B)
      initInput(c)
      c.clock.step()
      c.reset.poke(false.B)

      c.io.in.valid.poke(true.B)
      c.io.in.bits.result.poke(1.U)
      c.io.in.bits.wb.regWen.poke(true.B)
      c.io.in.bits.wb.rd.poke(3.U)
      c.io.in.bits.mem.valid.poke(true.B)
      c.io.in.bits.mem.write.poke(false.B)
      c.io.in.bits.mem.unsigned.poke(false.B)
      c.io.in.bits.mem.subop.poke(ExecSubop.Byte)
      c.io.axi.ar.ready.poke(true.B)
      c.clock.step()

      c.io.in.valid.poke(false.B)
      initAxi(c)
      c.io.out.ready.poke(true.B)
      c.io.status.pendingLoad.expect(true.B)
      c.io.status.pendingRd.expect(3.U)
      c.io.axi.r.valid.poke(true.B)
      c.io.axi.r.bits.id.poke(0.U)
      c.io.axi.r.bits.data.poke("h00008011".U)
      c.io.axi.r.bits.resp.poke(0.U)
      c.io.axi.r.bits.last.poke(true.B)
      c.clock.step()

      c.io.out.valid.expect(true.B)
      c.io.out.bits.wb.rd.expect(3.U)
      c.io.out.bits.wb.regWen.expect(true.B)
      c.io.out.bits.wbData.expect("hffffff80".U)
    }
  }
}
