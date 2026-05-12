package mycpu.core.components

import chisel3._
import chisel3.simulator.EphemeralSimulator._
import org.scalatest.flatspec.AnyFlatSpec

class HazardUnitSpec extends AnyFlatSpec {
  private def init(c: HazardUnit): Unit = {
    c.io.raw.decode.rs1.valid.poke(false.B)
    c.io.raw.decode.rs1.addr.poke(0.U)
    c.io.raw.decode.rs2.valid.poke(false.B)
    c.io.raw.decode.rs2.addr.poke(0.U)

    c.io.raw.idExLoad.valid.poke(false.B)
    c.io.raw.idExLoad.addr.poke(0.U)
    c.io.raw.lsuLoad.valid.poke(false.B)
    c.io.raw.lsuLoad.addr.poke(0.U)
    c.io.raw.lsuToMemWbFire.poke(false.B)
    c.io.ctrl.redirect.poke(false.B)
  }

  "HazardUnit" should "stall when ID/EX load blocks a dependent decode instruction" in {
    simulate(new HazardUnit) { c =>
      init(c)
      c.io.raw.decode.rs1.valid.poke(true.B)
      c.io.raw.decode.rs1.addr.poke(1.U)
      c.io.raw.idExLoad.valid.poke(true.B)
      c.io.raw.idExLoad.addr.poke(1.U)

      c.io.raw.loadUseStall.expect(true.B)
      c.io.stall.expect(true.B)
    }
  }

  it should "stall when LSU has a pending dependent load" in {
    simulate(new HazardUnit) { c =>
      init(c)
      c.io.raw.decode.rs2.valid.poke(true.B)
      c.io.raw.decode.rs2.addr.poke(4.U)
      c.io.raw.lsuLoad.valid.poke(true.B)
      c.io.raw.lsuLoad.addr.poke(4.U)

      c.io.raw.loadUseStall.expect(true.B)
    }
  }

  it should "release an LSU dependency when the result fires into MEM/WB" in {
    simulate(new HazardUnit) { c =>
      init(c)
      c.io.raw.decode.rs2.valid.poke(true.B)
      c.io.raw.decode.rs2.addr.poke(4.U)
      c.io.raw.lsuLoad.valid.poke(true.B)
      c.io.raw.lsuLoad.addr.poke(4.U)
      c.io.raw.lsuToMemWbFire.poke(true.B)

      c.io.raw.loadUseStall.expect(false.B)
    }
  }

  it should "ignore rd zero and unused operands" in {
    simulate(new HazardUnit) { c =>
      init(c)
      c.io.raw.decode.rs1.valid.poke(false.B)
      c.io.raw.decode.rs1.addr.poke(1.U)
      c.io.raw.idExLoad.valid.poke(true.B)
      c.io.raw.idExLoad.addr.poke(1.U)
      c.io.raw.loadUseStall.expect(false.B)

      c.io.raw.decode.rs1.valid.poke(true.B)
      c.io.raw.idExLoad.addr.poke(0.U)
      c.io.raw.loadUseStall.expect(false.B)
    }
  }

  it should "expose redirect as the control flush view" in {
    simulate(new HazardUnit) { c =>
      init(c)
      c.io.ctrl.redirect.poke(true.B)
      c.io.ctrl.flush.expect(true.B)
      c.io.flush.expect(true.B)
    }
  }
}
