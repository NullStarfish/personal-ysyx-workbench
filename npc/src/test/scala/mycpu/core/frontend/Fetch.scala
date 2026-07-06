package mycpu.core.frontend

import chisel3._
import chisel3.simulator.EphemeralSimulator._
import mycpu.common._
import mycpu.memory.FetchResp
import org.scalatest.flatspec.AnyFlatSpec

class FetchSim extends AnyFlatSpec {
  private val maxWait = 20

  private def init(c: Fetch): Unit = {
    c.io.instReq.ready.poke(false.B)
    c.io.instResp.valid.poke(false.B)
    c.io.instResp.bits.inst.poke(0.U)
    c.io.instResp.bits.hit.poke(false.B)
    c.io.out.ready.poke(false.B)
    c.io.redirect.valid.poke(false.B)
    c.io.redirect.bits.poke(0.U)
  }

  private def resetDut(c: Fetch): Unit = {
    init(c)
    c.reset.poke(true.B)
    c.clock.step()
    c.reset.poke(false.B)
  }

  private def acceptFetch(c: Fetch, expectedPc: BigInt): Unit = {
    var cycles = 0
    c.io.instReq.ready.poke(false.B)
    while (c.io.instReq.valid.peek().litValue == 0 && cycles < maxWait) {
      c.clock.step()
      cycles += 1
    }
    assert(cycles < maxWait, s"Fetch did not present request for 0x${expectedPc.toString(16)}")
    c.io.instReq.bits.expect(expectedPc.U)
    c.io.instReq.ready.poke(true.B)
    c.clock.step()
    c.io.instReq.ready.poke(false.B)
  }

  private def sendReply(c: Fetch, inst: BigInt, icacheHit: Boolean = false): Unit = {
    var cycles = 0
    c.io.instResp.valid.poke(true.B)
    c.io.instResp.bits.inst.poke(inst.U)
    c.io.instResp.bits.hit.poke(icacheHit.B)
    while (c.io.instResp.ready.peek().litValue == 0 && cycles < maxWait) {
      c.clock.step()
      cycles += 1
    }
    assert(cycles < maxWait, s"Fetch did not accept reply 0x${inst.toString(16)}")
    c.clock.step()
    c.io.instResp.valid.poke(false.B)
  }

  private def expectOut(c: Fetch, expectedPc: BigInt, expectedInst: BigInt, expectedHit: Boolean = false): Unit = {
    var cycles = 0
    c.io.out.ready.poke(true.B)
    while (c.io.out.valid.peek().litValue == 0 && cycles < maxWait) {
      c.clock.step()
      cycles += 1
    }
    assert(cycles < maxWait, s"Fetch did not emit packet for 0x${expectedPc.toString(16)}")
    c.io.out.bits.pc.expect(expectedPc.U)
    c.io.out.bits.inst.expect(expectedInst.U)
    c.io.out.bits.icacheHit.expect(expectedHit.B)
    c.io.out.bits.isException.expect(false.B)
    c.clock.step()
    c.io.out.ready.poke(false.B)
  }

  "Fetch" should "present reset PC until the request is accepted" in {
    simulate(new Fetch) { c =>
      resetDut(c)

      c.io.instReq.ready.poke(false.B)
      c.clock.step(3)
      acceptFetch(c, START_ADDR)
    }
  }

  it should "emit returned instructions with the PCs of accepted requests" in {
    simulate(new Fetch) { c =>
      resetDut(c)

      acceptFetch(c, START_ADDR)
      sendReply(c, BigInt("00112233", 16), icacheHit = true)
      expectOut(c, START_ADDR, BigInt("00112233", 16), expectedHit = true)

      acceptFetch(c, START_ADDR + 4)
      sendReply(c, BigInt("00000013", 16))
      expectOut(c, START_ADDR + 4, BigInt("00000013", 16))
    }
  }

  it should "redirect the next request and suppress the stale response" in {
    simulate(new Fetch) { c =>
      val target = START_ADDR + 0x40

      resetDut(c)

      acceptFetch(c, START_ADDR)

      c.io.redirect.valid.poke(true.B)
      c.io.redirect.bits.poke(target.U)
      c.io.instResp.valid.poke(true.B)
      c.io.instResp.bits.inst.poke("hdeadbeef".U)
      c.io.out.ready.poke(true.B)
      c.clock.step()

      c.io.redirect.valid.poke(false.B)
      c.io.instResp.valid.poke(true.B)
      c.io.instResp.bits.inst.poke("hdeadbeef".U)
      c.io.out.valid.expect(false.B)
      c.clock.step()
      c.io.instResp.valid.poke(false.B)

      acceptFetch(c, target)
      sendReply(c, BigInt("00112233", 16))
      expectOut(c, target, BigInt("00112233", 16))
    }
  }

  it should "leave jump redirection to the backend" in {
    simulate(new Fetch) { c =>
      val jalImm = 0x20
      val jal =
        (BigInt((jalImm >> 20) & 0x1) << 31) |
          (BigInt((jalImm >> 1) & 0x3ff) << 21) |
          (BigInt((jalImm >> 11) & 0x1) << 20) |
          (BigInt((jalImm >> 12) & 0xff) << 12) |
          BigInt("1101111", 2)

      resetDut(c)

      acceptFetch(c, START_ADDR)
      sendReply(c, jal)
      expectOut(c, START_ADDR, jal)

      acceptFetch(c, START_ADDR + 4)
    }
  }
}
