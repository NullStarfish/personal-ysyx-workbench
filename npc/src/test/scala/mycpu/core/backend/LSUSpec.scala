package mycpu.core.backend

import chisel3._
import chisel3.simulator.EphemeralSimulator._
import mycpu.core.bundles._
import org.scalatest.flatspec.AnyFlatSpec

class LSUSpec extends AnyFlatSpec {
  private def initInput(c: LSU): Unit = {
    c.io.in.valid.poke(false.B)
    c.io.in.bits.lhs.poke(0.U)
    c.io.in.bits.rhs.poke(0.U)
    c.io.in.bits.wbCtrl.wen.poke(false.B)
    c.io.in.bits.wbCtrl.rd.poke(0.U)
    c.io.in.bits.memCtrl.en.poke(false.B)
    c.io.in.bits.memCtrl.write.poke(false.B)
    c.io.in.bits.memCtrl.unsigned.poke(false.B)
    c.io.in.bits.memCtrl.subop.poke(SizeSubop.None)
    c.io.mem.a.ready.poke(false.B)
    c.io.mem.w.ready.poke(false.B)
    c.io.mem.r.valid.poke(false.B)
    c.io.mem.r.bits.data.poke(0.U)
    c.io.mem.r.bits.resp.poke(0.U)
    c.io.mem.r.bits.last.poke(true.B)
    c.io.mem.r.bits.id.poke(0.U)
    c.io.mem.b.valid.poke(false.B)
    c.io.mem.b.bits.resp.poke(0.U)
    c.io.mem.b.bits.id.poke(0.U)
    c.io.out.ready.poke(true.B)
  }

  private def resetDut(c: LSU): Unit = {
    initInput(c)
    c.reset.poke(true.B)
    c.clock.step()
    c.reset.poke(false.B)
  }

  "LSU" should "pass through non-memory execute results without a memory request" in {
    simulate(new LSU) { c =>
      resetDut(c)

      c.io.in.valid.poke(true.B)
      c.io.in.bits.lhs.poke("h12345678".U)
      c.io.in.bits.wbCtrl.wen.poke(true.B)
      c.io.in.bits.wbCtrl.rd.poke(5.U)

      c.io.in.ready.expect(true.B)
      c.io.mem.a.valid.expect(false.B)
      c.io.mem.w.valid.expect(false.B)
      c.io.out.valid.expect(true.B)
      c.io.out.bits.wbCtrl.rd.expect(5.U)
      c.io.out.bits.wbCtrl.wen.expect(true.B)
      c.io.out.bits.wbData.wdata.expect("h12345678".U)
      c.clock.step()

      c.io.in.valid.poke(false.B)
      c.io.out.valid.expect(false.B)
    }
  }

  it should "keep its private memory protocol quiet during reset" in {
    simulate(new LSU) { c =>
      initInput(c)

      c.reset.poke(true.B)
      c.io.in.valid.poke(true.B)
      c.io.in.bits.rhs.poke("h80000000".U)
      c.io.in.bits.memCtrl.en.poke(true.B)
      c.io.in.bits.memCtrl.write.poke(false.B)
      c.io.in.bits.memCtrl.subop.poke(SizeSubop.Word)
      c.io.mem.a.ready.poke(true.B)
      c.io.mem.r.valid.poke(true.B)
      c.io.mem.r.bits.data.poke("h12345678".U)

      c.io.in.ready.expect(false.B)
      c.io.mem.a.valid.expect(false.B)
      c.io.mem.r.ready.expect(false.B)
      c.io.out.valid.expect(false.B)
      c.io.pendingLoad.valid.expect(false.B)
      c.clock.step()

      c.reset.poke(false.B)
      c.io.in.ready.expect(true.B)
      c.io.mem.a.valid.expect(true.B)
    }
  }

  it should "issue one load request and sign extend the reply" in {
    simulate(new LSU) { c =>
      resetDut(c)

      c.io.in.valid.poke(true.B)
      c.io.in.bits.rhs.poke(1.U)
      c.io.in.bits.wbCtrl.wen.poke(true.B)
      c.io.in.bits.wbCtrl.rd.poke(3.U)
      c.io.in.bits.memCtrl.en.poke(true.B)
      c.io.in.bits.memCtrl.write.poke(false.B)
      c.io.in.bits.memCtrl.unsigned.poke(false.B)
      c.io.in.bits.memCtrl.subop.poke(SizeSubop.Byte)
      c.io.mem.a.ready.poke(true.B)

      c.io.mem.a.valid.expect(true.B)
      c.io.mem.a.bits.addr.expect(1.U)
      c.io.mem.a.bits.write.expect(false.B)
      c.io.mem.a.bits.size.expect(0.U)
      c.io.mem.a.bits.len.expect(0.U)
      c.clock.step()

      c.io.in.valid.poke(false.B)
      c.io.mem.a.ready.poke(false.B)
      c.io.mem.a.valid.expect(false.B)
      c.io.in.ready.expect(false.B)

      c.io.mem.r.valid.poke(true.B)
      c.io.mem.r.bits.data.poke("h00008011".U)
      c.io.mem.r.ready.expect(true.B)
      c.io.out.valid.expect(true.B)
      c.io.out.bits.wbCtrl.rd.expect(3.U)
      c.io.out.bits.wbCtrl.wen.expect(true.B)
      c.io.out.bits.wbData.wdata.expect("hffffff80".U)
      c.clock.step()
    }
  }

  it should "issue one store request and wait for an ack reply" in {
    simulate(new LSU) { c =>
      resetDut(c)

      c.io.in.valid.poke(true.B)
      c.io.in.bits.lhs.poke("h000000aa".U)
      c.io.in.bits.rhs.poke(3.U)
      c.io.in.bits.memCtrl.en.poke(true.B)
      c.io.in.bits.memCtrl.write.poke(true.B)
      c.io.in.bits.memCtrl.subop.poke(SizeSubop.Byte)
      c.io.mem.a.ready.poke(true.B)
      c.io.mem.w.ready.poke(true.B)

      c.io.mem.a.valid.expect(true.B)
      c.io.mem.a.bits.addr.expect(3.U)
      c.io.mem.a.bits.write.expect(true.B)
      c.io.mem.a.bits.size.expect(0.U)
      c.io.mem.w.valid.expect(true.B)
      c.io.mem.w.bits.data.expect("haa000000".U)
      c.io.mem.w.bits.strb.expect("b1000".U)
      c.io.mem.w.bits.last.expect(true.B)
      c.clock.step()

      c.io.in.valid.poke(false.B)
      c.io.mem.a.ready.poke(false.B)
      c.io.mem.w.ready.poke(false.B)
      c.io.out.valid.expect(false.B)
      c.io.mem.b.valid.poke(true.B)
      c.io.out.valid.expect(true.B)
      c.io.mem.b.ready.expect(true.B)
      c.io.out.bits.wbCtrl.wen.expect(false.B)
      c.clock.step()
    }
  }
}
