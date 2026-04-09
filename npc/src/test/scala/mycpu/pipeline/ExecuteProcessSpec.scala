package mycpu.pipeline

import HwOS.kernel._
import chisel3._
import chisel3.simulator.EphemeralSimulator._
import mycpu.common._
import org.scalatest.flatspec.AnyFlatSpec

final class DummyExecuteLsuProcess(localName: String)(implicit kernel: Kernel) extends HwProcess(localName) {
  val opKind = RegInit(0.U(8.W))
  val rdReg = RegInit(0.U(5.W))
  val addrReg = RegInit(0.U(XLEN.W))
  val dataReg = RegInit(0.U(XLEN.W))
  val unsignedReg = RegInit(false.B)

  val api: LsuApiDecl = new LsuApiDecl {
    override def loadPath(): HwInline[Unit] = HwInline.thread(s"${name}_load_path") { t =>
      val tag = s"${name}_load_path_${System.identityHashCode(new Object())}"
      t.Step(s"${tag}_idle") {}
    }

    override def storePath(): HwInline[Unit] = HwInline.thread(s"${name}_store_path") { t =>
      val tag = s"${name}_store_path_${System.identityHashCode(new Object())}"
      t.Step(s"${tag}_idle") {}
    }

    override def loadWord(wbToken: UInt, addr: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_load_word") { _ =>
      opKind := 1.U
      rdReg := wbToken
      addrReg := addr
      unsignedReg := false.B
    }

    override def storeWord(addr: UInt, data: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_store_word") { _ =>
      opKind := 2.U
      addrReg := addr
      dataReg := data
    }

    override def loadByte(wbToken: UInt, addr: UInt, unsigned: Bool): HwInline[Unit] = HwInline.atomic(s"${name}_load_byte") { _ =>
      opKind := 3.U
      rdReg := wbToken
      addrReg := addr
      unsignedReg := unsigned
    }

    override def loadHalf(wbToken: UInt, addr: UInt, unsigned: Bool): HwInline[Unit] = HwInline.atomic(s"${name}_load_half") { _ =>
      opKind := 4.U
      rdReg := wbToken
      addrReg := addr
      unsignedReg := unsigned
    }

    override def storeByte(addr: UInt, data: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_store_byte") { _ =>
      opKind := 5.U
      addrReg := addr
      dataReg := data
    }

    override def storeHalf(addr: UInt, data: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_store_half") { _ =>
      opKind := 6.U
      addrReg := addr
      dataReg := data
    }
  }

  override def entry(): Unit = {}
}

final class DummyExecuteWritebackProcess(localName: String)(implicit kernel: Kernel) extends HwProcess(localName) {
  val x = RegInit(VecInit(Seq.fill(32)(0.U(XLEN.W))))
  val pc = RegInit(START_ADDR.U(XLEN.W))

  val api: WritebackApiDecl = new WritebackApiDecl {
    override def wbPath(): HwInline[Unit] = {
      val tag = s"${name}_wb_path_${System.identityHashCode(new Object())}"
      HwInline.thread(tag) { t =>
        t.Step(s"${tag}_idle") {}
      }
    }
    override def writeReg(token: UInt, data: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_write_reg") { _ =>
      when(token =/= 0.U) {
        x(token) := data
      }
    }

    override def writeRegAndRedirect(token: UInt, data: UInt, nextPc: UInt): HwInline[Unit] = HwInline.atomic(
      s"${name}_write_reg_and_redirect",
    ) { _ =>
      when(token =/= 0.U) {
        x(token) := data
      }
      pc := nextPc
    }

    override def redirect(nextPc: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_redirect") { _ =>
      pc := nextPc
    }

    override def redirectRelative(delta: SInt): HwInline[Unit] = HwInline.atomic(s"${name}_redirect_relative") { _ =>
      pc := (pc.asSInt + delta).asUInt
    }

    override def commit(): HwInline[Unit] = HwInline.atomic(s"${name}_commit") { _ =>
    }
  }

  override def entry(): Unit = {}
}

