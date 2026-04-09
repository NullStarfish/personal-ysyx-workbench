package mycpu.pipeline

import HwOS.kernel._
import chisel3._
import chisel3.util._
import chisel3.simulator.EphemeralSimulator._
import org.scalatest.flatspec.AnyFlatSpec

trait CommitShapeApiDecl {
  def commitLike(token: UInt, data: UInt): HwInline[Unit]
}

final class CommitShapeProcess(localName: String)(implicit kernel: Kernel) extends HwProcess(localName) {
  private val flags = RegInit(VecInit(Seq(true.B, true.B, false.B, false.B)))
  private val addrs = RegInit(VecInit(Seq(1.U(5.W), 2.U(5.W), 0.U(5.W), 0.U(5.W))))
  private val regs = RegInit(VecInit(Seq.fill(32)(0.U(32.W))))

  val api: CommitShapeApiDecl = new CommitShapeApiDecl {
    override def commitLike(token: UInt, data: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_commit_like") { t =>
      val idx = (token - 1.U)(log2Ceil(4) - 1, 0)
      t.waitCondition((token === 0.U) || flags(idx))
      when(token =/= 0.U) {
        when(addrs(idx) =/= 0.U) {
          regs(addrs(idx)) := data
        }
        
      }
      t.Prev.edge.add {
        when (token =/= 0.U) {
          flags(idx) := false.B
          addrs(idx) := 0.U
        }
      }
    }
  }

  def readReg(addr: Int): UInt = regs(addr)

  override def entry(): Unit = {}
}

class CommitShapeHarness extends Module {
  val io = IO(new Bundle {
    val done = Output(Bool())
    val x1 = Output(UInt(32.W))
  })

  implicit val kernel: Kernel = new Kernel()

  object Init extends HwProcess("Init") {
    val proc = spawn(new CommitShapeProcess("CommitShape"))
    private val worker = createThread("Worker")
    private val doneReg = RegInit(false.B)
    private val x1Reg = RegInit(0.U(32.W))

    override def entry(): Unit = {
      val api = proc.api
      worker.entry {
        worker.Step("Commit") {
          SysCall.Inline(api.commitLike(1.U(3.W), "hdeadbeef".U(32.W)))
        }
        worker.Step("Sample") {
          x1Reg := proc.readReg(1)
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
        io.x1 := x1Reg
      }
    }
  }

  Init.build()
}

class CommitShapeScopeSpec extends AnyFlatSpec {
  "commit-like atomic shape" should "support wait, current write, and edge clear from one token-derived index" in {
    simulate(new CommitShapeHarness) { c =>
      c.reset.poke(true.B)
      c.clock.step()
      c.reset.poke(false.B)
      c.clock.step(8)
      c.io.done.expect(true.B)
      c.io.x1.expect("hdeadbeef".U(32.W))
    }
  }
}
