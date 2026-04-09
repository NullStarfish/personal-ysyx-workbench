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
    override def loadWord(rd: UInt, addr: UInt): HwInline[Unit] = HwInline.thread(s"${name}_load_word") { _ =>
      opKind := 1.U
      rdReg := rd
      addrReg := addr
      SysCall.Return()
    }

    override def storeWord(addr: UInt, data: UInt): HwInline[Unit] = HwInline.thread(s"${name}_store_word") { _ =>
      opKind := 2.U
      addrReg := addr
      dataReg := data
      SysCall.Return()
    }

    override def loadByte(rd: UInt, addr: UInt, unsigned: Bool): HwInline[Unit] = HwInline.thread(s"${name}_load_byte") { _ =>
      opKind := 3.U
      rdReg := rd
      addrReg := addr
      unsignedReg := unsigned
      SysCall.Return()
    }

    override def loadHalf(rd: UInt, addr: UInt, unsigned: Bool): HwInline[Unit] = HwInline.thread(s"${name}_load_half") { _ =>
      opKind := 4.U
      rdReg := rd
      addrReg := addr
      unsignedReg := unsigned
      SysCall.Return()
    }

    override def storeByte(addr: UInt, data: UInt): HwInline[Unit] = HwInline.thread(s"${name}_store_byte") { _ =>
      opKind := 5.U
      addrReg := addr
      dataReg := data
      SysCall.Return()
    }

    override def storeHalf(addr: UInt, data: UInt): HwInline[Unit] = HwInline.thread(s"${name}_store_half") { _ =>
      opKind := 6.U
      addrReg := addr
      dataReg := data
      SysCall.Return()
    }
  }

  override def entry(): Unit = {}
}

final class DummyExecuteWritebackProcess(localName: String)(implicit kernel: Kernel) extends HwProcess(localName) {
  val x = RegInit(VecInit(Seq.fill(32)(0.U(XLEN.W))))
  val pc = RegInit(START_ADDR.U(XLEN.W))

  val api: WritebackApiDecl = new WritebackApiDecl {
    override def writeReg(rd: UInt, data: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_write_reg") { _ =>
      when(rd =/= 0.U) {
        x(rd) := data
      }
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
    private val mainWorker = createThread("MainWorker")
    private val memWorker = createThread("MemWorker")
    private val daemon = createLogic("Daemon")
    private val doneReg = RegInit(false.B)
    private val mainDoneReg = RegInit(false.B)

    override def entry(): Unit = {
      mainWorker.entry {
        val exec = SysCall.Inline(execute.RequestExecuteApi())

        mainWorker.Step("Add") {
          SysCall.Inline(exec.add(1.U, 7.U(XLEN.W), 5.U(XLEN.W)))
        }

        mainWorker.Step("Sub") {
          SysCall.Inline(exec.sub(2.U, 7.U(XLEN.W), 5.U(XLEN.W)))
        }

        mainWorker.Step("Eq") {
          SysCall.Inline(exec.eq(9.U(XLEN.W), 9.U(XLEN.W), START_ADDR.U(XLEN.W), "h10".U(XLEN.W)))
        }

        mainWorker.Step("CsrRw") {
          SysCall.Inline(exec.csrRw(3.U, "h300".U, "h55".U(XLEN.W)))
        }

        mainWorker.Step("CsrRs") {
          SysCall.Inline(exec.csrRs(4.U, "h300".U, "h0a".U(XLEN.W)))
        }

        mainWorker.Step("CsrRc") {
          SysCall.Inline(exec.csrRc(5.U, "h300".U, "h0f".U(XLEN.W)))
        }

        mainWorker.Step("CsrReadBack") {
          SysCall.Inline(exec.csrRs(6.U, "h300".U, 0.U(XLEN.W)))
        }

        mainWorker.Step("Redirect") {
          SysCall.Inline(exec.redirect("h30000020".U(XLEN.W)))
        }

        mainWorker.Step("LoadWord") {
          SysCall.Inline(exec.loadWord(7.U, 10.U(XLEN.W), 4.U(XLEN.W)))
        }

        SysCall.Inline(exec.memPath())
        mainWorker.Step("Finish") {
          mainDoneReg := true.B
        }
        SysCall.Return()
      }

      memWorker.entry {
        val exec = SysCall.Inline(execute.RequestExecuteApi())

        memWorker.Step("StoreHalf") {
          SysCall.Inline(exec.storeHalf(20.U(XLEN.W), 8.U(XLEN.W), "h1234".U(XLEN.W)))
        }

        SysCall.Inline(exec.memPath())
        memWorker.Step("Finish") {
          doneReg := true.B
        }
        SysCall.Return()
      }

      daemon.run {
        when(!mainWorker.active && !mainWorker.done) {
          SysCall.Inline(SysCall.start(mainWorker))
        }
        when(mainDoneReg && !memWorker.active && !memWorker.done) {
          SysCall.Inline(SysCall.start(memWorker))
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
      while (c.io.done.peek().litValue == 0 && cycles < 50) {
        c.clock.step()
        cycles += 1
      }

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