class ExecuteProcessHarness extends Module {
  val io = IO(new Bundle {
    val done = Output(Bool())
    val x1 = Output(UInt(XLEN.W))
    val x2 = Output(UInt(XLEN.W))
    val x3 = Output(UInt(XLEN.W))
    val x4 = Output(UInt(XLEN.W))
    val x5 = Output(UInt(XLEN.W))
    val x6 = Output(UInt(XLEN.W))
    val pc = Output(UInt(XLEN.W))
    val lsuOp = Output(UInt(8.W))
    val lsuRd = Output(UInt(5.W))
    val lsuAddr = Output(UInt(XLEN.W))
    val lsuData = Output(UInt(XLEN.W))
    val lsuUnsigned = Output(Bool())
  })

  implicit val kernel: Kernel = new Kernel()

  object Init extends HwProcess("Init") {
    val links = new PipelineLinks
    val lsu = spawn(new DummyExecuteLsuProcess("Lsu"))
    val writeback = spawn(new DummyExecuteWritebackProcess("Writeback"))
    val execute = adopt(new ExecuteProcess(links.lsu, links.writeback, "Execute"))
    private val addWorker = createThread("AddWorker")
    private val subWorker = createThread("SubWorker")
    private val csrRwWorker = createThread("CsrRwWorker")
    private val csrRsWorker = createThread("CsrRsWorker")
    private val csrRcWorker = createThread("CsrRcWorker")
    private val csrReadBackWorker = createThread("CsrReadBackWorker")
    private val redirectWorker = createThread("RedirectWorker")
    private val loadWordWorker = createThread("LoadWordWorker")
    private val storeHalfWorker = createThread("StoreHalfWorker")
    private val daemon = createLogic("Daemon")
    private val doneReg = RegInit(false.B)

