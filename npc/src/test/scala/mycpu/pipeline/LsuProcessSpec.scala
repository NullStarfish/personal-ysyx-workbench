package mycpu.pipeline

import HwOS.kernel._
import chisel3._
import chisel3.simulator.EphemeralSimulator._
import chisel3.util._
import mycpu.common._
import org.scalatest.flatspec.AnyFlatSpec

final class DummyMemoryProcess(localName: String)(implicit kernel: Kernel) extends HwProcess(localName) {
  private val words = RegInit(
    VecInit(
      Seq(
        "h80ff1234".U(XLEN.W),
        0.U(XLEN.W),
        0.U(XLEN.W),
        0.U(XLEN.W),
      ),
    ),
  )

  val api: MemoryApiDecl = new MemoryApiDecl {
    override def read_once(addr: UInt, size: UInt): HwInline[UInt] = HwInline.thread(s"${name}_read_once") { t =>
      val idx = RegInit(0.U(2.W))
      val result = WireInit(0.U(XLEN.W))

      t.Step("Capture") {
        idx := addr(3, 2)
      }
      t.Step("Read") {
        result := words(idx)
      }
      result
    }

    override def write_once(addr: UInt, size: UInt, data: UInt, strb: UInt): HwInline[Unit] = HwInline.thread(s"${name}_write_once") { t =>
      val idx = RegInit(0.U(2.W))
      val writeData = RegInit(0.U(XLEN.W))
      val writeStrb = RegInit(0.U((XLEN / 8).W))

      t.Step("Capture") {
        idx := addr(3, 2)
        writeData := data
        writeStrb := strb
      }
      t.Step("Write") {
        val oldValue = words(idx)
        val mergedBytes = Wire(Vec(XLEN / 8, UInt(8.W)))
        for (byteIdx <- 0 until (XLEN / 8)) {
          mergedBytes(byteIdx) := Mux(
            writeStrb(byteIdx),
            writeData(byteIdx * 8 + 7, byteIdx * 8),
            oldValue(byteIdx * 8 + 7, byteIdx * 8),
          )
        }
        words(idx) := mergedBytes.asUInt
      }
    }
  }

  def readWord(idx: Int): UInt = words(idx)
  override def entry(): Unit = {}
}

final class DummyWritebackProcess(localName: String)(implicit kernel: Kernel) extends HwProcess(localName) {
  private val regs = RegInit(VecInit(Seq.fill(32)(0.U(XLEN.W))))
  private val pc = RegInit(0.U(XLEN.W))

