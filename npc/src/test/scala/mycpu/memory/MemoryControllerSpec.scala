package mycpu.memory

import chisel3._
import chisel3.simulator.EphemeralSimulator._
import org.scalatest.flatspec.AnyFlatSpec

class MemoryReadArbiterSpec extends AnyFlatSpec {
  private def init(c: MemoryReadArbiter): Unit = {
    c.reset.poke(true.B)
    c.clock.step()
    c.reset.poke(false.B)
    c.io.fetch.a.valid.poke(false.B)
    c.io.fetch.a.bits.addr.poke(0.U)
    c.io.fetch.a.bits.size.poke(2.U)
    c.io.fetch.a.bits.len.poke(0.U)
    c.io.fetch.a.bits.write.poke(false.B)
    c.io.fetch.a.bits.id.poke(0.U)
    c.io.fetch.r.ready.poke(false.B)
    c.io.lsuA.valid.poke(false.B)
    c.io.lsuA.bits.addr.poke(0.U)
    c.io.lsuA.bits.size.poke(0.U)
    c.io.lsuA.bits.len.poke(0.U)
    c.io.lsuA.bits.write.poke(false.B)
    c.io.lsuA.bits.id.poke(0.U)
    c.io.lsuR.ready.poke(false.B)
    c.io.outA.ready.poke(false.B)
    c.io.inR.valid.poke(false.B)
    c.io.inR.bits.data.poke(0.U)
    c.io.inR.bits.resp.poke(0.U)
    c.io.inR.bits.last.poke(true.B)
    c.io.inR.bits.id.poke(0.U)
  }

  "MemoryReadArbiter" should "prefer LSU reads over fetch reads and route the reply back" in {
    simulate(new MemoryReadArbiter) { c =>
      init(c)
      c.io.fetch.a.valid.poke(true.B)
      c.io.fetch.a.bits.addr.poke("h1000".U)
      c.io.fetch.a.bits.size.poke(2.U)
      c.io.fetch.a.bits.len.poke(0.U)
      c.io.lsuA.valid.poke(true.B)
      c.io.lsuA.bits.addr.poke("h2003".U)
      c.io.lsuA.bits.size.poke(0.U)
      c.io.outA.ready.poke(true.B)

      c.io.outA.valid.expect(true.B)
      c.io.outA.bits.addr.expect("h2003".U)
      c.io.outA.bits.size.expect(0.U)
      c.io.outA.bits.len.expect(0.U)
      c.io.lsuA.ready.expect(true.B)
      c.io.fetch.a.ready.expect(false.B)
      c.clock.step()

      c.io.fetch.a.valid.poke(false.B)
      c.io.lsuA.valid.poke(false.B)
      c.io.outA.ready.poke(false.B)
      c.io.lsuR.ready.poke(true.B)
      c.io.inR.valid.poke(true.B)
      c.io.inR.bits.data.poke("hfeedbeef".U)
      c.io.inR.bits.last.poke(true.B)

      c.io.inR.ready.expect(true.B)
      c.io.lsuR.valid.expect(false.B)
      c.io.fetch.r.valid.expect(false.B)
      c.clock.step()

      c.io.inR.valid.poke(false.B)
      c.io.lsuR.valid.expect(true.B)
      c.io.lsuR.bits.data.expect("hfeedbeef".U)
      c.io.fetch.r.valid.expect(false.B)
    }
  }
}

class MemoryControllerSpec extends AnyFlatSpec {
  private def init(c: MemoryController): Unit = {
    c.reset.poke(true.B)
    c.clock.step()
    c.reset.poke(false.B)
    c.io.icache.a.valid.poke(false.B)
    c.io.icache.a.bits.addr.poke(0.U)
    c.io.icache.a.bits.size.poke(2.U)
    c.io.icache.a.bits.len.poke(0.U)
    c.io.icache.a.bits.write.poke(false.B)
    c.io.icache.a.bits.id.poke(0.U)
    c.io.icache.r.ready.poke(false.B)
    c.io.lsu.a.valid.poke(false.B)
    c.io.lsu.a.bits.addr.poke(0.U)
    c.io.lsu.a.bits.size.poke(0.U)
    c.io.lsu.a.bits.len.poke(0.U)
    c.io.lsu.a.bits.write.poke(false.B)
    c.io.lsu.a.bits.id.poke(0.U)
    c.io.lsu.w.valid.poke(false.B)
    c.io.lsu.w.bits.data.poke(0.U)
    c.io.lsu.w.bits.strb.poke(0.U)
    c.io.lsu.w.bits.last.poke(true.B)
    c.io.lsu.r.ready.poke(false.B)
    c.io.lsu.b.ready.poke(false.B)
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
    c.io.axi.r.bits.last.poke(true.B)
  }

