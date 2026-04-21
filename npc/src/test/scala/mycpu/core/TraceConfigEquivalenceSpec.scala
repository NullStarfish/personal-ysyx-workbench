package mycpu.core

import chisel3._
import chisel3.simulator.EphemeralSimulator._
import mycpu.common._
import org.scalatest.flatspec.AnyFlatSpec

import scala.collection.mutable

class TraceConfigEquivalenceSpec extends AnyFlatSpec with CoreProgramSupport {
  private case class FinalState(
      regs: Seq[BigInt],
      memory: Map[BigInt, BigInt],
      mtvec: BigInt,
      mepc: BigInt,
      mstatus: BigInt,
      mcause: BigInt,
  )

  private def runAndCollect(enableTrace: Boolean): FinalState = {
    var captured: Option[FinalState] = None
    simulate(new Core(enableTracer = enableTrace, enableTraceFields = enableTrace)) { c =>
      val memory = mutable.Map[BigInt, BigInt](
        BigInt(START_ADDR) -> BigInt("00500093", 16),      // addi x1, x0, 5
        BigInt(START_ADDR + 4) -> BigInt("00108133", 16),  // add x2, x1, x1
        BigInt(START_ADDR + 8) -> BigInt("10002183", 16),  // lw x3, 256(x0)
        BigInt(START_ADDR + 12) -> BigInt("00118213", 16), // addi x4, x3, 1
        BigInt(START_ADDR + 16) -> BigInt("00402023", 16), // sw x4, 0(x0)
        BigInt(START_ADDR + 20) -> BigInt("00020463", 16), // beq x4, x0, 8
        BigInt(START_ADDR + 24) -> BigInt("00100293", 16), // addi x5, x0, 1
        BigInt(START_ADDR + 28) -> BigInt("0080006f", 16), // jal x0, 8
        BigInt(START_ADDR + 32) -> BigInt("00100313", 16), // addi x6, x0, 1 (wrong path)
        BigInt(START_ADDR + 36) -> BigInt("00200393", 16), // addi x7, x0, 2
        BigInt(START_ADDR + 40) -> BigInt("00100073", 16), // ebreak
        BigInt(0x100) -> BigInt(0x20),
        BigInt(0x0) -> BigInt(0x0),
      )

      c.reset.poke(true.B)
      initBus(c)
      c.clock.step()
      c.reset.poke(false.B)

      var pendingRead: List[ReadTxn] = Nil
      var pendingWriteResp: Option[WriteResp] = None
      for (_ <- 0 until 120) {
        val next = serviceBus(c, memory, pendingRead, pendingWriteResp)
        pendingRead = next._1
        pendingWriteResp = next._2
        c.clock.step()
      }

      captured = Some(FinalState(
        regs = (0 until 32).map(i => c.io.debug_regs(i).peek().litValue),
        memory = memory.toMap,
        mtvec = c.io.debug_csrs.mtvec.peek().litValue,
        mepc = c.io.debug_csrs.mepc.peek().litValue,
        mstatus = c.io.debug_csrs.mstatus.peek().litValue,
        mcause = c.io.debug_csrs.mcause.peek().litValue,
      ))
    }
    captured.get
  }

  "Core" should "preserve architectural results when trace fields are disabled" in {
    val traceOn = runAndCollect(enableTrace = true)
    val traceOff = runAndCollect(enableTrace = false)

    assert(traceOn.regs == traceOff.regs, s"register state diverged: on=${traceOn.regs} off=${traceOff.regs}")
    assert(traceOn.memory == traceOff.memory, s"memory state diverged: on=${traceOn.memory} off=${traceOff.memory}")
    assert(traceOn.mtvec == traceOff.mtvec, s"mtvec diverged: on=0x${traceOn.mtvec.toString(16)} off=0x${traceOff.mtvec.toString(16)}")
    assert(traceOn.mepc == traceOff.mepc, s"mepc diverged: on=0x${traceOn.mepc.toString(16)} off=0x${traceOff.mepc.toString(16)}")
    assert(traceOn.mstatus == traceOff.mstatus, s"mstatus diverged: on=0x${traceOn.mstatus.toString(16)} off=0x${traceOff.mstatus.toString(16)}")
    assert(traceOn.mcause == traceOff.mcause, s"mcause diverged: on=0x${traceOn.mcause.toString(16)} off=0x${traceOff.mcause.toString(16)}")
  }
}