  val api: WritebackApiDecl = new WritebackApiDecl {
    override def writeReg(rd: UInt, data: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_write_reg") { _ =>
      when(rd =/= 0.U) {
        regs(rd) := data
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

  def readReg(idx: Int): UInt = regs(idx)
  override def entry(): Unit = {}
}

class LsuProcessHarness extends Module {
  val io = IO(new Bundle {
    val done = Output(Bool())
    val signedByte = Output(UInt(XLEN.W))
    val unsignedHalf = Output(UInt(XLEN.W))
    val storedWord0 = Output(UInt(XLEN.W))
    val storedWord1 = Output(UInt(XLEN.W))
  })

  implicit val kernel: Kernel = new Kernel()

  object Init extends HwProcess("Init") {
    val links = new PipelineLinks
    val memory = spawn(new DummyMemoryProcess("DummyMemory"))
    val writeback = spawn(new DummyWritebackProcess("DummyWriteback"))
    val lsu: LsuProcess = adopt(new LsuProcess(links.memory, links.writeback, "Lsu"))
    private val loadSignedByteWorker = createThread("LoadSignedByteWorker")
    private val loadUnsignedHalfWorker = createThread("LoadUnsignedHalfWorker")
    private val storeByteWorker = createThread("StoreByteWorker")
    private val storeHalfWorker = createThread("StoreHalfWorker")
    private val readBackWord0Worker = createThread("ReadBackWord0Worker")
    private val storeWord1Worker = createThread("StoreWord1Worker")
    private val readBackWord1Worker = createThread("ReadBackWord1Worker")
    private val daemon = createLogic("Daemon")

    private val doneReg = RegInit(false.B)
    private val signedByteReg = RegInit(0.U(XLEN.W))
    private val unsignedHalfReg = RegInit(0.U(XLEN.W))
    private val storedWord0Reg = RegInit(0.U(XLEN.W))
    private val storedWord1Reg = RegInit(0.U(XLEN.W))
    private val loadSignedByteDone = RegInit(false.B)
    private val loadUnsignedHalfDone = RegInit(false.B)
    private val storeByteDone = RegInit(false.B)
    private val storeHalfDone = RegInit(false.B)
    private val readBackWord0Done = RegInit(false.B)
    private val storeWord1Done = RegInit(false.B)

    override def entry(): Unit = {
      loadSignedByteWorker.entry {
        val lsuApi = SysCall.Inline(lsu.RequestLsuApi())
        loadSignedByteWorker.Step("Run") {
          SysCall.Inline(lsuApi.loadByte(1.U, 2.U, false.B))
        }
        SysCall.Inline(lsuApi.loadPath())
        loadSignedByteWorker.Step("Finish") {
          signedByteReg := writeback.readReg(1)
          loadSignedByteDone := true.B
        }
        SysCall.Return()
      }

      loadUnsignedHalfWorker.entry {
        val lsuApi = SysCall.Inline(lsu.RequestLsuApi())
        loadUnsignedHalfWorker.Step("Run") {
          SysCall.Inline(lsuApi.loadHalf(2.U, 2.U, true.B))
        }
        SysCall.Inline(lsuApi.loadPath())
        loadUnsignedHalfWorker.Step("Finish") {
          unsignedHalfReg := writeback.readReg(2)
          loadUnsignedHalfDone := true.B
        }
        SysCall.Return()
      }

      storeByteWorker.entry {
        val lsuApi = SysCall.Inline(lsu.RequestLsuApi())
        storeByteWorker.Step("Run") {
          SysCall.Inline(lsuApi.storeByte(1.U, "hAA".U(XLEN.W)))
        }
        SysCall.Inline(lsuApi.storePath())
        storeByteWorker.Step("Finish") {
          storeByteDone := true.B
        }
        SysCall.Return()
      }

      storeHalfWorker.entry {
        val lsuApi = SysCall.Inline(lsu.RequestLsuApi())
        storeHalfWorker.Step("Run") {
          SysCall.Inline(lsuApi.storeHalf(2.U, "hBEEF".U(XLEN.W)))
        }
        SysCall.Inline(lsuApi.storePath())
        storeHalfWorker.Step("Finish") {
          storeHalfDone := true.B
        }
        SysCall.Return()
      }

      readBackWord0Worker.entry {
        val lsuApi = SysCall.Inline(lsu.RequestLsuApi())
        readBackWord0Worker.Step("Run") {
          SysCall.Inline(lsuApi.loadWord(3.U, 0.U))
        }
        SysCall.Inline(lsuApi.loadPath())
        readBackWord0Worker.Step("Finish") {
          storedWord0Reg := writeback.readReg(3)
          readBackWord0Done := true.B
        }
        SysCall.Return()
      }

      storeWord1Worker.entry {
        val lsuApi = SysCall.Inline(lsu.RequestLsuApi())
        storeWord1Worker.Step("Run") {
          SysCall.Inline(lsuApi.storeWord(4.U, "h55667788".U(XLEN.W)))
        }
        SysCall.Inline(lsuApi.storePath())
        storeWord1Worker.Step("Finish") {
          storeWord1Done := true.B
        }
        SysCall.Return()
      }

      readBackWord1Worker.entry {
        val lsuApi = SysCall.Inline(lsu.RequestLsuApi())
        readBackWord1Worker.Step("Run") {
          SysCall.Inline(lsuApi.loadWord(4.U, 4.U))
        }
        SysCall.Inline(lsuApi.loadPath())
        readBackWord1Worker.Step("Finish") {
          storedWord1Reg := writeback.readReg(4)
          doneReg := true.B
        }
        SysCall.Return()
      }

      daemon.run {
        when(!loadSignedByteWorker.active && !loadSignedByteWorker.done) {
          SysCall.Inline(SysCall.start(loadSignedByteWorker))
        }
        when(loadSignedByteDone && !loadUnsignedHalfWorker.active && !loadUnsignedHalfWorker.done) {
          SysCall.Inline(SysCall.start(loadUnsignedHalfWorker))
        }
        when(loadUnsignedHalfDone && !storeByteWorker.active && !storeByteWorker.done) {
          SysCall.Inline(SysCall.start(storeByteWorker))
        }
        when(storeByteDone && !storeHalfWorker.active && !storeHalfWorker.done) {
          SysCall.Inline(SysCall.start(storeHalfWorker))
        }
        when(storeHalfDone && !readBackWord0Worker.active && !readBackWord0Worker.done) {
          SysCall.Inline(SysCall.start(readBackWord0Worker))
        }
        when(readBackWord0Done && !storeWord1Worker.active && !storeWord1Worker.done) {
          SysCall.Inline(SysCall.start(storeWord1Worker))
        }
        when(storeWord1Done && !readBackWord1Worker.active && !readBackWord1Worker.done) {
          SysCall.Inline(SysCall.start(readBackWord1Worker))
        }
        io.done := doneReg
        io.signedByte := signedByteReg
        io.unsignedHalf := unsignedHalfReg
        io.storedWord0 := storedWord0Reg
        io.storedWord1 := storedWord1Reg
      }
    }
  }

  Init.links.memory.bind(Init.memory.api)
  Init.links.writeback.bind(Init.writeback.api)
  Init.links.lsu.bind(Init.lsu.api)
  Init.lsu.build()
  Init.build()
}

class LsuProcessSpec extends AnyFlatSpec {
  "LsuProcess" should "load and store correctly with dummy memory and dummy writeback" in {
    simulate(new LsuProcessHarness) { c =>
      c.reset.poke(true.B)
      c.clock.step()
      c.reset.poke(false.B)

      var cycles = 0
      while (c.io.done.peek().litValue == 0 && cycles < 120) {
        c.clock.step()
        cycles += 1
      }

      c.io.done.expect(true.B)
      c.io.signedByte.expect("hffffffff".U(XLEN.W))
      c.io.unsignedHalf.expect("h000080ff".U(XLEN.W))
      c.io.storedWord0.expect("hbeefaa34".U(XLEN.W))
      c.io.storedWord1.expect("h55667788".U(XLEN.W))
    }
  }
}
