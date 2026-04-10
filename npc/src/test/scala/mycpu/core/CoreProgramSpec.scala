package mycpu.core

import chisel3._
import chisel3.simulator.EphemeralSimulator._
import mycpu.common._
import org.scalatest.flatspec.AnyFlatSpec

import scala.collection.mutable

class CoreProgramSpec extends AnyFlatSpec with CoreProgramSupport {
  "Core" should "flush wrong-path instructions after a taken branch" in {
    simulate(new Core) { c =>
      val memory = Map[BigInt, BigInt](
        BigInt(START_ADDR) -> BigInt("00100093", 16),      // addi x1, x0, 1
        BigInt(START_ADDR + 4) -> BigInt("00100113", 16),  // addi x2, x0, 1
        BigInt(START_ADDR + 8) -> BigInt("00208463", 16),  // beq x1, x2, 8
        BigInt(START_ADDR + 12) -> BigInt("00100193", 16), // addi x3, x0, 1 (wrong path)
        BigInt(START_ADDR + 16) -> BigInt("00200213", 16), // addi x4, x0, 2 (target)
        BigInt(START_ADDR + 20) -> BigInt("00100073", 16)  // ebreak
      )

      c.reset.poke(true.B)
      initBus(c)
      c.clock.step()
      c.reset.poke(false.B)

      var pending: Option[ReadTxn] = None
      var cycles = 0
      while (cycles < 50) {
        pending = serviceReadBus(c, memory, pending)
        c.clock.step()
        cycles += 1
      }

      c.io.debug_regs(1).expect(1.U)
      c.io.debug_regs(2).expect(1.U)
      c.io.debug_regs(3).expect(0.U)
      c.io.debug_regs(4).expect(2.U)
    }
  }

  it should "handle forwarding, load-use stall, and redirect in one short program" in {
    simulate(new Core) { c =>
      val memory = Map[BigInt, BigInt](
        BigInt(START_ADDR) -> BigInt("00500093", 16),      // addi x1, x0, 5
        BigInt(START_ADDR + 4) -> BigInt("00108133", 16),  // add x2, x1, x1
        BigInt(START_ADDR + 8) -> BigInt("10002183", 16),  // lw x3, 256(x0)
        BigInt(START_ADDR + 12) -> BigInt("00118213", 16), // addi x4, x3, 1
        BigInt(START_ADDR + 16) -> BigInt("00020463", 16), // beq x4, x0, 8
        BigInt(START_ADDR + 20) -> BigInt("00100293", 16), // addi x5, x0, 1
        BigInt(START_ADDR + 24) -> BigInt("0080006f", 16), // jal x0, 8
        BigInt(START_ADDR + 28) -> BigInt("00100313", 16), // addi x6, x0, 1 (wrong path)
        BigInt(START_ADDR + 32) -> BigInt("00200393", 16), // addi x7, x0, 2 (target)
        BigInt(START_ADDR + 36) -> BigInt("00100073", 16), // ebreak
        BigInt(0x100) -> BigInt(0x20)
      )

      c.reset.poke(true.B)
      initBus(c)
      c.clock.step()
      c.reset.poke(false.B)

      var pending: Option[ReadTxn] = None
      var cycles = 0
      while (cycles < 80) {
        pending = serviceReadBus(c, memory, pending)
        c.clock.step()
        cycles += 1
      }

      c.io.debug_regs(1).expect(5.U)
      c.io.debug_regs(2).expect(10.U)
      c.io.debug_regs(3).expect("h20".U)
      c.io.debug_regs(4).expect("h21".U)
      c.io.debug_regs(5).expect(1.U)
      c.io.debug_regs(6).expect(0.U)
      c.io.debug_regs(7).expect(2.U)
    }
  }

  it should "handle store then load then use through the full pipeline" in {
    simulate(new Core) { c =>
      val memory = mutable.Map[BigInt, BigInt](
        BigInt(START_ADDR) -> BigInt("05500093", 16),      // addi x1, x0, 0x55
        BigInt(START_ADDR + 4) -> BigInt("10102023", 16),  // sw x1, 256(x0)
        BigInt(START_ADDR + 8) -> BigInt("10002103", 16),  // lw x2, 256(x0)
        BigInt(START_ADDR + 12) -> BigInt("00110193", 16), // addi x3, x2, 1
        BigInt(START_ADDR + 16) -> BigInt("00100073", 16), // ebreak
        BigInt(0x100) -> BigInt(0)
      )

      c.reset.poke(true.B)
      initBus(c)
      c.clock.step()
      c.reset.poke(false.B)

      var pendingRead: Option[ReadTxn] = None
      var pendingWriteResp: Option[WriteResp] = None
      var cycles = 0
      while (cycles < 80) {
        val next = serviceBus(c, memory, pendingRead, pendingWriteResp)
        pendingRead = next._1
        pendingWriteResp = next._2
        c.clock.step()
        cycles += 1
      }

      c.io.debug_regs(1).expect("h55".U)
      c.io.debug_regs(2).expect("h55".U)
      c.io.debug_regs(3).expect("h56".U)
      assert(memory(BigInt(0x100)) == BigInt(0x55))
    }
  }
}
