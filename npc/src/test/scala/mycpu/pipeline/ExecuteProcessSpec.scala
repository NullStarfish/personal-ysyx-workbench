package mycpu.pipeline

import HwOS.kernel._
import HwOS.kernel.process.ProcessBuilder
import chisel3._
import chisel3.simulator.EphemeralSimulator._
import mycpu.axi._
import mycpu.common._
import mycpu.mem.Memory
import org.scalatest.flatspec.AnyFlatSpec

final class StubExecuteDecodeProcess(localName: String)(implicit kernel: Kernel)
    extends HwProcess(localName)
{
  val api: DecodeApiDecl = new DecodeApiDecl {
    override def decodeInst(inst: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_decode_inst") { _ => () }
  }
  override def entry(): Unit = {}
}

class ExecuteProcessHarness extends Module {
  val io = IO(new Bundle {
    val done = Output(Bool())
    val addResult = Output(UInt(XLEN.W))
    val subResult = Output(UInt(XLEN.W))
    val eqResult = Output(Bool())
  })

  implicit val kernel: Kernel = new Kernel()

  val bus = Wire(new AXI4Bundle(AXI_ID_WIDTH, XLEN, XLEN))
  bus.setAsMasterInit()

  io.done := DontCare
  io.addResult := DontCare
  io.subResult := DontCare
  io.eqResult := DontCare

  object Init extends HwProcess("Init") {
    private def adoptChild[T <: HwProcess](child: => T): T = {
      ProcessBuilder.push(this)
      val c = child
      ProcessBuilder.pop()
      children += c
      c
    }

    val fetchRef = new ApiRef[FetchApiDecl]
    val decodeRef = new ApiRef[DecodeApiDecl]
    private val memory = spawn(new Memory(bus, maxClients = 1))
    private val regfile = spawn(new RegfileProcess("Regfile"))
    lazy val decode = spawn(new StubExecuteDecodeProcess("Decode"))
    lazy val fetch: FetchProcess = adoptChild(new FetchProcess(memory, decodeRef, "Fetch"))
    val execute = spawn(new ExecuteProcess(fetchRef, regfile, "Execute"))
    private val worker = createThread("Worker")
    private val daemon = createLogic("Daemon")

    private val doneReg = RegInit(false.B)
    private val addResultReg = RegInit(0.U(XLEN.W))
    private val subResultReg = RegInit(0.U(XLEN.W))
    private val eqResultReg = RegInit(false.B)

    override def entry(): Unit = {
      worker.entry {
        val exec = SysCall.Inline(execute.RequestExecuteApi())
        worker.Step("Add") {
          SysCall.Inline(exec.add(1.U, 7.U(XLEN.W), 5.U(XLEN.W)))
        }
        worker.Prev.edge.add {
          addResultReg := 12.U(XLEN.W)
        }

        worker.Step("Sub") {
          SysCall.Inline(exec.sub(2.U, 7.U(XLEN.W), 5.U(XLEN.W)))
        }
        worker.Prev.edge.add {
          subResultReg := 2.U(XLEN.W)
        }

        worker.Step("Eq") {
          SysCall.Inline(exec.eq(9.U(XLEN.W), 9.U(XLEN.W), 16.S(XLEN.W)))
        }
        worker.Prev.edge.add {
          eqResultReg := true.B
        }

        worker.Step("WriteEffects") {
          SysCall.Inline(exec.redirect("h30000020".U(XLEN.W)))
          doneReg := true.B
        }
        SysCall.Return()
      }

      daemon.run {
        when(!worker.active && !worker.done) {
          SysCall.Inline(SysCall.start(worker))
        }

        io.done := doneReg
        io.addResult := addResultReg
        io.subResult := subResultReg
        io.eqResult := eqResultReg
      }
    }
  }

  Init.decodeRef.bind(Init.decode.api)
  Init.fetchRef.bind(Init.fetch.api)
  Init.fetch.build()
  Init.build()

  bus.aw.ready := false.B
  bus.w.ready := false.B
  bus.b.valid := false.B
  bus.b.bits := DontCare
  bus.ar.ready := false.B
  bus.r.valid := false.B
  bus.r.bits := DontCare
}

class ExecuteProcessSpec extends AnyFlatSpec {
  "ExecuteProcess" should "expose explicit action APIs for decode-facing control flow" in {
    simulate(new ExecuteProcessHarness) { c =>
      c.reset.poke(true.B)
      c.clock.step()
      c.reset.poke(false.B)

      var cycles = 0
      while (c.io.done.peek().litValue == 0 && cycles < 30) {
        c.clock.step()
        cycles += 1
      }

      c.io.done.expect(true.B)
      c.io.addResult.expect(12.U)
      c.io.subResult.expect(2.U)
      c.io.eqResult.expect(true.B)
    }
  }
}
