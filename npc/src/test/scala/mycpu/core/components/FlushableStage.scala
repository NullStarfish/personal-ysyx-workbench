package mycpu.core.components

import chisel3._
import chisel3.simulator.EphemeralSimulator._
import org.scalatest.flatspec.AnyFlatSpec

class FlushableStageSim extends AnyFlatSpec {
  private def init(c: FlushableStage[UInt]): Unit = {
    c.io.enq.valid.poke(false.B)
    c.io.enq.bits.poke(0.U)
    c.io.deq.ready.poke(false.B)
    c.io.flush.poke(false.B)
    c.io.stall.poke(false.B)
  }

  private def resetDut(c: FlushableStage[UInt]): Unit = {
    init(c)
    c.reset.poke(true.B)
    c.clock.step()
    c.reset.poke(false.B)
  }

  "FlushableStage" should "accept one element and present it on deq" in {
    simulate(new FlushableStage(UInt(8.W))) { c =>
      resetDut(c)

      c.io.enq.valid.poke(true.B)
      c.io.enq.bits.poke("h2a".U)
      c.io.deq.ready.poke(false.B)
      c.io.enq.ready.expect(true.B)
      c.clock.step()

      c.io.enq.valid.poke(false.B)
      c.io.deq.valid.expect(true.B)
      c.io.deq.bits.expect("h2a".U)

      c.io.deq.ready.poke(true.B)
      c.clock.step()

      c.io.deq.valid.expect(false.B)
    }
  }

  it should "apply backpressure when the single entry is full" in {
    simulate(new FlushableStage(UInt(8.W))) { c =>
      resetDut(c)

      c.io.enq.valid.poke(true.B)
      c.io.enq.bits.poke("h11".U)
      c.io.deq.ready.poke(false.B)
      c.clock.step()

      c.io.enq.bits.poke("h22".U)
      c.io.enq.ready.expect(false.B)
      c.io.deq.valid.expect(true.B)
      c.io.deq.bits.expect("h11".U)
    }
  }

  it should "flush a queued element" in {
    simulate(new FlushableStage(UInt(8.W))) { c =>
      resetDut(c)

      c.io.enq.valid.poke(true.B)
      c.io.enq.bits.poke("h55".U)
      c.clock.step()

      c.io.enq.valid.poke(false.B)
      c.io.deq.valid.expect(true.B)
      c.io.flush.poke(true.B)
      c.clock.step()

      c.io.flush.poke(false.B)
      c.io.deq.valid.expect(false.B)
      c.io.enq.ready.expect(true.B)
    }
  }

  it should "block upstream while stalled without hiding queued data" in {
    simulate(new FlushableStage(UInt(8.W))) { c =>
      resetDut(c)

      c.io.enq.valid.poke(true.B)
      c.io.enq.bits.poke("h7c".U)
      c.clock.step()

      c.io.enq.valid.poke(false.B)
      c.io.stall.poke(true.B)
      c.io.deq.ready.poke(false.B)

      c.io.enq.ready.expect(false.B)
      c.io.deq.valid.expect(true.B)
      c.io.deq.bits.expect("h7c".U)
      c.clock.step()

      c.io.stall.poke(false.B)
      c.io.deq.ready.poke(false.B)
      c.io.deq.valid.expect(true.B)
      c.io.deq.bits.expect("h7c".U)
    }
  }

  it should "allow downstream to consume a queued element while stalled" in {
    simulate(new FlushableStage(UInt(8.W))) { c =>
      resetDut(c)

      c.io.enq.valid.poke(true.B)
      c.io.enq.bits.poke("h3d".U)
      c.clock.step()

      c.io.enq.valid.poke(false.B)
      c.io.stall.poke(true.B)
      c.io.deq.ready.poke(true.B)

      c.io.enq.ready.expect(false.B)
      c.io.deq.valid.expect(true.B)
      c.io.deq.bits.expect("h3d".U)
      c.clock.step()

      c.io.stall.poke(false.B)
      c.io.deq.ready.poke(false.B)
      c.io.deq.valid.expect(false.B)
    }
  }

  it should "give flush priority over a stalled queued element" in {
    simulate(new FlushableStage(UInt(8.W))) { c =>
      resetDut(c)

      c.io.enq.valid.poke(true.B)
      c.io.enq.bits.poke("haa".U)
      c.clock.step()

      c.io.enq.valid.poke(false.B)
      c.io.stall.poke(true.B)
      c.io.flush.poke(true.B)
      c.clock.step()

      c.io.stall.poke(false.B)
      c.io.flush.poke(false.B)
      c.io.deq.valid.expect(false.B)
    }
  }
}
