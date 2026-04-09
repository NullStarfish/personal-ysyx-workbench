package mycpu.pipeline

import HwOS.kernel._
import chisel3._
import chisel3.simulator.EphemeralSimulator._
import mycpu.common._
import org.scalatest.flatspec.AnyFlatSpec

final class DummyWritebackFetchProcess(localName: String)(implicit kernel: Kernel) extends HwProcess(localName) {
  val pcReg = RegInit(START_ADDR.U(XLEN.W))

  val api: FetchApiDecl = new FetchApiDecl {
    override def writePC(nextPc: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_write_pc") { _ =>
      pcReg := nextPc
    }

    override def offsetPC(delta: SInt): HwInline[Unit] = HwInline.atomic(s"${name}_offset_pc") { _ =>
      pcReg := (pcReg.asSInt + delta).asUInt
    }

    override def currentPC(): HwInline[UInt] = HwInline.bindings(s"${name}_current_pc") { _ =>
      pcReg
    }
  }

  override def entry(): Unit = {}
}

final class DummyWritebackTraceProcess(localName: String)(implicit kernel: Kernel) extends HwProcess(localName) {
  val commitCount = RegInit(0.U(32.W))

  val api: TraceApiDecl = new TraceApiDecl {
    override def issue(pc: UInt, inst: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_issue") { _ =>
    }

    override def commit(): HwInline[Unit] = HwInline.atomic(s"${name}_commit") { _ =>
      commitCount := commitCount + 1.U
    }
  }

  override def entry(): Unit = {}
}

class WritebackProcessHarness extends Module {
  val io = IO(new Bundle {
    val done = Output(Bool())
    val x1 = Output(UInt(XLEN.W))
    val x5 = Output(UInt(XLEN.W))
    val pc = Output(UInt(XLEN.W))
    val commitCount = Output(UInt(32.W))
  })

  implicit val kernel: Kernel = new Kernel()

  object Init extends HwProcess("Init") {
    val fetch = spawn(new DummyWritebackFetchProcess("Fetch"))
    val regfile = spawn(new RegfileProcess("Regfile"))
    val trace = spawn(new DummyWritebackTraceProcess("Trace"))
    val writeback = spawn(new WritebackProcess(fetch.api, regfile.api, trace.api, "Writeback"))

    private val writeRegWorker = createThread("WriteRegWorker")
    private val writeRegRedirectWorker = createThread("WriteRegRedirectWorker")
    private val redirectRelativeWorker = createThread("RedirectRelativeWorker")
    private val observer = createThread("Observer")
    private val daemon = createLogic("Daemon")

    private val doneReg = RegInit(false.B)
    private val x1Reg = RegInit(0.U(XLEN.W))
    private val x5Reg = RegInit(0.U(XLEN.W))
    private val pcReg = RegInit(0.U(XLEN.W))
    private val commitCountReg = RegInit(0.U(32.W))

    override def entry(): Unit = {
      writeRegWorker.entry {
        val wbApi = SysCall.Inline(writeback.RequestWritebackApi())
        val regApi = SysCall.Inline(regfile.RequestRegfileApi())
        val tokenReg = RegInit(0.U(4.W))
        writeRegWorker.Step("Reserve") {
          tokenReg := SysCall.Call(regApi.reserve(1.U), "WriteReg")
        }
        writeRegWorker.Step("WriteReg") {
          SysCall.Inline(wbApi.writeReg(tokenReg, "h12345678".U(XLEN.W)))
        }
        SysCall.Inline(wbApi.wbPath())
        writeRegWorker.Step("Done") {}
        SysCall.Return()
      }

      writeRegRedirectWorker.entry {
        val wbApi = SysCall.Inline(writeback.RequestWritebackApi())
        val regApi = SysCall.Inline(regfile.RequestRegfileApi())
        val tokenReg = RegInit(0.U(4.W))
        writeRegRedirectWorker.Step("Reserve") {
          tokenReg := SysCall.Call(regApi.reserve(5.U), "WriteRegAndRedirect")
        }
        writeRegRedirectWorker.Step("WriteRegAndRedirect") {
          SysCall.Inline(wbApi.writeRegAndRedirect(tokenReg, "hdeadbeef".U(XLEN.W), "h30000020".U(XLEN.W)))
        }
        SysCall.Inline(wbApi.wbPath())
        writeRegRedirectWorker.Step("Done") {}
        SysCall.Return()
      }

      redirectRelativeWorker.entry {
        val wbApi = SysCall.Inline(writeback.RequestWritebackApi())
        redirectRelativeWorker.Step("RedirectRelative") {
          SysCall.Inline(wbApi.redirectRelative(4.S(XLEN.W)))
        }
        SysCall.Inline(wbApi.wbPath())
        redirectRelativeWorker.Step("Done") {}
        SysCall.Return()
      }

      observer.entry {
        val regProbeApi = SysCall.Inline(regfile.RequestRegfileProbeApi())

        observer.Step("WaitRetire") {
          observer.waitCondition(trace.commitCount === 3.U)
        }
        observer.Step("Sample") {
          val regsFlat = SysCall.Inline(regProbeApi.readAllFlat())
          x1Reg := regsFlat(2 * XLEN - 1, 1 * XLEN)
          x5Reg := regsFlat(6 * XLEN - 1, 5 * XLEN)
          pcReg := fetch.pcReg
          commitCountReg := trace.commitCount
        }
        observer.Step("PublishDone") {
          doneReg := true.B
        }
        SysCall.Return()
      }

      daemon.run {
        when(trace.commitCount === 0.U && !writeRegWorker.active && !writeRegWorker.done) {
          SysCall.Inline(SysCall.start(writeRegWorker))
        }
        when(trace.commitCount === 1.U && !writeRegRedirectWorker.active && !writeRegRedirectWorker.done) {
          SysCall.Inline(SysCall.start(writeRegRedirectWorker))
        }
        when(trace.commitCount === 2.U && !redirectRelativeWorker.active && !redirectRelativeWorker.done) {
          SysCall.Inline(SysCall.start(redirectRelativeWorker))
        }
        when(!observer.active && !observer.done) {
          SysCall.Inline(SysCall.start(observer))
        }

        io.done := doneReg
        io.x1 := x1Reg
        io.x5 := x5Reg
        io.pc := pcReg
        io.commitCount := commitCountReg
      }
    }
  }

  Init.build()
}

class WritebackProcessSpec extends AnyFlatSpec {
  "WritebackProcess" should "enqueue requests and retire register writes plus pc updates through the worker" in {
    simulate(new WritebackProcessHarness) { c =>
      c.reset.poke(true.B)
      c.clock.step()
      c.reset.poke(false.B)

      var cycles = 0
      while (c.io.done.peek().litValue == 0 && cycles < 80) {
        c.clock.step()
        cycles += 1
      }
      c.clock.step()

      c.io.done.expect(true.B)
      c.io.x1.expect("h12345678".U(XLEN.W))
      c.io.x5.expect("hdeadbeef".U(XLEN.W))
      c.io.pc.expect("h30000024".U(XLEN.W))
      c.io.commitCount.expect(3.U)
    }
  }
}
