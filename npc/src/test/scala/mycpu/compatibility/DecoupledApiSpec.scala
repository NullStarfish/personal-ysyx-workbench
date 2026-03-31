package mycpu.compatibility

import HwOS.kernel.process.HwProcess
import HwOS.kernel.system.{Kernel, SysCall}
import chisel3._
import chisel3.simulator.EphemeralSimulator._
import chisel3.util.Queue
import org.scalatest.flatspec.AnyFlatSpec

class DecoupledApiHarness extends Module {
  val io = IO(new Bundle {
    val start = Input(Bool())
    val done = Output(Bool())
    val pushed = Output(UInt(32.W))
    val popped = Output(UInt(32.W))
  })

  implicit val kernel: Kernel = new Kernel()

  io.done := DontCare
  io.pushed := DontCare
  io.popped := DontCare

  object Init extends HwProcess("Init") {
    private val worker = createThread("Worker")
    private val daemon = createLogic("Daemon")
    private val queue = Module(new Queue(UInt(32.W), 1))

    private val pushedReg = RegInit(0.U(32.W))
    private val poppedReg = RegInit(0.U(32.W))
    private val doneReg = RegInit(false.B)

    queue.io.enq.valid := false.B
    queue.io.enq.bits := 0.U
    queue.io.deq.ready := false.B

    override def entry(): Unit = {
      worker.entry {
        val poppedWire = WireInit(0.U(32.W))

        worker.Step("Push") {
          SysCall.Inline(DecoupledApi.push(queue.io.enq, 42.U))
          pushedReg := 42.U
        }

        worker.Step("Pop") {
          poppedWire := SysCall.Inline(DecoupledApi.pop(queue.io.deq))
        }

        worker.Prev.edge.add {
          poppedReg := poppedWire
        }

        worker.Step("Done") {
          doneReg := true.B
        }

        SysCall.Return()
      }

      daemon.run {
        when(io.start && !worker.active) {
          doneReg := false.B
          pushedReg := 0.U
          poppedReg := 0.U
          SysCall.Inline(SysCall.start(worker))
        }

        io.done := doneReg
        io.pushed := pushedReg
        io.popped := poppedReg
      }
    }
  }

  Init.build()
}

class DecoupledApiSpec extends AnyFlatSpec {
  "DecoupledApi" should "allow a thread to push into and pop from a Queue using edge capture" in {
    simulate(new DecoupledApiHarness) { c =>
      c.reset.poke(true.B)
      c.io.start.poke(false.B)
      c.clock.step()

      c.reset.poke(false.B)
      c.io.start.poke(true.B)
      c.clock.step()
      c.io.start.poke(false.B)

      var cycles = 0
      while (c.io.done.peek().litValue == 0 && cycles < 20) {
        c.clock.step()
        cycles += 1
      }

      c.io.done.expect(true.B)
      c.io.pushed.expect(42.U)
      c.io.popped.expect(42.U)
    }
  }
}