  "MemoryController" should "translate fetch requests into AXI word reads" in {
    simulate(new MemoryController) { c =>
      init(c)
      c.io.icache.a.valid.poke(true.B)
      c.io.icache.a.bits.addr.poke("ha0000000".U)
      c.io.icache.a.bits.size.poke(2.U)
      c.io.icache.a.bits.len.poke(0.U)
      c.io.axi.ar.ready.poke(true.B)

      c.io.axi.ar.valid.expect(true.B)
      c.io.axi.ar.bits.addr.expect("ha0000000".U)
      c.io.axi.ar.bits.size.expect(2.U)
      c.io.axi.ar.bits.len.expect(0.U)
      c.io.icache.a.ready.expect(true.B)
      c.clock.step()

      c.io.icache.a.valid.poke(false.B)
      c.io.axi.ar.ready.poke(false.B)
      c.io.axi.r.valid.poke(true.B)
      c.io.axi.r.bits.data.poke("h00100093".U)
      c.io.icache.r.ready.poke(true.B)

      c.io.axi.r.ready.expect(true.B)
      c.io.icache.r.valid.expect(false.B)
      c.clock.step()

      c.io.axi.r.valid.poke(false.B)
      c.io.icache.r.valid.expect(true.B)
      c.io.icache.r.bits.data.expect("h00100093".U)
    }
  }

  it should "translate LSU writes into AXI AW/W/B and send an LSU ack" in {
    simulate(new MemoryController) { c =>
      init(c)
      c.io.lsu.a.valid.poke(true.B)
      c.io.lsu.a.bits.write.poke(true.B)
      c.io.lsu.a.bits.addr.poke("h80000003".U)
      c.io.lsu.a.bits.size.poke(0.U)
      c.io.lsu.w.valid.poke(true.B)
      c.io.lsu.w.bits.data.poke("h00aa0000".U)
      c.io.lsu.w.bits.strb.poke("b0100".U)
      c.io.lsu.w.bits.last.poke(true.B)
      c.io.axi.aw.ready.poke(true.B)
      c.io.axi.w.ready.poke(false.B)

      c.io.lsu.a.ready.expect(true.B)
      c.io.lsu.w.ready.expect(false.B)
      c.io.axi.aw.valid.expect(true.B)
      c.io.axi.aw.bits.addr.expect("h80000003".U)
      c.io.axi.aw.bits.size.expect(0.U)
      c.io.axi.w.valid.expect(true.B)
      c.io.axi.w.bits.data.expect("h00aa0000".U)
      c.io.axi.w.bits.strb.expect("b0100".U)
      c.clock.step()

      c.io.lsu.a.valid.poke(false.B)
      c.io.axi.aw.ready.poke(false.B)
      c.io.axi.w.ready.poke(true.B)

      c.io.lsu.w.ready.expect(true.B)
      c.io.axi.aw.valid.expect(false.B)
      c.io.axi.w.valid.expect(true.B)
      c.io.axi.w.bits.data.expect("h00aa0000".U)
      c.io.axi.w.bits.strb.expect("b0100".U)
      c.clock.step()

      c.io.lsu.w.valid.poke(false.B)
      c.io.axi.w.ready.poke(false.B)
      c.io.axi.b.valid.poke(true.B)
      c.io.lsu.b.ready.poke(true.B)

      c.io.lsu.b.valid.expect(true.B)
      c.io.axi.b.ready.expect(true.B)
    }
  }
}
