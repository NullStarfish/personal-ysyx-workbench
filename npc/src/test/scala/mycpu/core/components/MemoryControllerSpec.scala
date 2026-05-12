package mycpu.core.components

import chisel3._
import chisel3.simulator.EphemeralSimulator._
import org.scalatest.flatspec.AnyFlatSpec

class MemoryReadArbiterSpec extends AnyFlatSpec {
  private def init(c: MemoryReadArbiter): Unit = {
    c.reset.poke(true.B)
    c.clock.step()
    c.reset.poke(false.B)
    c.io.fetchReq.valid.poke(false.B)
    c.io.fetchReq.bits.poke(0.U)
    c.io.fetchReply.ready.poke(false.B)
    c.io.lsuReq.valid.poke(false.B)
    c.io.lsuReq.bits.addr.poke(0.U)
    c.io.lsuReq.bits.data.poke(0.U)
    c.io.lsuReq.bits.strb.poke(0.U)
    c.io.lsuReq.bits.write.poke(false.B)
    c.io.lsuReq.bits.size.poke(0.U)
    c.io.lsuReply.ready.poke(false.B)
    c.io.outReq.ready.poke(false.B)
    c.io.inReply.valid.poke(false.B)
    c.io.inReply.bits.poke(0.U)
  }

  "MemoryReadArbiter" should "prefer LSU reads over fetch reads and route the reply back" in {
    simulate(new MemoryReadArbiter) { c =>
      init(c)
      c.io.fetchReq.valid.poke(true.B)
      c.io.fetchReq.bits.poke("h1000".U)
      c.io.lsuReq.valid.poke(true.B)
      c.io.lsuReq.bits.addr.poke("h2003".U)
      c.io.lsuReq.bits.size.poke(0.U)
      c.io.outReq.ready.poke(true.B)

      c.io.outReq.valid.expect(true.B)
      c.io.outReq.bits.addr.expect("h2003".U)
      c.io.outReq.bits.size.expect(0.U)
      c.io.lsuReq.ready.expect(true.B)
      c.io.fetchReq.ready.expect(false.B)
      c.clock.step()

      c.io.fetchReq.valid.poke(false.B)
      c.io.lsuReq.valid.poke(false.B)
      c.io.outReq.ready.poke(false.B)
      c.io.lsuReply.ready.poke(true.B)
      c.io.inReply.valid.poke(true.B)
      c.io.inReply.bits.poke("hfeedbeef".U)

      c.io.inReply.ready.expect(true.B)
      c.io.lsuReply.valid.expect(false.B)
      c.io.fetchReply.valid.expect(false.B)
      c.clock.step()

      c.io.inReply.valid.poke(false.B)
      c.io.lsuReply.valid.expect(true.B)
      c.io.lsuReply.bits.expect("hfeedbeef".U)
      c.io.fetchReply.valid.expect(false.B)
    }
  }
}

class MemoryControllerSpec extends AnyFlatSpec {
  private def init(c: MemoryController): Unit = {
    c.reset.poke(true.B)
    c.clock.step()
    c.reset.poke(false.B)
    c.io.fetchReq.valid.poke(false.B)
    c.io.fetchReq.bits.poke(0.U)
    c.io.fetchReply.ready.poke(false.B)
    c.io.lsuReq.valid.poke(false.B)
    c.io.lsuReq.bits.addr.poke(0.U)
    c.io.lsuReq.bits.data.poke(0.U)
    c.io.lsuReq.bits.strb.poke(0.U)
    c.io.lsuReq.bits.write.poke(false.B)
    c.io.lsuReq.bits.size.poke(0.U)
    c.io.lsuReply.ready.poke(false.B)
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
      c.io.fetchReq.valid.poke(true.B)
      c.io.fetchReq.bits.poke("ha0000000".U)
      c.io.axi.ar.ready.poke(true.B)

      c.io.axi.ar.valid.expect(true.B)
      c.io.axi.ar.bits.addr.expect("ha0000000".U)
      c.io.axi.ar.bits.size.expect(2.U)
      c.io.fetchReq.ready.expect(true.B)
      c.clock.step()

      c.io.fetchReq.valid.poke(false.B)
      c.io.axi.ar.ready.poke(false.B)
      c.io.axi.r.valid.poke(true.B)
      c.io.axi.r.bits.data.poke("h00100093".U)
      c.io.fetchReply.ready.poke(true.B)

      c.io.axi.r.ready.expect(true.B)
      c.io.fetchReply.valid.expect(false.B)
      c.clock.step()

      c.io.axi.r.valid.poke(false.B)
      c.io.fetchReply.valid.expect(true.B)
      c.io.fetchReply.bits.expect("h00100093".U)
    }
  }

  it should "translate LSU writes into AXI AW/W/B and send an LSU ack" in {
    simulate(new MemoryController) { c =>
      init(c)
      c.io.lsuReq.valid.poke(true.B)
      c.io.lsuReq.bits.write.poke(true.B)
      c.io.lsuReq.bits.addr.poke("h80000003".U)
      c.io.lsuReq.bits.data.poke("h00aa0000".U)
      c.io.lsuReq.bits.strb.poke("b0100".U)
      c.io.lsuReq.bits.size.poke(0.U)
      c.io.axi.aw.ready.poke(true.B)
      c.io.axi.w.ready.poke(false.B)

      c.io.lsuReq.ready.expect(false.B)
      c.io.axi.aw.valid.expect(true.B)
      c.io.axi.aw.bits.addr.expect("h80000003".U)
      c.io.axi.aw.bits.size.expect(0.U)
      c.io.axi.w.valid.expect(true.B)
      c.io.axi.w.bits.data.expect("h00aa0000".U)
      c.io.axi.w.bits.strb.expect("b0100".U)
      c.clock.step()

      c.io.axi.aw.ready.poke(false.B)
      c.io.axi.w.ready.poke(true.B)

      c.io.lsuReq.ready.expect(true.B)
      c.io.axi.aw.valid.expect(false.B)
      c.io.axi.w.valid.expect(true.B)
      c.io.axi.w.bits.data.expect("h00aa0000".U)
      c.io.axi.w.bits.strb.expect("b0100".U)
      c.clock.step()

      c.io.lsuReq.valid.poke(false.B)
      c.io.axi.aw.ready.poke(false.B)
      c.io.axi.w.ready.poke(false.B)
      c.io.axi.b.valid.poke(true.B)
      c.io.lsuReply.ready.poke(true.B)

      c.io.lsuReply.valid.expect(true.B)
      c.io.axi.b.ready.expect(true.B)
    }
  }
}
