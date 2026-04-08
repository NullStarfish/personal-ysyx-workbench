package mycpu.pipeline

import HwOS.kernel._
import HwOS.stdlib.sync._
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

  private val loadSlotLock = spawn(new MutexProcess(1, "LoadSlotLock"))
  private val storeSlotLock = spawn(new MutexProcess(1, "StoreSlotLock"))
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

  private val loadCompleted = RegInit(false.B)
  private val storeCompleted = RegInit(false.B)

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
        loadCompleted := true.B
      }
      SysCall.Return()
    }

    storeWorker.entry {
      val memory = memoryRef.get
      val wbApi = writebackRef.get
      val reqReg = RegInit(0.U.asTypeOf(new StoreReq))

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
        storeCompleted := true.B
      }
      SysCall.Return()
    }

    val daemon = createLogic("Daemon")
    daemon.run {
      when(loadReqBuffer.valid && !loadWorker.active) {
        SysCall.Inline(SysCall.start(loadWorker))
      }
      when(storeReqBuffer.valid && !storeWorker.active) {
        SysCall.Inline(SysCall.start(storeWorker))
      }
    }
  }

  val api: LsuApiDecl = new LsuApiDecl {
    private def launchLoad(kind: UInt, rd: UInt, addr: UInt, unsigned: Bool): HwInline[Unit] = HwInline.thread(s"${name}_launch_load") { t =>
      val stepTag = s"${name}_load_${System.identityHashCode(new Object())}"
      val lock = SysCall.Inline(loadSlotLock.RequestLease(0))

      t.Step(s"${stepTag}_AcquireSlot") {
        SysCall.Inline(lock.Acquire())
        loadCompleted := false.B
      }

      t.Step(s"${stepTag}_PushReq") {
        SysCall.Inline(loadReqBuffer.pushAssign { req =>
          req.loadKind := kind
          req.rd := rd
          req.addr := addr
          req.unsigned := unsigned
        })
      }

      t.Step(s"${stepTag}_WaitDone") {
        t.waitCondition(loadCompleted)
      }

      t.Step(s"${stepTag}_ReleaseSlot") {
        SysCall.Inline(lock.Release())
        SysCall.Return()
      }
    }

    private def launchStore(kind: UInt, addr: UInt, data: UInt): HwInline[Unit] = HwInline.thread(s"${name}_launch_store") { t =>
      val stepTag = s"${name}_store_${System.identityHashCode(new Object())}"
      val lock = SysCall.Inline(storeSlotLock.RequestLease(0))

      t.Step(s"${stepTag}_AcquireSlot") {
        SysCall.Inline(lock.Acquire())
        storeCompleted := false.B
      }

      t.Step(s"${stepTag}_PushReq") {
        SysCall.Inline(storeReqBuffer.pushAssign { req =>
          req.storeKind := kind
          req.addr := addr
          req.data := data
        })
      }

      t.Step(s"${stepTag}_WaitDone") {
        t.waitCondition(storeCompleted)
      }

      t.Step(s"${stepTag}_ReleaseSlot") {
        SysCall.Inline(lock.Release())
        SysCall.Return()
      }
    }

    override def loadWord(rd: UInt, addr: UInt): HwInline[Unit] = launchLoad(LOAD_WORD, rd, addr, false.B)
    override def loadByte(rd: UInt, addr: UInt, unsigned: Bool): HwInline[Unit] = launchLoad(LOAD_BYTE, rd, addr, unsigned)
    override def loadHalf(rd: UInt, addr: UInt, unsigned: Bool): HwInline[Unit] = launchLoad(LOAD_HALF, rd, addr, unsigned)

    override def storeWord(addr: UInt, data: UInt): HwInline[Unit] = launchStore(STORE_WORD, addr, data)
    override def storeByte(addr: UInt, data: UInt): HwInline[Unit] = launchStore(STORE_BYTE, addr, data)
    override def storeHalf(addr: UInt, data: UInt): HwInline[Unit] = launchStore(STORE_HALF, addr, data)
  }

  def RequestLsuApi(): HwInline[LsuApiDecl] = HwInline.bindings(s"${name}_lsu_api") { _ =>
    api
  }
}
