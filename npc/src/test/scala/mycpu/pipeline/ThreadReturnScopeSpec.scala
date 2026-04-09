package mycpu.pipeline

import HwOS.kernel._
import chisel3._
import chisel3.simulator.EphemeralSimulator._
import org.scalatest.flatspec.AnyFlatSpec

trait TinyReturnApiDecl {
  def reserve(addr: UInt): HwInline[UInt]
}

final class TinyReturnProcess(localName: String)(implicit kernel: Kernel) extends HwProcess(localName) {
  private val reqAddrReg = RegInit(0.U(4.W))
  private val resultReg = RegInit(0.U(4.W))

  val api: TinyReturnApiDecl = new TinyReturnApiDecl {
    override def reserve(addr: UInt): HwInline[UInt] = HwInline.thread(s"${name}_reserve") { t =>
      val stepTag = s"${name}_reserve_${System.identityHashCode(new Object())}"
      t.Step(s"${stepTag}_Capture") {
        t.Prev.edge.add {
          reqAddrReg := addr
        }
      }
      t.Step(s"${stepTag}_Compute") {
        t.Prev.edge.add {
          resultReg := reqAddrReg + 1.U
        }
      }
      resultReg
    }
  }

  override def entry(): Unit = {}
}

class ThreadReturnScopeHarness extends Module {
  val io = IO(new Bundle {
    val done = Output(Bool())
    val result = Output(UInt(4.W))
  })

  implicit val kernel: Kernel = new Kernel()

  object Init extends HwProcess("Init") {
    val callee = spawn(new TinyReturnProcess("Tiny"))
    private val caller = createThread("Caller")
    private val doneReg = RegInit(false.B)
    private val resultReg = RegInit(0.U(4.W))
    private val api = callee.api

    override def entry(): Unit = {
      caller.entry {
        val token = SysCall.Inline(api.reserve(3.U))
        caller.Step("Latch") {
          resultReg := token
          doneReg := true.B
        }
        SysCall.Return()
      }

      val daemon = createLogic("Daemon")
      daemon.run {
        when(!caller.active && !caller.done) {
          SysCall.Inline(SysCall.start(caller))
        }
        io.done := doneReg
        io.result := resultReg
      }
    }
  }

  Init.build()
}

class ThreadReturnScopeSpec extends AnyFlatSpec {
  "Thread return value" should "be usable from a caller thread without extra regfile semantics" in {
    simulate(new ThreadReturnScopeHarness) { c =>
      c.reset.poke(true.B)
      c.clock.step()
      c.reset.poke(false.B)

      var cycles = 0
      while (c.io.done.peek().litValue == 0 && cycles < 20) {
        c.clock.step()
        cycles += 1
      }

      c.io.done.expect(true.B)
      c.io.result.expect(4.U)
    }
  }
}
