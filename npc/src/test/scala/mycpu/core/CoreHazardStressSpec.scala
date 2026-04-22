package mycpu.core

import chisel3._
import chisel3.simulator.EphemeralSimulator._
import mycpu.common._
import org.scalatest.flatspec.AnyFlatSpec

class CoreHazardStressSpec extends AnyFlatSpec with CoreProgramSupport {
  private def serviceReadBusWithDelay(
      c: Core,
      memory: Map[BigInt, BigInt],
      pending: List[ReadTxn],
      responseDelay: Int,
  ): List[ReadTxn] = {
    initBus(c)

    val arValid = c.io.master.ar.valid.peek().litValue == 1
    val nextPending =
      if (arValid) {
        c.io.master.ar.ready.poke(true.B)
        pending :+ ReadTxn(c.io.master.ar.bits.addr.peek().litValue, delay = responseDelay)
      } else pending

    nextPending match {
      case ReadTxn(addr, 0) :: tail =>
        c.io.master.r.valid.poke(true.B)
        c.io.master.r.bits.id.poke(0.U)
        c.io.master.r.bits.data.poke(memory.getOrElse(addr, BigInt(0)).U)
        c.io.master.r.bits.resp.poke(0.U)
        c.io.master.r.bits.last.poke(true.B)
        if (c.io.master.r.ready.peek().litValue == 1) tail else nextPending
      case head :: tail =>
        head.copy(delay = head.delay - 1) :: tail
      case Nil =>
        Nil
    }
  }

  "Core" should "survive back-to-back jal flushes without retiring wrong-path instructions" in {
    simulate(new Core) { c =>
      val memory = Map[BigInt, BigInt](
        BigInt(START_ADDR) -> BigInt("00100093", 16),      // addi x1, x0, 1
        BigInt(START_ADDR + 4) -> BigInt("0080006f", 16),  // jal x0, 8
        BigInt(START_ADDR + 8) -> BigInt("00100113", 16),  // addi x2, x0, 1 (wrong path)
        BigInt(START_ADDR + 12) -> BigInt("00108093", 16), // addi x1, x1, 1
        BigInt(START_ADDR + 16) -> BigInt("0080006f", 16), // jal x0, 8
        BigInt(START_ADDR + 20) -> BigInt("00100193", 16), // addi x3, x0, 1 (wrong path)
        BigInt(START_ADDR + 24) -> BigInt("00108213", 16), // addi x4, x1, 1
        BigInt(START_ADDR + 28) -> BigInt("00100073", 16), // ebreak
      )

      c.reset.poke(true.B)
      initBus(c)
      c.clock.step()
      c.reset.poke(false.B)

      var pending: List[ReadTxn] = Nil
      stepUntilRetireCount(c, targetRetires = 6, maxCycles = 80, {
        pending = serviceReadBus(c, memory, pending)
      })

      c.io.debug_regs(1).expect(2.U)
      c.io.debug_regs(2).expect(0.U)
      c.io.debug_regs(3).expect(0.U)
      c.io.debug_regs(4).expect(3.U)
    }
  }

  it should "stall a load-fed branch until data arrives and still flush the wrong path" in {
    simulate(new Core) { c =>
      val memory = Map[BigInt, BigInt](
        BigInt(START_ADDR) -> BigInt("10002083", 16),      // lw x1, 256(x0)
        BigInt(START_ADDR + 4) -> BigInt("00008463", 16),  // beq x1, x0, 8
        BigInt(START_ADDR + 8) -> BigInt("00100113", 16),  // addi x2, x0, 1 (wrong path)
        BigInt(START_ADDR + 12) -> BigInt("00208193", 16), // addi x3, x1, 2
        BigInt(START_ADDR + 16) -> BigInt("00100073", 16), // ebreak
        BigInt(0x100) -> BigInt(0x0)
      )

      c.reset.poke(true.B)
      initBus(c)
      c.clock.step()
      c.reset.poke(false.B)

      var pending: List[ReadTxn] = Nil
      stepUntilRetireCount(c, targetRetires = 4, maxCycles = 80, {
        pending = serviceReadBus(c, memory, pending)
      })

      c.io.debug_regs(1).expect(0.U)
      c.io.debug_regs(2).expect(0.U)
      c.io.debug_regs(3).expect(2.U)
    }
  }

  it should "preserve a forwarded RAW result across a jal flush boundary" in {
    simulate(new Core) { c =>
      val memory = Map[BigInt, BigInt](
        BigInt(START_ADDR) -> BigInt("00500093", 16),      // addi x1, x0, 5
        BigInt(START_ADDR + 4) -> BigInt("00108113", 16),  // addi x2, x1, 1
        BigInt(START_ADDR + 8) -> BigInt("0080006f", 16),  // jal x0, 8
        BigInt(START_ADDR + 12) -> BigInt("00110193", 16), // addi x3, x2, 1 (wrong path)
        BigInt(START_ADDR + 16) -> BigInt("00210213", 16), // addi x4, x2, 2
        BigInt(START_ADDR + 20) -> BigInt("00100073", 16), // ebreak
      )

      c.reset.poke(true.B)
      initBus(c)
      c.clock.step()
      c.reset.poke(false.B)

      var pending: List[ReadTxn] = Nil
      stepUntilRetireCount(c, targetRetires = 5, maxCycles = 80, {
        pending = serviceReadBus(c, memory, pending)
      })

      c.io.debug_regs(1).expect(5.U)
      c.io.debug_regs(2).expect(6.U)
      c.io.debug_regs(3).expect(0.U)
      c.io.debug_regs(4).expect(8.U)
    }
  }

