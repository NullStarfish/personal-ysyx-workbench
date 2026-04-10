package mycpu.core

import chisel3._
import chisel3.simulator.EphemeralSimulator._
import mycpu.common._
import org.scalatest.flatspec.AnyFlatSpec

class CoreHazardSpec extends AnyFlatSpec with CoreProgramSupport {

  "Core" should "stall on load-use hazards until load data is available" in {
    simulate(new Core) { c =>
      val memory = Map[BigInt, BigInt](
        BigInt(START_ADDR) -> BigInt("10002083", 16),      // lw x1, 256(x0)
        BigInt(START_ADDR + 4) -> BigInt("00108113", 16),  // addi x2, x1, 1
        BigInt(START_ADDR + 8) -> BigInt("00100073", 16),  // ebreak
        BigInt(0x100) -> BigInt(0x10)
      )

      c.reset.poke(true.B)
      initBus(c)
      c.clock.step()
      c.reset.poke(false.B)

      var pending: List[ReadTxn] = Nil
      stepUntilRetireCount(c, targetRetires = 3, maxCycles = 40, {
        pending = serviceReadBus(c, memory, pending)
      })

      c.io.debug_regs(1).expect("h10".U)
      c.io.debug_regs(2).expect("h11".U)
    }
  }

  it should "flush wrong-path instructions after jal redirect" in {
    simulate(new Core) { c =>
      val memory = Map[BigInt, BigInt](
        BigInt(START_ADDR) -> BigInt("00100093", 16),      // addi x1, x0, 1
        BigInt(START_ADDR + 4) -> BigInt("0080006f", 16),  // jal x0, 8
        BigInt(START_ADDR + 8) -> BigInt("00100113", 16),  // addi x2, x0, 1 (wrong path)
        BigInt(START_ADDR + 12) -> BigInt("00200193", 16), // addi x3, x0, 2 (target)
        BigInt(START_ADDR + 16) -> BigInt("00100073", 16)  // ebreak
      )

      c.reset.poke(true.B)
      initBus(c)
      c.clock.step()
      c.reset.poke(false.B)

      var pending: List[ReadTxn] = Nil
      stepUntilRetireCount(c, targetRetires = 4, maxCycles = 40, {
        pending = serviceReadBus(c, memory, pending)
      })

      c.io.debug_regs(1).expect(1.U)
      c.io.debug_regs(2).expect(0.U)
      c.io.debug_regs(3).expect(2.U)
    }
  }

}
