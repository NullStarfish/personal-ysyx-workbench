package mycpu.core.components

import chisel3._
import chisel3.simulator.EphemeralSimulator._
import org.scalatest.flatspec.AnyFlatSpec

class HazardUnitSpec extends AnyFlatSpec {
  private def init(c: HazardUnit): Unit = {
    c.io.decode.rs1.valid.poke(false.B)
    c.io.decode.rs1.bits.poke(0.U)
    c.io.decode.rs2.valid.poke(false.B)
    c.io.decode.rs2.bits.poke(0.U)

    c.io.idExLoad.valid.poke(false.B)
    c.io.idExLoad.rd.poke(0.U)
    c.io.lsuLoad.valid.poke(false.B)
    c.io.lsuLoad.rd.poke(0.U)
    c.io.lsuToMemWbFire.poke(false.B)
  }

  "HazardUnit" should "stall when ID/EX load blocks a dependent decode instruction" in {
    simulate(new HazardUnit) { c =>
      init(c)
      c.io.decode.rs1.valid.poke(true.B)
      c.io.decode.rs1.bits.poke(1.U)
      c.io.idExLoad.valid.poke(true.B)
      c.io.idExLoad.rd.poke(1.U)

      c.io.loadUseStall.expect(true.B)
    }
  }

  it should "stall when LSU has a pending dependent load" in {
    simulate(new HazardUnit) { c =>
      init(c)
      c.io.decode.rs2.valid.poke(true.B)
      c.io.decode.rs2.bits.poke(4.U)
      c.io.lsuLoad.valid.poke(true.B)
      c.io.lsuLoad.rd.poke(4.U)

      c.io.loadUseStall.expect(true.B)
    }
  }

  it should "release an LSU dependency when the result fires into MEM/WB" in {
    simulate(new HazardUnit) { c =>
      init(c)
      c.io.decode.rs2.valid.poke(true.B)
      c.io.decode.rs2.bits.poke(4.U)
      c.io.lsuLoad.valid.poke(true.B)
      c.io.lsuLoad.rd.poke(4.U)
      c.io.lsuToMemWbFire.poke(true.B)

      c.io.loadUseStall.expect(false.B)
    }
  }

  it should "ignore rd zero and unused operands" in {
    simulate(new HazardUnit) { c =>
      init(c)
      c.io.decode.rs1.valid.poke(false.B)
      c.io.decode.rs1.bits.poke(1.U)
      c.io.idExLoad.valid.poke(true.B)
      c.io.idExLoad.rd.poke(1.U)
      c.io.loadUseStall.expect(false.B)

      c.io.decode.rs1.valid.poke(true.B)
      c.io.idExLoad.rd.poke(0.U)
      c.io.loadUseStall.expect(false.B)
    }
  }
}
