package mycpu.core

import chisel3._
import chisel3.simulator.EphemeralSimulator._
import org.scalatest.flatspec.AnyFlatSpec

class CoreSmokeSpec extends AnyFlatSpec {
  "Core" should "elaborate and run a few idle cycles" in {
    simulate(new Core) { c =>
      c.reset.poke(true.B)
      c.io.master.aw.ready.poke(false.B)
      c.io.master.w.ready.poke(false.B)
      c.io.master.b.valid.poke(false.B)
      c.io.master.b.bits.id.poke(0.U)
      c.io.master.b.bits.resp.poke(0.U)
      c.io.master.ar.ready.poke(false.B)
      c.io.master.r.valid.poke(false.B)
      c.io.master.r.bits.id.poke(0.U)
      c.io.master.r.bits.data.poke(0.U)
      c.io.master.r.bits.resp.poke(0.U)
      c.io.master.r.bits.last.poke(false.B)
      c.clock.step()
      c.reset.poke(false.B)
      c.clock.step(5)
    }
  }
}
