package mycpu.pipeline

import HwOS.kernel._
import chisel3._
import chisel3.util._
import chisel3.simulator.EphemeralSimulator._
import org.scalatest.flatspec.AnyFlatSpec

trait GuardProbeApiDecl {
  def simpleGuard(token: UInt): HwInline[Unit]
  def indexedGuard(token: UInt): HwInline[Unit]
}

final class GuardProbeProcess(localName: String)(implicit kernel: Kernel) extends HwProcess(localName) {
  private val flags = RegInit(VecInit(Seq(true.B, true.B, true.B, true.B)))
  private val seenSimple = RegInit(false.B)
  private val seenIndexed = RegInit(false.B)

  val api: GuardProbeApiDecl = new GuardProbeApiDecl {
    override def simpleGuard(token: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_simple") { t =>
      t.waitCondition(token === 1.U || token === 2.U)
      seenSimple := true.B
    }

    override def indexedGuard(token: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_indexed") { t =>
      val idx = (token - 1.U)(log2Ceil(4) - 1, 0)
      t.waitCondition((token === 0.U) || flags(idx))
      seenIndexed := true.B
    }
  }

  def simpleSeen: Bool = seenSimple
  def indexedSeen: Bool = seenIndexed

  override def entry(): Unit = {}
}

class WaitConditionSimpleHarness extends Module {
  val io = IO(new Bundle {
    val done = Output(Bool())
  })

  implicit val kernel: Kernel = new Kernel()

  object Init extends HwProcess("Init") {
    val probe = spawn(new GuardProbeProcess("Probe"))
    private val worker = createThread("Worker")
    private val doneReg = RegInit(false.B)

    override def entry(): Unit = {
      val api = probe.api
      worker.entry {
        worker.Step("Run") {
          SysCall.Inline(api.simpleGuard(1.U))
          doneReg := true.B
        }
        SysCall.Return()
      }
      val daemon = createLogic("Daemon")
      daemon.run {
        when(!worker.active && !worker.done) {
          SysCall.Inline(SysCall.start(worker))
        }
        io.done := doneReg
      }
    }
  }

  Init.build()
}

class WaitConditionIndexedHarness extends Module {
  val io = IO(new Bundle {
    val done = Output(Bool())
  })

  implicit val kernel: Kernel = new Kernel()

  object Init extends HwProcess("Init") {
    val probe = spawn(new GuardProbeProcess("Probe"))
    private val worker = createThread("Worker")
    private val doneReg = RegInit(false.B)

    override def entry(): Unit = {
      val api = probe.api
      worker.entry {
        worker.Step("Run") {
          SysCall.Inline(api.indexedGuard(1.U(3.W)))
          doneReg := true.B
        }
        SysCall.Return()
      }
      val daemon = createLogic("Daemon")
      daemon.run {
        when(!worker.active && !worker.done) {
          SysCall.Inline(SysCall.start(worker))
        }
        io.done := doneReg
      }
    }
  }

  Init.build()
}

class WaitConditionGuardScopeSpec extends AnyFlatSpec {
  "waitCondition guard" should "support a simple parameterized predicate" in {
    simulate(new WaitConditionSimpleHarness) { c =>
      c.reset.poke(true.B)
      c.clock.step()
      c.reset.poke(false.B)
      c.clock.step(5)
      c.io.done.expect(true.B)
    }
  }

  it should "support an indexed predicate over a Vec" in {
    simulate(new WaitConditionIndexedHarness) { c =>
      c.reset.poke(true.B)
      c.clock.step()
      c.reset.poke(false.B)
      c.clock.step(5)
      c.io.done.expect(true.B)
    }
  }
}