  it should "retire the jal target instruction even when fetch responses are delayed" in {
    simulate(new Core) { c =>
      val memory = Map[BigInt, BigInt](
        BigInt(START_ADDR) -> BigInt("00000413", 16),      // mv s0, zero
        BigInt(START_ADDR + 4) -> BigInt("df002117", 16),  // auipc sp, 0xdf002
        BigInt(START_ADDR + 8) -> BigInt("ffc10113", 16),  // addi sp, sp, -4
        BigInt(START_ADDR + 12) -> BigInt("00c000ef", 16), // jal x1, 12 -> 0x18
        BigInt(START_ADDR + 16) -> BigInt("00100293", 16), // addi x5, x0, 1 (wrong path)
        BigInt(START_ADDR + 20) -> BigInt("00100313", 16), // addi x6, x0, 1 (wrong path)
        BigInt(START_ADDR + 24) -> BigInt("ffc10113", 16), // addi sp, sp, -4 (target)
        BigInt(START_ADDR + 28) -> BigInt("00100073", 16), // ebreak
      )

      c.reset.poke(true.B)
      initBus(c)
      c.clock.step()
      c.reset.poke(false.B)

      var pending: List[ReadTxn] = Nil
      stepUntilRetireCount(c, targetRetires = 6, maxCycles = 120, {
        pending = serviceReadBusWithDelay(c, memory, pending, responseDelay = 3)
      })

      val auipcPc = BigInt(START_ADDR + 4)
      val auipcImm20 = BigInt("df002", 16)
      val signedAuipcImm = if (auipcImm20.testBit(19)) auipcImm20 - (BigInt(1) << 20) else auipcImm20
      val expectedSp = (auipcPc + (signedAuipcImm << 12) - 8) & BigInt("ffffffff", 16)

      c.io.trace.retireCount.expect(6.U)
      c.io.debug_regs(1).expect((START_ADDR + 16).U) // jal link
      c.io.debug_regs(2).expect(expectedSp.U) // sp updated by auipc/addi/target addi
      c.io.debug_regs(5).expect(0.U)                      // wrong path killed
      c.io.debug_regs(6).expect(0.U)                      // wrong path killed
      c.io.debug_regs(8).expect(0.U)                 // mv s0, zero
    }
  }

  it should "retire the uart polling loop once when lsr bit 5 is set" in {
    simulate(new Core) { c =>
      val uartBase = BigInt("10000000", 16)
      val uartLsrAddr = uartBase + 5
      val program = Map[BigInt, BigInt](
        BigInt(START_ADDR) -> BigInt("10000737", 16),      // lui a4, 0x10000
        BigInt(START_ADDR + 4) -> BigInt("00570713", 16),  // addi a4, a4, 5
        BigInt(START_ADDR + 8) -> BigInt("00074783", 16),  // lbu a5, 0(a4)
        BigInt(START_ADDR + 12) -> BigInt("0207f793", 16), // andi a5, a5, 0x20
        BigInt(START_ADDR + 16) -> BigInt("fe078ce3", 16), // beqz a5, -8
        BigInt(START_ADDR + 20) -> BigInt("00100073", 16), // ebreak
        uartLsrAddr -> BigInt("60606060", 16),
      )

      c.reset.poke(true.B)
      initBus(c)
      c.clock.step()
      c.reset.poke(false.B)

      var pending: List[ReadTxn] = Nil
      val retired = stepUntilRetireCountCollect(c, targetRetires = 6, maxCycles = 120, {
        pending = serviceReadBus(c, program, pending)
      })

      val andiPc = BigInt(START_ADDR + 12)
      val andiInst = BigInt("0207f793", 16)
      val andiRetires = retired.count { case (pc, inst, _) => pc == andiPc && inst == andiInst }
      val lbuRetires = retired.count { case (pc, inst, _) => pc == BigInt(START_ADDR + 8) && inst == BigInt("00074783", 16) }

      assert(retired.nonEmpty, "expected at least one retired instruction")
      assert(
        retired.last._1 == BigInt(START_ADDR + 20),
        s"expected loop to exit and retire ebreak, got last retire pc=0x${retired.last._1.toString(16)}"
      )
      assert(lbuRetires == 1, s"expected one lbu retire, got $lbuRetires: $retired")
      assert(andiRetires == 1, s"expected one andi retire, got $andiRetires: $retired")
      c.io.debug_regs(15).expect("h20".U)
    }
  }
}
