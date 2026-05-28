package mycpu.cache

import chisel3._
import chisel3.simulator.EphemeralSimulator._
import org.scalatest.flatspec.AnyFlatSpec

class ICacheSpec extends AnyFlatSpec {
  private val params = CacheParams(lineBytes = 16, sets = 4, ways = 1)
  private val maxWaitCycles = 32

  private def init(c: ICache): Unit = {
    c.reset.poke(true.B)
    c.io.cpuReq.valid.poke(false.B)
    c.io.cpuReq.bits.pc.poke(0.U)
    c.io.cpuReply.ready.poke(false.B)
    c.io.memReq.ready.poke(false.B)
    c.io.memReply.valid.poke(false.B)
    c.io.memReply.bits.data.poke(0.U)
    c.io.redirect.valid.poke(false.B)
    c.io.redirect.bits.poke(0.U)
    c.io.prefetch.valid.poke(false.B)
    c.io.prefetch.bits.addr.poke(0.U)
    c.clock.step()
    c.reset.poke(false.B)
  }

  private def waitFor(c: ICache, condition: => Boolean, description: String): Unit = {
    var cycles = 0
    while (!condition && cycles < maxWaitCycles) {
      c.clock.step()
      cycles += 1
    }
    assert(condition, s"timed out waiting for $description after $maxWaitCycles cycles")
  }

  private def expectMemReq(c: ICache, addr: BigInt): Unit = {
    waitFor(c, c.io.memReq.valid.peek().litValue == 1, "memory request")
    c.io.memReq.bits.addr.expect(addr.U)
  }

  private def expectReply(c: ICache, pc: BigInt, inst: BigInt, hit: Boolean): Unit = {
    waitFor(c, c.io.cpuReply.valid.peek().litValue == 1, "CPU reply")
    c.io.cpuReply.bits.pc.expect(pc.U)
    c.io.cpuReply.bits.inst.expect(inst.U)
    c.io.cpuReply.bits.hit.expect(hit.B)
    c.clock.step()
  }

  "ICache" should "refill a missing line and hit on later accesses" in {
    simulate(new ICache(params)) { c =>
      init(c)

      c.io.cpuReply.ready.poke(true.B)
      c.io.cpuReq.valid.poke(true.B)
      c.io.cpuReq.bits.pc.poke("h104".U)
      c.io.cpuReq.ready.expect(true.B)
      c.clock.step()

      c.io.cpuReq.valid.poke(false.B)
      c.clock.step()

      val refillWords = Seq(
        "h00100093",
        "h00200113",
        "h00300193",
        "h00400213",
      )

      for ((word, beat) <- refillWords.zipWithIndex) {
        expectMemReq(c, 0x100 + beat * 4)
        c.io.memReq.ready.poke(true.B)
        c.clock.step()

        c.io.memReq.ready.poke(false.B)
        c.io.memReply.valid.poke(true.B)
        c.io.memReply.bits.data.poke(BigInt(word.drop(1), 16).U)
        c.clock.step()
        c.io.memReply.valid.poke(false.B)
      }

      expectReply(c, 0x104, BigInt("00200113", 16), hit = false)

      c.io.cpuReq.valid.poke(true.B)
      c.io.cpuReq.bits.pc.poke("h108".U)
      c.io.cpuReq.ready.expect(true.B)
      c.io.cpuReply.valid.expect(false.B)
      c.clock.step()
      c.io.cpuReq.valid.poke(false.B)
      expectReply(c, 0x108, BigInt("00300193", 16), hit = true)
    }
  }

  it should "not accept another CPU request while a miss is refilling" in {
    simulate(new ICache(params)) { c =>
      init(c)

      c.io.cpuReply.ready.poke(true.B)
      c.io.cpuReq.valid.poke(true.B)
      c.io.cpuReq.bits.pc.poke("h104".U)
      c.io.cpuReq.ready.expect(true.B)
      c.clock.step()

      c.io.cpuReq.bits.pc.poke("h108".U)
      c.io.cpuReq.ready.expect(false.B)
      c.io.cpuReq.valid.poke(false.B)
      c.clock.step()

      val refillWords = Seq(
        "h00100093",
        "h00200113",
        "h00300193",
        "h00400213",
      )

      for ((word, beat) <- refillWords.zipWithIndex) {
        expectMemReq(c, 0x100 + beat * 4)
        c.io.memReq.ready.poke(true.B)
        c.clock.step()

        c.io.memReq.ready.poke(false.B)
        c.io.memReply.valid.poke(true.B)
        c.io.memReply.bits.data.poke(BigInt(word.drop(1), 16).U)
        c.clock.step()
        c.io.memReply.valid.poke(false.B)
      }

      expectReply(c, 0x104, BigInt("00200113", 16), hit = false)

      c.io.cpuReq.valid.poke(true.B)
      c.io.cpuReq.bits.pc.poke("h108".U)
      c.io.cpuReq.ready.expect(true.B)
      c.io.cpuReply.valid.expect(false.B)
      c.clock.step()
      c.io.cpuReq.valid.poke(false.B)
      expectReply(c, 0x108, BigInt("00300193", 16), hit = true)
    }
  }

  it should "not accept a CPU request during redirect flush" in {
    simulate(new ICache(params)) { c =>
      init(c)

      c.io.cpuReq.valid.poke(true.B)
      c.io.cpuReq.bits.pc.poke("h104".U)
      c.io.redirect.valid.poke(true.B)
      c.io.redirect.bits.poke("h200".U)
      c.io.cpuReq.ready.expect(false.B)
      c.clock.step()

      c.io.redirect.valid.poke(false.B)
      c.io.cpuReq.ready.expect(true.B)
    }
  }
}
