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
    c.io.cpuReq.bits.poke(0.U)
    c.io.cpuReply.ready.poke(false.B)
    c.io.mem.a.ready.poke(false.B)
    c.io.mem.r.valid.poke(false.B)
    c.io.mem.r.bits.data.poke(0.U)
    c.io.mem.r.bits.resp.poke(0.U)
    c.io.mem.r.bits.last.poke(false.B)
    c.io.mem.r.bits.id.poke(0.U)
    c.io.fencei.poke(false.B)
    c.io.prefetch.valid.poke(false.B)
    c.io.prefetch.bits.poke(0.U)
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
    waitFor(c, c.io.mem.a.valid.peek().litValue == 1, "memory request")
    c.io.mem.a.bits.addr.expect(addr.U)
    c.io.mem.a.bits.size.expect(2.U)
    c.io.mem.a.bits.len.expect((params.wordsPerLine - 1).U)
    c.io.mem.a.bits.write.expect(false.B)
  }

  private def expectReply(c: ICache, inst: BigInt, hit: Boolean): Unit = {
    waitFor(c, c.io.cpuReply.valid.peek().litValue == 1, "CPU reply")
    c.io.cpuReply.bits.inst.expect(inst.U)
    c.io.cpuReply.bits.hit.expect(hit.B)
    c.clock.step()
  }

  "ICache" should "refill a missing line and hit on later accesses" in {
    simulate(new ICache(params)) { c =>
      init(c)

      c.io.cpuReply.ready.poke(true.B)
      c.io.cpuReq.valid.poke(true.B)
      c.io.cpuReq.bits.poke("h104".U)
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

      expectMemReq(c, 0x100)
      c.io.mem.a.ready.poke(true.B)
      c.clock.step()
      c.io.mem.a.ready.poke(false.B)

      for ((word, index) <- refillWords.zipWithIndex) {
        c.io.mem.r.valid.poke(true.B)
        c.io.mem.r.bits.data.poke(BigInt(word.drop(1), 16).U)
        c.io.mem.r.bits.last.poke((index == refillWords.size - 1).B)
        c.clock.step()
        c.io.mem.r.valid.poke(false.B)
      }

      expectReply(c, BigInt("00200113", 16), hit = false)

      c.io.cpuReq.valid.poke(true.B)
      c.io.cpuReq.bits.poke("h108".U)
      c.io.cpuReq.ready.expect(true.B)
      c.io.cpuReply.valid.expect(false.B)
      c.clock.step()
      c.io.cpuReq.valid.poke(false.B)
      expectReply(c, BigInt("00300193", 16), hit = true)
    }
  }

  it should "not accept another CPU request while a miss is refilling" in {
    simulate(new ICache(params)) { c =>
      init(c)

      c.io.cpuReply.ready.poke(true.B)
      c.io.cpuReq.valid.poke(true.B)
      c.io.cpuReq.bits.poke("h104".U)
      c.io.cpuReq.ready.expect(true.B)
      c.clock.step()

      c.io.cpuReq.bits.poke("h108".U)
      c.io.cpuReq.ready.expect(false.B)
      c.io.cpuReq.valid.poke(false.B)
      c.clock.step()

      val refillWords = Seq(
        "h00100093",
        "h00200113",
        "h00300193",
        "h00400213",
      )

      expectMemReq(c, 0x100)
      c.io.mem.a.ready.poke(true.B)
      c.clock.step()
      c.io.mem.a.ready.poke(false.B)

      for ((word, index) <- refillWords.zipWithIndex) {
        c.io.mem.r.valid.poke(true.B)
        c.io.mem.r.bits.data.poke(BigInt(word.drop(1), 16).U)
        c.io.mem.r.bits.last.poke((index == refillWords.size - 1).B)
        c.clock.step()
        c.io.mem.r.valid.poke(false.B)
      }

      expectReply(c, BigInt("00200113", 16), hit = false)

      c.io.cpuReq.valid.poke(true.B)
      c.io.cpuReq.bits.poke("h108".U)
      c.io.cpuReq.ready.expect(true.B)
      c.io.cpuReply.valid.expect(false.B)
      c.clock.step()
      c.io.cpuReq.valid.poke(false.B)
      expectReply(c, BigInt("00300193", 16), hit = true)
    }
  }

  it should "not accept a CPU request during fence.i invalidation" in {
    simulate(new ICache(params)) { c =>
      init(c)

      c.io.cpuReq.valid.poke(true.B)
      c.io.cpuReq.bits.poke("h104".U)
      c.io.fencei.poke(true.B)
      c.io.cpuReq.ready.expect(false.B)
      c.clock.step()

      c.io.fencei.poke(false.B)
      c.io.cpuReq.ready.expect(true.B)
    }
  }
}