    override def entry(): Unit = {
      addWorker.entry {
        val exec = SysCall.Inline(execute.RequestExecuteApi())
        addWorker.Step("Add") {
          SysCall.Inline(exec.add(1.U, 1.U, 7.U(XLEN.W), 5.U(XLEN.W)))
        }
        SysCall.Inline(exec.execPath())
        addWorker.Step("Done") {}
        SysCall.Return()
      }

      subWorker.entry {
        val exec = SysCall.Inline(execute.RequestExecuteApi())
        subWorker.Step("Sub") {
          SysCall.Inline(exec.sub(2.U, 2.U, 7.U(XLEN.W), 5.U(XLEN.W)))
        }
        SysCall.Inline(exec.execPath())
        subWorker.Step("Done") {}
        SysCall.Return()
      }

      csrRwWorker.entry {
        val exec = SysCall.Inline(execute.RequestExecuteApi())
        csrRwWorker.Step("CsrRw") {
          SysCall.Inline(exec.csrRw(3.U, 3.U, "h300".U, "h55".U(XLEN.W)))
        }
        SysCall.Inline(exec.execPath())
        csrRwWorker.Step("Done") {}
        SysCall.Return()
      }

      csrRsWorker.entry {
        val exec = SysCall.Inline(execute.RequestExecuteApi())
        csrRsWorker.Step("CsrRs") {
          SysCall.Inline(exec.csrRs(4.U, 4.U, "h300".U, "h0a".U(XLEN.W)))
        }
        SysCall.Inline(exec.execPath())
        csrRsWorker.Step("Done") {}
        SysCall.Return()
      }

      csrRcWorker.entry {
        val exec = SysCall.Inline(execute.RequestExecuteApi())
        csrRcWorker.Step("CsrRc") {
          SysCall.Inline(exec.csrRc(5.U, 5.U, "h300".U, "h0f".U(XLEN.W)))
        }
        SysCall.Inline(exec.execPath())
        csrRcWorker.Step("Done") {}
        SysCall.Return()
      }

      csrReadBackWorker.entry {
        val exec = SysCall.Inline(execute.RequestExecuteApi())
        csrReadBackWorker.Step("CsrReadBack") {
          SysCall.Inline(exec.csrRs(6.U, 6.U, "h300".U, 0.U(XLEN.W)))
        }
        SysCall.Inline(exec.execPath())
        csrReadBackWorker.Step("Done") {}
        SysCall.Return()
      }

      redirectWorker.entry {
        val exec = SysCall.Inline(execute.RequestExecuteApi())
        redirectWorker.Step("Redirect") {
          SysCall.Inline(exec.redirect("h30000020".U(XLEN.W)))
        }
        SysCall.Inline(exec.execPath())
        redirectWorker.Step("Done") {}
        SysCall.Return()
      }

      loadWordWorker.entry {
        val exec = SysCall.Inline(execute.RequestExecuteApi())
        loadWordWorker.Step("LoadWord") {
          SysCall.Inline(exec.loadWord(7.U, 7.U, 10.U(XLEN.W), 4.U(XLEN.W)))
        }
        SysCall.Inline(exec.execPath())
        loadWordWorker.Step("Done") {}
        SysCall.Return()
      }

      storeHalfWorker.entry {
        val exec = SysCall.Inline(execute.RequestExecuteApi())
        storeHalfWorker.Step("StoreHalf") {
          SysCall.Inline(exec.storeHalf(20.U(XLEN.W), 8.U(XLEN.W), "h1234".U(XLEN.W)))
        }
        SysCall.Inline(exec.execPath())
        storeHalfWorker.Step("WaitStoreObserved") {
          storeHalfWorker.waitCondition(lsu.opKind === 6.U)
        }
        storeHalfWorker.Step("Finish") {
          doneReg := true.B
        }
        SysCall.Return()
      }

      daemon.run {
        when(!addWorker.active && !addWorker.done) { SysCall.Inline(SysCall.start(addWorker)) }
        when(addWorker.done && !subWorker.active && !subWorker.done) { SysCall.Inline(SysCall.start(subWorker)) }
        when(subWorker.done && !csrRwWorker.active && !csrRwWorker.done) { SysCall.Inline(SysCall.start(csrRwWorker)) }
        when(csrRwWorker.done && !csrRsWorker.active && !csrRsWorker.done) {
          SysCall.Inline(SysCall.start(csrRsWorker))
        }
        when(csrRsWorker.done && !csrRcWorker.active && !csrRcWorker.done) {
          SysCall.Inline(SysCall.start(csrRcWorker))
        }
        when(csrRcWorker.done && !csrReadBackWorker.active && !csrReadBackWorker.done) {
          SysCall.Inline(SysCall.start(csrReadBackWorker))
        }
        when(csrReadBackWorker.done && !redirectWorker.active && !redirectWorker.done) {
          SysCall.Inline(SysCall.start(redirectWorker))
        }
        when(redirectWorker.done && !loadWordWorker.active && !loadWordWorker.done) {
          SysCall.Inline(SysCall.start(loadWordWorker))
        }
        when(loadWordWorker.done && !storeHalfWorker.active && !storeHalfWorker.done) {
          SysCall.Inline(SysCall.start(storeHalfWorker))
        }

        io.done := doneReg
        io.x1 := writeback.x(1)
        io.x2 := writeback.x(2)
        io.x3 := writeback.x(3)
        io.x4 := writeback.x(4)
        io.x5 := writeback.x(5)
        io.x6 := writeback.x(6)
        io.pc := writeback.pc
        io.lsuOp := lsu.opKind
        io.lsuRd := lsu.rdReg
        io.lsuAddr := lsu.addrReg
        io.lsuData := lsu.dataReg
        io.lsuUnsigned := lsu.unsignedReg
      }
    }
  }

  Init.links.lsu.bind(Init.lsu.api)
  Init.links.writeback.bind(Init.writeback.api)
  Init.execute.build()
  Init.build()
}

class ExecuteProcessSpec extends AnyFlatSpec {
  "ExecuteProcess" should "compose alu, csr, writeback and lsu requests through dummy backend processes" in {
    simulate(new ExecuteProcessHarness) { c =>
      c.reset.poke(true.B)
      c.clock.step()
      c.reset.poke(false.B)

      var cycles = 0
      while (c.io.done.peek().litValue == 0 && cycles < 120) {
        c.clock.step()
        cycles += 1
      }
      c.clock.step()

      c.io.done.expect(true.B)
      c.io.x1.expect(12.U)
      c.io.x2.expect(2.U)
      c.io.x3.expect("h1800".U)
      c.io.x4.expect("h55".U)
      c.io.x5.expect("h5f".U)
      c.io.x6.expect("h50".U)
      c.io.pc.expect("h30000020".U(XLEN.W))
      c.io.lsuOp.expect(6.U)
      c.io.lsuAddr.expect(28.U)
      c.io.lsuData.expect("h1234".U(XLEN.W))
    }
  }
}
