package mycpu.core.frontend

import chisel3._
import chisel3.simulator.EphemeralSimulator._
import mycpu.common._
import org.scalatest.flatspec.AnyFlatSpec

class FetchSpec extends AnyFlatSpec {
  private case class ReadTxn(addr: BigInt, delay: Int)

  private def initAxiInputs(c: Fetch): Unit = {
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
    c.io.axi.r.bits.last.poke(false.B)
  }

  private def serviceReadBus(
      c: Fetch,
      memory: Map[BigInt, BigInt],
      pending: List[ReadTxn],
  ): List[ReadTxn] = {
    initAxiInputs(c)

    val arValid = c.io.axi.ar.valid.peek().litValue == 1
    val nextPending =
      if (arValid) {
        c.io.axi.ar.ready.poke(true.B)
        pending :+ ReadTxn(c.io.axi.ar.bits.addr.peek().litValue, delay = 1)
      } else pending

    nextPending match {
      case ReadTxn(addr, 0) :: tail =>
        c.io.axi.r.valid.poke(true.B)
        c.io.axi.r.bits.id.poke(0.U)
        c.io.axi.r.bits.data.poke(memory.getOrElse(addr, BigInt(0)).U)
        c.io.axi.r.bits.resp.poke(0.U)
        c.io.axi.r.bits.last.poke(true.B)
        if (c.io.axi.r.ready.peek().litValue == 1) tail else nextPending
      case head :: tail =>
        head.copy(delay = head.delay - 1) :: tail
      case Nil =>
        Nil
    }
  }

  "Fetch" should "issue one AXI read and emit a FetchPacket for the start PC" in {
    simulate(new Fetch) { c =>
      val memory = Map(
        BigInt(START_ADDR) -> BigInt("00112233", 16),
      )

      c.reset.poke(true.B)
      c.io.ctrl.stall.poke(false.B)
      c.io.ctrl.redirect.valid.poke(false.B)
      c.io.ctrl.redirect.bits.poke(0.U)
      c.io.out.ready.poke(true.B)
      initAxiInputs(c)
      c.clock.step()

      c.reset.poke(false.B)

      var pending: List[ReadTxn] = Nil
      var cycles = 0
      while (c.io.out.valid.peek().litValue == 0 && cycles < 20) {
        pending = serviceReadBus(c, memory, pending)
        c.clock.step()
        cycles += 1
      }

      c.io.out.valid.expect(true.B)
      c.io.out.bits.pc.expect(START_ADDR.U)
      c.io.out.bits.inst.expect("h00112233".U)
      c.io.out.bits.isException.expect(false.B)
    }
  }

  it should "refetch from next_pc after pc_update_en is asserted" in {
    simulate(new Fetch) { c =>
      val firstPc = BigInt(START_ADDR)
      val secondPc = BigInt(START_ADDR + 0x20)
      val memory = Map(
        firstPc -> BigInt("89abcdef", 16),
        secondPc -> BigInt("13572468", 16),
      )

      c.reset.poke(true.B)
      c.io.ctrl.stall.poke(false.B)
      c.io.ctrl.redirect.valid.poke(false.B)
      c.io.ctrl.redirect.bits.poke(0.U)
      c.io.out.ready.poke(true.B)
      initAxiInputs(c)
      c.clock.step()
      c.reset.poke(false.B)

      var pending: List[ReadTxn] = Nil
      var cycles = 0
      while (c.io.out.valid.peek().litValue == 0 && cycles < 20) {
        pending = serviceReadBus(c, memory, pending)
        c.clock.step()
        cycles += 1
      }

      c.io.out.valid.expect(true.B)
      c.io.out.bits.pc.expect(firstPc.U)
      c.io.out.bits.inst.expect("h89abcdef".U)

      c.clock.step()

      c.io.ctrl.redirect.bits.poke(secondPc.U)
      c.io.ctrl.redirect.valid.poke(true.B)
      pending = serviceReadBus(c, memory, pending)
      c.clock.step()
      c.io.ctrl.redirect.valid.poke(false.B)

      cycles = 0
      while (c.io.out.valid.peek().litValue == 0 && cycles < 20) {
        pending = serviceReadBus(c, memory, pending)
        c.clock.step()
        cycles += 1
      }

      c.io.out.valid.expect(true.B)
      c.io.out.bits.pc.expect(secondPc.U)
      c.io.out.bits.inst.expect("h13572468".U)
    }
  }
}
