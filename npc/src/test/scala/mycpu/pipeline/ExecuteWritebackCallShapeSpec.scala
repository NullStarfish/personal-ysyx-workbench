package mycpu.pipeline

import HwOS.kernel._
import chisel3._
import chisel3.simulator.EphemeralSimulator._
import mycpu.common._
import org.scalatest.flatspec.AnyFlatSpec

final class CountingExecuteLsuProcess(localName: String)(implicit kernel: Kernel) extends HwProcess(localName) {
  val loadCalls = RegInit(0.U(8.W))
  val storeCalls = RegInit(0.U(8.W))

  val api: LsuApiDecl = new LsuApiDecl {
    override def loadPath(): HwInline[Unit] = HwInline.thread(s"${name}_load_path") { t =>
      t.Step(s"${name}_load_idle") {}
    }

    override def storePath(): HwInline[Unit] = HwInline.thread(s"${name}_store_path") { t =>
      t.Step(s"${name}_store_idle") {}
    }

    override def loadWord(wbToken: UInt, addr: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_load_word") { _ =>
      loadCalls := loadCalls + 1.U
    }
    override def storeWord(addr: UInt, data: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_store_word") { _ =>
      storeCalls := storeCalls + 1.U
    }
    override def loadByte(wbToken: UInt, addr: UInt, unsigned: Bool): HwInline[Unit] = HwInline.atomic(s"${name}_load_byte") { _ =>
      loadCalls := loadCalls + 1.U
    }
    override def loadHalf(wbToken: UInt, addr: UInt, unsigned: Bool): HwInline[Unit] = HwInline.atomic(s"${name}_load_half") { _ =>
      loadCalls := loadCalls + 1.U
    }
    override def storeByte(addr: UInt, data: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_store_byte") { _ =>
      storeCalls := storeCalls + 1.U
    }
    override def storeHalf(addr: UInt, data: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_store_half") { _ =>
      storeCalls := storeCalls + 1.U
    }
  }

  override def entry(): Unit = {}
}

final class CountingExecuteWritebackProcess(localName: String)(implicit kernel: Kernel) extends HwProcess(localName) {
  val writeRegCalls = RegInit(0.U(8.W))
  val writeRegRedirectCalls = RegInit(0.U(8.W))
  val redirectCalls = RegInit(0.U(8.W))
  val redirectRelativeCalls = RegInit(0.U(8.W))
  val commitCalls = RegInit(0.U(8.W))
  val lastToken = RegInit(0.U(4.W))
  val lastData = RegInit(0.U(XLEN.W))

  val api: WritebackApiDecl = new WritebackApiDecl {
    override def wbPath(): HwInline[Unit] = HwInline.thread(s"${name}_wb_path") { t =>
      t.Step(s"${name}_idle") {}
    }

    override def writeReg(token: UInt, data: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_write_reg") { _ =>
      writeRegCalls := writeRegCalls + 1.U
      lastToken := token
      lastData := data
    }

    override def writeRegAndRedirect(token: UInt, data: UInt, nextPc: UInt): HwInline[Unit] =
      HwInline.atomic(s"${name}_write_reg_redirect") { _ =>
        writeRegRedirectCalls := writeRegRedirectCalls + 1.U
        lastToken := token
        lastData := data
      }

    override def redirect(nextPc: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_redirect") { _ =>
      redirectCalls := redirectCalls + 1.U
    }

    override def redirectRelative(delta: SInt): HwInline[Unit] = HwInline.atomic(s"${name}_redirect_rel") { _ =>
      redirectRelativeCalls := redirectRelativeCalls + 1.U
    }

  }

  override def entry(): Unit = {}
}

final class CountingExecuteHazardProcess(localName: String)(implicit kernel: Kernel) extends HwProcess(localName) {
  val redirectCalls = RegInit(0.U(8.W))
  val redirectRelativeCalls = RegInit(0.U(8.W))
  val redirectNoCommitCalls = RegInit(0.U(8.W))
  val redirectRelativeNoCommitCalls = RegInit(0.U(8.W))

  val api: ControlHazardApiDecl = new ControlHazardApiDecl {
    override def redirect(nextPc: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_redirect") { _ =>
      redirectCalls := redirectCalls + 1.U
    }

    override def redirectRelative(delta: SInt): HwInline[Unit] = HwInline.atomic(s"${name}_redirect_rel") { _ =>
      redirectRelativeCalls := redirectRelativeCalls + 1.U
    }

    override def redirectNoCommit(nextPc: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_redirect_no_commit") { _ =>
      redirectNoCommitCalls := redirectNoCommitCalls + 1.U
    }

    override def redirectRelativeNoCommit(delta: SInt): HwInline[Unit] = HwInline.atomic(s"${name}_redirect_rel_no_commit") { _ =>
      redirectRelativeNoCommitCalls := redirectRelativeNoCommitCalls + 1.U
    }
  }

  override def entry(): Unit = {}
}

final class CountingExecuteTraceProcess(localName: String)(implicit kernel: Kernel) extends HwProcess(localName) {
  val commitCalls = RegInit(0.U(8.W))

