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

  private val loadSlotLock = spawn(new MutexProcess(1, "LoadSlotLock"))
  private val storeSlotLock = spawn(new MutexProcess(1, "StoreSlotLock"))
  private val loadWorker = createThread("LoadWorker")
  private val storeWorker = createThread("StoreWorker")

  private val LOAD_WORD = 0.U(2.W)
  private val LOAD_BYTE = 1.U(2.W)
  private val LOAD_HALF = 2.U(2.W)

  private val STORE_WORD = 0.U(2.W)
  private val STORE_BYTE = 1.U(2.W)
  private val STORE_HALF = 2.U(2.W)

  private case class LoadSlot(
      loadKind: UInt,
      rd: UInt,
      addr: UInt,
      unsigned: Bool,
      pending: Bool,
      completed: Bool,
  )

  private case class StoreSlot(
      storeKind: UInt,
      addr: UInt,
      data: UInt,
      pending: Bool,
      completed: Bool,
  )

  private val loadSlot = LoadSlot(
    loadKind = RegInit(0.U(2.W)),
    rd = RegInit(0.U(5.W)),
    addr = RegInit(0.U(XLEN.W)),
    unsigned = RegInit(false.B),
    pending = RegInit(false.B),
    completed = RegInit(false.B),
  )

  private val storeSlot = StoreSlot(
    storeKind = RegInit(0.U(2.W)),
    addr = RegInit(0.U(XLEN.W)),
    data = RegInit(0.U(XLEN.W)),
    pending = RegInit(false.B),
    completed = RegInit(false.B),
  )

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
      val rawReadData = RegInit(0.U(XLEN.W))
      val alignedAddr = WireInit(Mux(
        loadSlot.loadKind === LOAD_WORD,
        loadSlot.addr,
        Cat(loadSlot.addr(XLEN - 1, 2), 0.U(2.W)),
      ))
      val loaded = SysCall.Inline(memory.read_once(alignedAddr, 2.U))
      loadWorker.Prev.edge.add {
        rawReadData := loaded
      }
      loadWorker.Step("Writeback") {
        val resultData = WireInit(rawReadData)
        when(loadSlot.loadKind === LOAD_BYTE) {
          resultData := extractByte(rawReadData, loadSlot.addr(1, 0), loadSlot.unsigned)
        }.elsewhen(loadSlot.loadKind === LOAD_HALF) {
          resultData := extractHalf(rawReadData, loadSlot.addr(1), loadSlot.unsigned)
        }
        SysCall.Inline(wbApi.writeReg(loadSlot.rd, resultData))
        loadSlot.completed := true.B
      }
      SysCall.Return()
    }

    storeWorker.entry {
      val memory = memoryRef.get
      val wbApi = writebackRef.get
      val writeData = WireInit(0.U(XLEN.W))
      val writeSize = WireInit(0.U(2.W))
      val writeStrb = WireInit(0.U(4.W))

      when(storeSlot.storeKind === STORE_WORD) {
        writeData := storeSlot.data
        writeSize := 2.U
        writeStrb := "b1111".U
      }.elsewhen(storeSlot.storeKind === STORE_BYTE) {
        writeData := (storeSlot.data(7, 0) << (storeSlot.addr(1, 0) << 3))(31, 0)
        writeSize := 0.U
        writeStrb := UIntToOH(storeSlot.addr(1, 0), 4)
      }.otherwise {
        writeData := Mux(storeSlot.addr(1), Cat(storeSlot.data(15, 0), 0.U(16.W)), storeSlot.data(15, 0))
        writeSize := 1.U
        writeStrb := Mux(storeSlot.addr(1), "b1100".U(4.W), "b0011".U(4.W))
      }

      SysCall.Inline(memory.write_once(storeSlot.addr, writeSize, writeData, writeStrb))
      storeWorker.Prev.edge.add {
        SysCall.Inline(wbApi.commit())
        storeSlot.completed := true.B
      }
      SysCall.Return()
    }

    val daemon = createLogic("Daemon")
    daemon.run {
      when(loadSlot.pending && !loadWorker.active) {
        loadSlot.pending := false.B
        SysCall.Inline(SysCall.start(loadWorker))
      }
      when(storeSlot.pending && !storeWorker.active) {
        storeSlot.pending := false.B
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
      }
      t.Prev.edge.add {
        loadSlot.loadKind := kind
        loadSlot.rd := rd
        loadSlot.addr := addr
        loadSlot.unsigned := unsigned
        loadSlot.pending := true.B
        loadSlot.completed := false.B
      }

      t.Step(s"${stepTag}_WaitDone") {
        t.waitCondition(loadSlot.completed)
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
      }
      t.Prev.edge.add {
        storeSlot.storeKind := kind
        storeSlot.addr := addr
        storeSlot.data := data
        storeSlot.pending := true.B
        storeSlot.completed := false.B
      }

      t.Step(s"${stepTag}_WaitDone") {
        t.waitCondition(storeSlot.completed)
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
