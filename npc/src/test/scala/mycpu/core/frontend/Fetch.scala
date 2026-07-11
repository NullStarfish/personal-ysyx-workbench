package mycpu.core.frontend

import chisel3._
import chisel3.simulator.EphemeralSimulator._
import mycpu.common.START_ADDR
import org.scalatest.flatspec.AnyFlatSpec

class FetchSim extends AnyFlatSpec {
  private def resetDut(c: Fetch): Unit = {
    c.io.block.poke(false.B)
    c.io.out.ready.poke(false.B)
    c.io.redirect.valid.poke(false.B)
    c.io.redirect.bits.poke(0.U)
    c.reset.poke(true.B)
    c.clock.step()
    c.reset.poke(false.B)
  }

  "Fetch" should "hold PC until I$0 launches the lookup" in {
    simulate(new Fetch) { c =>
      resetDut(c)

      c.io.out.valid.expect(true.B)
      c.io.out.bits.pc.expect(START_ADDR.U)
      c.clock.step(3)
      c.io.out.bits.pc.expect(START_ADDR.U)

      c.io.out.ready.poke(true.B)
      c.clock.step()
      c.io.out.bits.pc.expect((START_ADDR + 4).U)
    }
  }

  it should "suppress valid and hold PC while launch is blocked" in {
    simulate(new Fetch) { c =>
      resetDut(c)

      c.io.out.ready.poke(true.B)
      c.io.block.poke(true.B)
      c.io.out.valid.expect(false.B)
      c.clock.step(3)

      c.io.block.poke(false.B)
      c.io.out.valid.expect(true.B)
      c.io.out.bits.pc.expect(START_ADDR.U)
    }
  }

  it should "redirect with priority over a sequential launch" in {
    simulate(new Fetch) { c =>
      val target = START_ADDR + 0x40
      resetDut(c)

      c.io.out.ready.poke(true.B)
      c.io.redirect.valid.poke(true.B)
      c.io.redirect.bits.poke(target.U)
      c.io.out.valid.expect(false.B)
      c.clock.step()

      c.io.redirect.valid.poke(false.B)
      c.io.out.valid.expect(true.B)
      c.io.out.bits.pc.expect(target.U)
    }
  }
}