  val api: TraceApiDecl = new TraceApiDecl {
    override def issue(pc: UInt, inst: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_issue") { _ => }
    override def commit(): HwInline[Unit] = HwInline.atomic(s"${name}_commit") { _ =>
      commitCalls := commitCalls + 1.U
    }
  }

  override def entry(): Unit = {}
}

class ExecuteWritebackCallShapeHarness(mode: String) extends Module {
  val io = IO(new Bundle {
    val done = Output(Bool())
    val writeRegCalls = Output(UInt(8.W))
    val writeRegRedirectCalls = Output(UInt(8.W))
    val redirectCalls = Output(UInt(8.W))
    val redirectNoCommitCalls = Output(UInt(8.W))
    val commitCalls = Output(UInt(8.W))
    val lastToken = Output(UInt(4.W))
    val lastData = Output(UInt(XLEN.W))
  })

  implicit val kernel: Kernel = new Kernel()

  object Init extends HwProcess("Init") {
    val links = new PipelineLinks
    val lsu = spawn(new CountingExecuteLsuProcess("Lsu"))
    val writeback = spawn(new CountingExecuteWritebackProcess("Writeback"))
    val hazard = spawn(new CountingExecuteHazardProcess("Hazard"))
    val trace = spawn(new CountingExecuteTraceProcess("Trace"))
    val execute = adopt(new ExecuteProcess(links.lsu, links.writeback, links.hazard, links.trace, "Execute"))
    private val worker = createThread("Worker")
    private val observer = createThread("Observer")
    private val daemon = createLogic("Daemon")
    private val doneReg = RegInit(false.B)

    override def entry(): Unit = {
      worker.entry {
        val exec = SysCall.Inline(execute.RequestExecuteApi())
        worker.Step("Issue") {
          mode match {
            case "alu" =>
              SysCall.Inline(exec.add(3.U, 7.U, 4.U(XLEN.W), 8.U(XLEN.W)))
            case "branch-not-taken" =>
              SysCall.Inline(exec.ne(1.U(XLEN.W), 1.U(XLEN.W), "h30000000".U(XLEN.W), 8.U(XLEN.W)))
            case "jal" =>
              SysCall.Inline(exec.jal(5.U, 9.U, "h30000010".U(XLEN.W), 8.S(XLEN.W)))
            case _ =>
              throw new IllegalArgumentException(s"unknown mode: $mode")
          }
        }
        SysCall.Inline(exec.execPath())
        worker.Step("Done") {}
        SysCall.Return()
      }

      observer.entry {
        observer.Step("Wait") {
          mode match {
            case "alu" =>
              observer.waitCondition(writeback.writeRegCalls === 1.U)
            case "branch-not-taken" =>
              observer.waitCondition(trace.commitCalls === 1.U)
            case "jal" =>
              observer.waitCondition(writeback.writeRegCalls === 1.U && hazard.redirectNoCommitCalls === 1.U)
          }
        }
        observer.Step("Publish") {
          doneReg := true.B
        }
        SysCall.Return()
      }

      daemon.run {
        when(!worker.active && !worker.done) { SysCall.Inline(SysCall.start(worker)) }
        when(!observer.active && !observer.done) { SysCall.Inline(SysCall.start(observer)) }

        io.done := doneReg
        io.writeRegCalls := writeback.writeRegCalls
        io.writeRegRedirectCalls := writeback.writeRegRedirectCalls
        io.redirectCalls := hazard.redirectCalls
        io.redirectNoCommitCalls := hazard.redirectNoCommitCalls
        io.commitCalls := trace.commitCalls
        io.lastToken := writeback.lastToken
        io.lastData := writeback.lastData
      }
    }
  }

  Init.links.lsu.bind(Init.lsu.api)
  Init.links.writeback.bind(Init.writeback.api)
  Init.links.hazard.bind(Init.hazard.api)
  Init.links.trace.bind(Init.trace.api)
  Init.execute.build()
  Init.build()
}

class ExecuteWritebackCallShapeSpec extends AnyFlatSpec {
  private def runHarness(mode: String)(check: ExecuteWritebackCallShapeHarness => Unit): Unit = {
    simulate(new ExecuteWritebackCallShapeHarness(mode)) { c =>
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
      check(c)
    }
  }

  "ExecuteProcess writeback call shape" should "call writeReg exactly once for a plain ALU op" in {
    runHarness("alu") { c =>
      c.io.writeRegCalls.expect(1.U)
      c.io.writeRegRedirectCalls.expect(0.U)
      c.io.redirectCalls.expect(0.U)
      c.io.redirectNoCommitCalls.expect(0.U)
      c.io.commitCalls.expect(0.U)
      c.io.lastToken.expect(7.U)
      c.io.lastData.expect(12.U)
    }
  }

  it should "call commit exactly once for a not-taken branch" in {
    runHarness("branch-not-taken") { c =>
      c.io.writeRegCalls.expect(0.U)
      c.io.writeRegRedirectCalls.expect(0.U)
      c.io.redirectCalls.expect(0.U)
      c.io.redirectNoCommitCalls.expect(0.U)
      c.io.commitCalls.expect(1.U)
    }
  }

  it should "call writeReg once and redirectNoCommit once for jal" in {
    runHarness("jal") { c =>
      c.io.writeRegCalls.expect(1.U)
      c.io.writeRegRedirectCalls.expect(0.U)
      c.io.redirectCalls.expect(0.U)
      c.io.redirectNoCommitCalls.expect(1.U)
      c.io.commitCalls.expect(0.U)
      c.io.lastToken.expect(9.U)
      c.io.lastData.expect("h30000014".U(XLEN.W))
    }
  }
}
