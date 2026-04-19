package labcpu.core.frontend

import chisel3._
import chisel3.simulator.EphemeralSimulator._
import org.scalatest.flatspec.AnyFlatSpec

class FetchSpec extends AnyFlatSpec {
  "Fetch" should "increment pc when unstalled" in {
    simulate(new Fetch(startAddr = 0x100L)) { c =>
      c.reset.poke(true.B)
      c.io.imem.rdata.poke("h00000013".U)
      c.io.out.ready.poke(true.B)
      c.io.stall.poke(false.B)
      c.io.redirect.valid.poke(false.B)
      c.io.redirect.bits.poke(0.U)
      c.clock.step()
      c.reset.poke(false.B)
      c.clock.step()
      c.io.out.bits.pc.expect(0x104.U)
    }
  }

  it should "redirect pc explicitly" in {
    simulate(new Fetch(startAddr = 0x100L)) { c =>
      c.reset.poke(true.B)
      c.io.imem.rdata.poke("h00000013".U)
      c.io.out.ready.poke(true.B)
      c.io.stall.poke(false.B)
      c.io.redirect.valid.poke(true.B)
      c.io.redirect.bits.poke(0x200.U)
      c.clock.step()
      c.reset.poke(false.B)
      c.clock.step()
      c.io.out.bits.pc.expect(0x200.U)
    }
  }
}
