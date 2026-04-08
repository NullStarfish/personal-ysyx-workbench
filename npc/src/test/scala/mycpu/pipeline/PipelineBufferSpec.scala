package mycpu.pipeline

import HwOS.kernel._
import chisel3._
import chisel3.simulator.EphemeralSimulator._
import mycpu.common._
import org.scalatest.flatspec.AnyFlatSpec

class PipelineBufferHarness extends Module {
  val io = IO(new Bundle {
    val done = Output(Bool())
    val firstPop = Output(UInt(32.W))
    val secondPop = Output(UInt(32.W))
  })

  implicit val kernel: Kernel = new Kernel()

  object Init extends HwProcess("Init") {
    private val buffer = spawn(new PipelineBuffer(UInt(32.W), localName = "Buffer"))
    private val worker = createThread("Worker")
    private val daemon = createLogic("Daemon")
    private val doneReg = RegInit(false.B)
    private val firstPopReg = RegInit(0.U(32.W))
    private val secondPopReg = RegInit(0.U(32.W))

    override def entry(): Unit = {
      worker.entry {
        worker.Step("PushFirst") {
          SysCall.Inline(buffer.push(42.U))
        }
        worker.Step("PopFirst") {
          firstPopReg := SysCall.Inline(buffer.pop())
        }
        worker.Step("PushThenClear") {
          SysCall.Inline(buffer.push(77.U))
          SysCall.Inline(buffer.clear())
        }
        worker.Step("PushSecond") {
          SysCall.Inline(buffer.push(99.U))
        }
        worker.Step("PopSecond") {
          secondPopReg := SysCall.Inline(buffer.pop())
        }
        worker.Step("Finish") {
          doneReg := true.B
        }
        SysCall.Return()
      }

      daemon.run {
        when(!worker.active && !worker.done) {
          SysCall.Inline(SysCall.start(worker))
        }
        io.done := doneReg
        io.firstPop := firstPopReg
        io.secondPop := secondPopReg
      }
    }
  }

  Init.build()
}

class PipelineBufferSpec extends AnyFlatSpec {
  "PipelineBuffer" should "support push, pop and clear with an internal one-cycle clear gate" in {
    simulate(new PipelineBufferHarness) { c =>
      c.reset.poke(true.B)
      c.clock.step()
      c.reset.poke(false.B)

      var cycles = 0
      while (c.io.done.peek().litValue == 0 && cycles < 40) {
        c.clock.step()
        cycles += 1
      }

      c.io.done.expect(true.B)
      c.io.firstPop.expect(42.U)
      c.io.secondPop.expect(99.U)
    }
  }
}
