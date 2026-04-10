package mycpu.core.components

import chisel3._
import chisel3.simulator.EphemeralSimulator._
import org.scalatest.flatspec.AnyFlatSpec

class HazardUnitSpec extends AnyFlatSpec {
  private def init(c: HazardUnit): Unit = {
    c.io.decodeInst.poke(0.U)
    c.io.idLoadValid.poke(false.B)
    c.io.idLoadRd.poke(0.U)
    c.io.exLoadValid.poke(false.B)
    c.io.exLoadRd.poke(0.U)
    c.io.memPendingLoad.poke(false.B)
    c.io.memPendingRd.poke(0.U)
    c.io.exFire.poke(false.B)
    c.io.exRedirectValid.poke(false.B)
  }

  "HazardUnit" should "stall when ID/EX load blocks a dependent decode instruction" in {
    simulate(new HazardUnit) { c =>
      init(c)
      c.io.decodeInst.poke("h00108133".U) // add x2, x1, x1
      c.io.idLoadValid.poke(true.B)
      c.io.idLoadRd.poke(1.U)
      c.clock.step()

      c.io.loadUseStall.expect(true.B)
      c.io.redirectFlush.expect(false.B)
    }
  }

  it should "stall when LSU still has a pending load for a source register" in {
    simulate(new HazardUnit) { c =>
      init(c)
      c.io.decodeInst.poke("h00108133".U) // add x2, x1, x1
      c.io.memPendingLoad.poke(true.B)
      c.io.memPendingRd.poke(1.U)
      c.clock.step()

      c.io.loadUseStall.expect(true.B)
    }
  }

  it should "assert redirect flush only when execute fires a redirect" in {
    simulate(new HazardUnit) { c =>
      init(c)
      c.io.exFire.poke(true.B)
      c.io.exRedirectValid.poke(true.B)
      c.clock.step()

      c.io.redirectFlush.expect(true.B)
      c.io.loadUseStall.expect(false.B)
    }
  }
}
