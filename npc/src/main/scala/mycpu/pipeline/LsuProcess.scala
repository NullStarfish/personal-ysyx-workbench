package mycpu.pipeline

import HwOS.kernel._
import chisel3._
import chisel3.util._
import mycpu.common._

final class LsuProcess(
    memoryRef: ApiRef[MemoryApiDecl],
    writebackRef: ApiRef[WritebackApiDecl],
    localName: String = "Lsu",
)(implicit kernel: Kernel)
    extends HwProcess(localName) {

  final class LoadReq extends Bundle {
    val loadKind = UInt(2.W)
    val rd = UInt(5.W)
    val addr = UInt(XLEN.W)
    val unsigned = Bool()
  }

  final class StoreReq extends Bundle {
    val storeKind = UInt(2.W)
    val addr = UInt(XLEN.W)
    val data = UInt(XLEN.W)
  }

  private val loadReqBuffer = spawn(new PipelineBuffer(new LoadReq, "LoadReqBuffer"))
  private val storeReqBuffer = spawn(new PipelineBuffer(new StoreReq, "StoreReqBuffer"))
  private val loadWorker = createThread("LoadWorker")
  private val storeWorker = createThread("StoreWorker")

  private val LOAD_WORD = 0.U(2.W)
  private val LOAD_BYTE = 1.U(2.W)
  private val LOAD_HALF = 2.U(2.W)

  private val STORE_WORD = 0.U(2.W)
  private val STORE_BYTE = 1.U(2.W)
  private val STORE_HALF = 2.U(2.W)

  private val launchLoadReqReg = RegInit(0.U.asTypeOf(new LoadReq))
  private val launchStoreReqReg = RegInit(0.U.asTypeOf(new StoreReq))

  private def extractByte(word: UInt, byteSel: UInt, unsigned: Bool): UInt = {
    val shifted = (word >> (byteSel << 3))(7, 0)
    Mux(unsigned, Cat(0.U((XLEN - 8).W), shifted), Cat(Fill(XLEN - 8, shifted(7)), shifted))
  }

  private def extractHalf(word: UInt, halfSel: UInt, unsigned: Bool): UInt = {
    val shifted = (word >> (halfSel << 4))(15, 0)
    Mux(unsigned, Cat(0.U((XLEN - 16).W), shifted), Cat(Fill(XLEN - 16, shifted(15)), shifted))
  }

  override def entry(): Unit = {
    loadWorker.entry {
      val memory = memoryRef.get
      val wbApi = writebackRef.get
      val reqReg = RegInit(0.U.asTypeOf(new LoadReq))
      val rawReadData = RegInit(0.U(XLEN.W))

      loadWorker.Step("WaitReq") {
        loadWorker.waitCondition(loadReqBuffer.valid)
      }
      loadWorker.Step("TakeReq") {
        reqReg := SysCall.Inline(loadReqBuffer.pop())
      }

      val alignedAddr = WireInit(Mux(
        reqReg.loadKind === LOAD_WORD,
        reqReg.addr,
        Cat(reqReg.addr(XLEN - 1, 2), 0.U(2.W)),
      ))
      val loaded = SysCall.Inline(memory.read_once(alignedAddr, 2.U))
      loadWorker.Prev.edge.add {
        rawReadData := loaded
      }

      loadWorker.Step("Writeback") {
        val resultData = WireInit(rawReadData)
        when(reqReg.loadKind === LOAD_BYTE) {
          resultData := extractByte(rawReadData, reqReg.addr(1, 0), reqReg.unsigned)
        }.elsewhen(reqReg.loadKind === LOAD_HALF) {
          resultData := extractHalf(rawReadData, reqReg.addr(1), reqReg.unsigned)
        }
        SysCall.Inline(wbApi.writeReg(reqReg.rd, resultData))
        loadWorker.jump(loadWorker.stepRef("WaitReq"))
      }
    }

    storeWorker.entry {
      val memory = memoryRef.get
      val wbApi = writebackRef.get
      val reqReg = RegInit(0.U.asTypeOf(new StoreReq))

      storeWorker.Step("WaitReq") {
        storeWorker.waitCondition(storeReqBuffer.valid)
      }
      storeWorker.Step("TakeReq") {
        reqReg := SysCall.Inline(storeReqBuffer.pop())
      }

      val writeData = WireInit(0.U(XLEN.W))
      val writeSize = WireInit(0.U(2.W))
      val writeStrb = WireInit(0.U(4.W))

      when(reqReg.storeKind === STORE_WORD) {
        writeData := reqReg.data
        writeSize := 2.U
        writeStrb := "b1111".U
      }.elsewhen(reqReg.storeKind === STORE_BYTE) {
        writeData := (reqReg.data(7, 0) << (reqReg.addr(1, 0) << 3))(31, 0)
        writeSize := 0.U
        writeStrb := UIntToOH(reqReg.addr(1, 0), 4)
      }.otherwise {
        writeData := Mux(reqReg.addr(1), Cat(reqReg.data(15, 0), 0.U(16.W)), reqReg.data(15, 0))
        writeSize := 1.U
        writeStrb := Mux(reqReg.addr(1), "b1100".U(4.W), "b0011".U(4.W))
      }

      SysCall.Inline(memory.write_once(reqReg.addr, writeSize, writeData, writeStrb))
      storeWorker.Prev.edge.add {
        SysCall.Inline(wbApi.commit())
        storeWorker.jump(storeWorker.stepRef("WaitReq"))
      }
    }

    val daemon = createLogic("Daemon")
    daemon.run {
      when(!loadWorker.active) {
        SysCall.Inline(SysCall.start(loadWorker))
      }
      when(!storeWorker.active) {
        SysCall.Inline(SysCall.start(storeWorker))
      }
    }
  }

  val api: LsuApiDecl = new LsuApiDecl {
    private val loadAcquireRefName = s"${name}_Load_AcquireSlot"
    private val storeAcquireRefName = s"${name}_Store_AcquireSlot"

    override def loadPath(): HwInline[Unit] = HwInline.thread(s"${name}_load_path") { t =>
      t.Step(loadAcquireRefName) {}

      t.Step(s"${name}_Load_PushReq") {
        SysCall.Inline(loadReqBuffer.push(launchLoadReqReg))
      }

      t.Step(s"${name}_Load_ReleaseSlot") {}
    }

    override def storePath(): HwInline[Unit] = HwInline.thread(s"${name}_store_path") { t =>
      t.Step(storeAcquireRefName) {}

      t.Step(s"${name}_Store_PushReq") {
        SysCall.Inline(storeReqBuffer.push(launchStoreReqReg))
      }

      t.Step(s"${name}_Store_ReleaseSlot") {}
    }

    override def loadWord(rd: UInt, addr: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_load_word") { t =>
      launchLoadReqReg.loadKind := LOAD_WORD
      launchLoadReqReg.rd := rd
      launchLoadReqReg.addr := addr
      launchLoadReqReg.unsigned := false.B
      t.jump(t.stepRef(loadAcquireRefName))
    }
    override def loadByte(rd: UInt, addr: UInt, unsigned: Bool): HwInline[Unit] = HwInline.atomic(s"${name}_load_byte") { t =>
      launchLoadReqReg.loadKind := LOAD_BYTE
      launchLoadReqReg.rd := rd
      launchLoadReqReg.addr := addr
      launchLoadReqReg.unsigned := unsigned
      t.jump(t.stepRef(loadAcquireRefName))
    }
    override def loadHalf(rd: UInt, addr: UInt, unsigned: Bool): HwInline[Unit] = HwInline.atomic(s"${name}_load_half") { t =>
      launchLoadReqReg.loadKind := LOAD_HALF
      launchLoadReqReg.rd := rd
      launchLoadReqReg.addr := addr
      launchLoadReqReg.unsigned := unsigned
      t.jump(t.stepRef(loadAcquireRefName))
    }

    override def storeWord(addr: UInt, data: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_store_word") { t =>
      launchStoreReqReg.storeKind := STORE_WORD
      launchStoreReqReg.addr := addr
      launchStoreReqReg.data := data
      t.jump(t.stepRef(storeAcquireRefName))
    }
    override def storeByte(addr: UInt, data: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_store_byte") { t =>
      launchStoreReqReg.storeKind := STORE_BYTE
      launchStoreReqReg.addr := addr
      launchStoreReqReg.data := data
      t.jump(t.stepRef(storeAcquireRefName))
    }
    override def storeHalf(addr: UInt, data: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_store_half") { t =>
      launchStoreReqReg.storeKind := STORE_HALF
      launchStoreReqReg.addr := addr
      launchStoreReqReg.data := data
      t.jump(t.stepRef(storeAcquireRefName))
    }
  }

  def RequestLsuApi(): HwInline[LsuApiDecl] = HwInline.bindings(s"${name}_lsu_api") { _ =>
    api
  }
}
