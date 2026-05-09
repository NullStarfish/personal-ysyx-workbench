package mycpu.core.frontend

import chisel3._
import chisel3.simulator.EphemeralSimulator._
import mycpu.common._
import org.scalatest.flatspec.AnyFlatSpec

class FetchSim extends AnyFlatSpec {
  private def init(c: Fetch): Unit = {
    c.io.fetch.ready.poke(false.B)
    c.io.reply.valid.poke(false.B)
    c.io.reply.bits.poke(0.U)
    c.io.out.ready.poke(false.B)
    c.io.redirect.valid.poke(false.B)
    c.io.redirect.bits.poke(0.U)
  }

  private def resetDut(c: Fetch): Unit = {
    init(c)
    c.reset.poke(true.B)
    c.clock.step()
    c.reset.poke(false.B)
  }

  "Fetch" should "request the reset PC and hold it until the request fires" in {
    simulate(new Fetch) { c =>
      resetDut(c)

      c.io.fetch.valid.expect(true.B)
      c.io.fetch.bits.expect(START_ADDR.U)

      c.io.fetch.ready.poke(false.B)
      c.clock.step()

      c.io.fetch.valid.expect(true.B)
      c.io.fetch.bits.expect(START_ADDR.U)
    }
  }

  it should "emit the returned instruction with the PC of the fired request" in {
    simulate(new Fetch) { c =>
      resetDut(c)

      c.io.fetch.ready.poke(true.B)
      c.io.fetch.valid.expect(true.B)
      c.io.fetch.bits.expect(START_ADDR.U)
      c.clock.step()

      c.io.fetch.ready.poke(false.B)
      c.io.reply.valid.poke(true.B)
      c.io.reply.bits.poke("h00112233".U)
      c.io.out.ready.poke(true.B)

      c.io.reply.ready.expect(true.B)
      c.io.out.valid.expect(true.B)
      c.io.out.bits.pc.expect(START_ADDR.U)
      c.io.out.bits.inst.expect("h00112233".U)
      c.io.out.bits.isException.expect(false.B)

    }
  }

  it should "advance to the next sequential PC after a completed fetch" in {
    simulate(new Fetch) { c =>
      resetDut(c)

      c.io.fetch.ready.poke(true.B)
      c.io.fetch.bits.expect(START_ADDR.U)
      c.clock.step()

      c.io.fetch.ready.poke(false.B)
      c.io.reply.valid.poke(true.B)
      c.io.reply.bits.poke("h00000013".U)
      c.io.out.ready.poke(true.B)
      c.clock.step()

      c.io.reply.valid.poke(false.B)
      c.io.out.ready.poke(false.B)
      c.io.fetch.valid.expect(true.B)
      c.io.fetch.bits.expect((START_ADDR + 4).U)
    }
  }

  it should "redirect the next request and suppress the stale response" in {
    simulate(new Fetch) { c =>
      val target = START_ADDR + 0x40

      resetDut(c)

      c.io.fetch.ready.poke(true.B)
      c.io.fetch.bits.expect(START_ADDR.U)
      c.clock.step()

      c.io.fetch.ready.poke(false.B)
      c.io.redirect.valid.poke(true.B)
      c.io.redirect.bits.poke(target.U)
      c.io.reply.valid.poke(true.B)
      c.io.reply.bits.poke("hdeadbeef".U)
      c.io.out.ready.poke(true.B)

      c.io.fetch.valid.expect(false.B)
      c.io.out.valid.expect(false.B)
      c.clock.step()

      c.io.redirect.valid.poke(false.B)
      c.io.reply.valid.poke(false.B)

      c.io.fetch.valid.expect(true.B)
      c.io.fetch.bits.expect(target.U)
    }
  }
}
