package mycpu.pipeline

import HwOS.kernel._
import HwOS.stdlib.sync._
import chisel3._
import mycpu.common._

final class ExecuteProcess(
    lsuRef: ApiRef[LsuApiDecl],
    writebackRef: ApiRef[WritebackApiDecl],
    localName: String = "Execute",
)(implicit kernel: Kernel)
    extends HwProcess(localName) {

  private val alu = spawn(new AluProcess("Alu"))
  private val csr = spawn(new CsrProcess("Csr"))
  private val loadSlotLock = spawn(new MutexProcess(1, "LoadSlotLock"))
  private val storeSlotLock = spawn(new MutexProcess(1, "StoreSlotLock"))
  private val loadWordWorker = createThread("LoadWordWorker")
  private val loadByteWorker = createThread("LoadByteWorker")
  private val loadHalfWorker = createThread("LoadHalfWorker")
  private val storeWordWorker = createThread("StoreWordWorker")
  private val storeByteWorker = createThread("StoreByteWorker")
  private val storeHalfWorker = createThread("StoreHalfWorker")

  private val LOAD_WORD = 0.U(2.W)
  private val LOAD_BYTE = 1.U(2.W)
  private val LOAD_HALF = 2.U(2.W)

  private val STORE_WORD = 0.U(2.W)
  private val STORE_BYTE = 1.U(2.W)
  private val STORE_HALF = 2.U(2.W)

  private case class LoadSlot(
      kind: UInt,
      rd: UInt,
      base: UInt,
      offset: UInt,
      unsigned: Bool,
      pending: Bool,
      issued: Bool,
      completed: Bool,
  )

  private case class StoreSlot(
      kind: UInt,
      base: UInt,
      offset: UInt,
      data: UInt,
      pending: Bool,
      issued: Bool,
      completed: Bool,
  )

  private val loadSlot = LoadSlot(
    kind = RegInit(0.U(2.W)),
    rd = RegInit(0.U(5.W)),
    base = RegInit(0.U(XLEN.W)),
    offset = RegInit(0.U(XLEN.W)),
    unsigned = RegInit(false.B),
    pending = RegInit(false.B),
    issued = RegInit(false.B),
    completed = RegInit(false.B),
  )

  private val storeSlot = StoreSlot(
    kind = RegInit(0.U(2.W)),
    base = RegInit(0.U(XLEN.W)),
    offset = RegInit(0.U(XLEN.W)),
    data = RegInit(0.U(XLEN.W)),
    pending = RegInit(false.B),
    issued = RegInit(false.B),
    completed = RegInit(false.B),
  )

  override def entry(): Unit = {
    loadWordWorker.entry {
      val lsuApi = SysCall.Inline(RequestLsuApi())
      val aluApi = alu.api
      val addrReg = RegInit(0.U(XLEN.W))
      loadWordWorker.Step("ComputeAddr") {
        addrReg := SysCall.Inline(aluApi.add(loadSlot.base, loadSlot.offset))
      }
      SysCall.Call(lsuApi.loadWord(loadSlot.rd, addrReg), "AfterLoadWord")
      loadWordWorker.Step("AfterLoadWord") {
        loadSlot.completed := true.B
        SysCall.Return()
      }
    }

    loadByteWorker.entry {
      val lsuApi = SysCall.Inline(RequestLsuApi())
      val aluApi = alu.api
      val addrReg = RegInit(0.U(XLEN.W))
      loadByteWorker.Step("ComputeAddr") {
        addrReg := SysCall.Inline(aluApi.add(loadSlot.base, loadSlot.offset))
      }
      SysCall.Call(lsuApi.loadByte(loadSlot.rd, addrReg, loadSlot.unsigned), "AfterLoadByte")
      loadByteWorker.Step("AfterLoadByte") {
        loadSlot.completed := true.B
        SysCall.Return()
      }
    }

    loadHalfWorker.entry {
      val lsuApi = SysCall.Inline(RequestLsuApi())
      val aluApi = alu.api
      val addrReg = RegInit(0.U(XLEN.W))
      loadHalfWorker.Step("ComputeAddr") {
        addrReg := SysCall.Inline(aluApi.add(loadSlot.base, loadSlot.offset))
      }
      SysCall.Call(lsuApi.loadHalf(loadSlot.rd, addrReg, loadSlot.unsigned), "AfterLoadHalf")
      loadHalfWorker.Step("AfterLoadHalf") {
        loadSlot.completed := true.B
        SysCall.Return()
      }
    }

    storeWordWorker.entry {
      val lsuApi = SysCall.Inline(RequestLsuApi())
      val aluApi = alu.api
      val addrReg = RegInit(0.U(XLEN.W))
      storeWordWorker.Step("ComputeAddr") {
        addrReg := SysCall.Inline(aluApi.add(storeSlot.base, storeSlot.offset))
      }
      SysCall.Call(lsuApi.storeWord(addrReg, storeSlot.data), "AfterStoreWord")
      storeWordWorker.Step("AfterStoreWord") {
        storeSlot.completed := true.B
        SysCall.Return()
      }
    }

    storeByteWorker.entry {
      val lsuApi = SysCall.Inline(RequestLsuApi())
      val aluApi = alu.api
      val addrReg = RegInit(0.U(XLEN.W))
      storeByteWorker.Step("ComputeAddr") {
        addrReg := SysCall.Inline(aluApi.add(storeSlot.base, storeSlot.offset))
      }
      SysCall.Call(lsuApi.storeByte(addrReg, storeSlot.data), "AfterStoreByte")
      storeByteWorker.Step("AfterStoreByte") {
        storeSlot.completed := true.B
        SysCall.Return()
      }
    }

    storeHalfWorker.entry {
      val lsuApi = SysCall.Inline(RequestLsuApi())
      val aluApi = alu.api
      val addrReg = RegInit(0.U(XLEN.W))
      storeHalfWorker.Step("ComputeAddr") {
        addrReg := SysCall.Inline(aluApi.add(storeSlot.base, storeSlot.offset))
      }
      SysCall.Call(lsuApi.storeHalf(addrReg, storeSlot.data), "AfterStoreHalf")
      storeHalfWorker.Step("AfterStoreHalf") {
        storeSlot.completed := true.B
        SysCall.Return()
      }
    }

    val daemon = createLogic("Daemon")
    daemon.run {
      when(loadSlot.pending && !loadWordWorker.active && !loadByteWorker.active && !loadHalfWorker.active) {
        loadSlot.pending := false.B
        when(loadSlot.kind === LOAD_WORD) {
          SysCall.Inline(SysCall.start(loadWordWorker))
        }.elsewhen(loadSlot.kind === LOAD_BYTE) {
          SysCall.Inline(SysCall.start(loadByteWorker))
        }.otherwise {
          SysCall.Inline(SysCall.start(loadHalfWorker))
        }
      }
      when(storeSlot.pending && !storeWordWorker.active && !storeByteWorker.active && !storeHalfWorker.active) {
        storeSlot.pending := false.B
        when(storeSlot.kind === STORE_WORD) {
          SysCall.Inline(SysCall.start(storeWordWorker))
        }.elsewhen(storeSlot.kind === STORE_BYTE) {
          SysCall.Inline(SysCall.start(storeByteWorker))
        }.otherwise {
          SysCall.Inline(SysCall.start(storeHalfWorker))
        }
      }
    }
  }

  val api: ExecuteApiDecl = new ExecuteApiDecl {
    private def aluApi = alu.api
    private def csrApi = csr.api
    private def wbApi = writebackRef.get

    private def writeComputedReg(opName: String, rd: UInt, result: UInt): Unit = {
      printf(p"[EXEC] ${opName} lhs-result write rd=${Decimal(rd)} data=${Hexadecimal(result)}\n")
      SysCall.Inline(wbApi.writeReg(rd, result))
    }

    def add(rd: UInt, lhs: UInt, rhs: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_add") { _ =>
      val result = SysCall.Inline(aluApi.add(lhs, rhs))
      writeComputedReg("add", rd, result)
    }

    def sub(rd: UInt, lhs: UInt, rhs: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_sub") { _ =>
      val result = SysCall.Inline(aluApi.sub(lhs, rhs))
      writeComputedReg("sub", rd, result)
    }

    def and(rd: UInt, lhs: UInt, rhs: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_and") { _ =>
      val result = SysCall.Inline(aluApi.and(lhs, rhs))
      writeComputedReg("and", rd, result)
    }

    def or(rd: UInt, lhs: UInt, rhs: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_or") { _ =>
      val result = SysCall.Inline(aluApi.or(lhs, rhs))
      writeComputedReg("or", rd, result)
    }

    def xor(rd: UInt, lhs: UInt, rhs: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_xor") { _ =>
      val result = SysCall.Inline(aluApi.xor(lhs, rhs))
      writeComputedReg("xor", rd, result)
    }

    def sll(rd: UInt, lhs: UInt, rhs: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_sll") { _ =>
      val result = SysCall.Inline(aluApi.sll(lhs, rhs))
      writeComputedReg("sll", rd, result)
    }

    def srl(rd: UInt, lhs: UInt, rhs: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_srl") { _ =>
      val result = SysCall.Inline(aluApi.srl(lhs, rhs))
      writeComputedReg("srl", rd, result)
    }

    def sra(rd: UInt, lhs: UInt, rhs: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_sra") { _ =>
      val result = SysCall.Inline(aluApi.sra(lhs, rhs))
      writeComputedReg("sra", rd, result)
    }

    def slt(rd: UInt, lhs: UInt, rhs: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_slt") { _ =>
      val result = SysCall.Inline(aluApi.slt(lhs, rhs))
      writeComputedReg("slt", rd, result)
    }

    def sltu(rd: UInt, lhs: UInt, rhs: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_sltu") { _ =>
      val result = SysCall.Inline(aluApi.sltu(lhs, rhs))
      writeComputedReg("sltu", rd, result)
    }

    def writeReg(rd: UInt, data: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_write_reg") { _ =>
      printf(p"[EXEC] writeReg rd=${Decimal(rd)} data=${Hexadecimal(data)}\n")
      SysCall.Inline(wbApi.writeReg(rd, data))
    }

    def redirect(nextPc: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_redirect") { _ =>
      printf(p"[EXEC] redirect nextPc=${Hexadecimal(nextPc)}\n")
      SysCall.Inline(wbApi.redirect(nextPc))
    }

    def redirectRelative(delta: SInt): HwInline[Unit] = HwInline.atomic(s"${name}_redirect_relative") { _ =>
      printf(p"[EXEC] redirectRelative delta=${Hexadecimal(delta.asUInt)}\n")
      SysCall.Inline(wbApi.redirectRelative(delta))
    }

    private def branchTarget(pc: UInt, offset: UInt): UInt = SysCall.Inline(aluApi.add(pc, offset))

    def eq(lhs: UInt, rhs: UInt, pc: UInt, offset: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_eq") { _ =>
      val result = SysCall.Inline(aluApi.eq(lhs, rhs))
      val target = branchTarget(pc, offset)
      printf(p"[EXEC] eq lhs=${Hexadecimal(lhs)} rhs=${Hexadecimal(rhs)} pc=${Hexadecimal(pc)} offset=${Hexadecimal(offset)} result=${result} target=${Hexadecimal(target)}\n")
      when(result) {
        SysCall.Inline(wbApi.redirect(target))
      }
    }

    def ne(lhs: UInt, rhs: UInt, pc: UInt, offset: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_ne") { _ =>
      val result = !SysCall.Inline(aluApi.eq(lhs, rhs))
      val target = branchTarget(pc, offset)
      printf(p"[EXEC] ne lhs=${Hexadecimal(lhs)} rhs=${Hexadecimal(rhs)} pc=${Hexadecimal(pc)} offset=${Hexadecimal(offset)} result=${result} target=${Hexadecimal(target)}\n")
      when(result) {
        SysCall.Inline(wbApi.redirect(target))
      }
    }

    def lt(lhs: UInt, rhs: UInt, pc: UInt, offset: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_lt") { _ =>
      val result = SysCall.Inline(aluApi.lt(lhs, rhs))
      val target = branchTarget(pc, offset)
      printf(p"[EXEC] lt lhs=${Hexadecimal(lhs)} rhs=${Hexadecimal(rhs)} pc=${Hexadecimal(pc)} offset=${Hexadecimal(offset)} result=${result} target=${Hexadecimal(target)}\n")
      when(result) {
        SysCall.Inline(wbApi.redirect(target))
      }
    }

    def ltu(lhs: UInt, rhs: UInt, pc: UInt, offset: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_ltu") { _ =>
      val result = SysCall.Inline(aluApi.ltu(lhs, rhs))
      val target = branchTarget(pc, offset)
      printf(p"[EXEC] ltu lhs=${Hexadecimal(lhs)} rhs=${Hexadecimal(rhs)} pc=${Hexadecimal(pc)} offset=${Hexadecimal(offset)} result=${result} target=${Hexadecimal(target)}\n")
      when(result) {
        SysCall.Inline(wbApi.redirect(target))
      }
    }

    def ge(lhs: UInt, rhs: UInt, pc: UInt, offset: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_ge") { _ =>
      val result = !SysCall.Inline(aluApi.lt(lhs, rhs))
      val target = branchTarget(pc, offset)
      printf(p"[EXEC] ge lhs=${Hexadecimal(lhs)} rhs=${Hexadecimal(rhs)} pc=${Hexadecimal(pc)} offset=${Hexadecimal(offset)} result=${result} target=${Hexadecimal(target)}\n")
      when(result) {
        SysCall.Inline(wbApi.redirect(target))
      }
    }

    def geu(lhs: UInt, rhs: UInt, pc: UInt, offset: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_geu") { _ =>
      val result = !SysCall.Inline(aluApi.ltu(lhs, rhs))
      val target = branchTarget(pc, offset)
      printf(p"[EXEC] geu lhs=${Hexadecimal(lhs)} rhs=${Hexadecimal(rhs)} pc=${Hexadecimal(pc)} offset=${Hexadecimal(offset)} result=${result} target=${Hexadecimal(target)}\n")
      when(result) {
        SysCall.Inline(wbApi.redirect(target))
      }
    }

    def loadWord(rd: UInt, base: UInt, offset: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_load_word") { t =>
      val lock = SysCall.Inline(loadSlotLock.RequestLease(0))
      SysCall.Inline(lock.Acquire())
      when(!loadSlot.issued) {
        printf(p"[EXEC] loadWord base=${Hexadecimal(base)} offset=${Hexadecimal(offset)} rd=${Decimal(rd)}\n")
        loadSlot.kind := LOAD_WORD
        loadSlot.rd := rd
        loadSlot.base := base
        loadSlot.offset := offset
        loadSlot.unsigned := false.B
        loadSlot.pending := true.B
        loadSlot.issued := true.B
        loadSlot.completed := false.B
      }
      t.waitCondition(loadSlot.completed)
      when(loadSlot.completed) {
        loadSlot.issued := false.B
        SysCall.Inline(lock.Release())
      }
    }

    def storeWord(base: UInt, offset: UInt, data: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_store_word") { t =>
      val lock = SysCall.Inline(storeSlotLock.RequestLease(0))
      SysCall.Inline(lock.Acquire())
      when(!storeSlot.issued) {
        printf(p"[EXEC] storeWord base=${Hexadecimal(base)} offset=${Hexadecimal(offset)} data=${Hexadecimal(data)}\n")
        storeSlot.kind := STORE_WORD
        storeSlot.base := base
        storeSlot.offset := offset
        storeSlot.data := data
        storeSlot.pending := true.B
        storeSlot.issued := true.B
        storeSlot.completed := false.B
      }
      t.waitCondition(storeSlot.completed)
      when(storeSlot.completed) {
        storeSlot.issued := false.B
        SysCall.Inline(lock.Release())
      }
    }

    def loadByte(rd: UInt, base: UInt, offset: UInt, unsigned: Bool): HwInline[Unit] = HwInline.atomic(s"${name}_load_byte") { t =>
      val lock = SysCall.Inline(loadSlotLock.RequestLease(0))
      SysCall.Inline(lock.Acquire())
      when(!loadSlot.issued) {
        printf(p"[EXEC] loadByte base=${Hexadecimal(base)} offset=${Hexadecimal(offset)} unsigned=${unsigned} rd=${Decimal(rd)}\n")
        loadSlot.kind := LOAD_BYTE
        loadSlot.rd := rd
        loadSlot.base := base
        loadSlot.offset := offset
        loadSlot.unsigned := unsigned
        loadSlot.pending := true.B
        loadSlot.issued := true.B
        loadSlot.completed := false.B
      }
      t.waitCondition(loadSlot.completed)
      when(loadSlot.completed) {
        loadSlot.issued := false.B
        SysCall.Inline(lock.Release())
      }
    }

    def loadHalf(rd: UInt, base: UInt, offset: UInt, unsigned: Bool): HwInline[Unit] = HwInline.atomic(s"${name}_load_half") { t =>
      val lock = SysCall.Inline(loadSlotLock.RequestLease(0))
      SysCall.Inline(lock.Acquire())
      when(!loadSlot.issued) {
        printf(p"[EXEC] loadHalf base=${Hexadecimal(base)} offset=${Hexadecimal(offset)} unsigned=${unsigned} rd=${Decimal(rd)}\n")
        loadSlot.kind := LOAD_HALF
        loadSlot.rd := rd
        loadSlot.base := base
        loadSlot.offset := offset
        loadSlot.unsigned := unsigned
        loadSlot.pending := true.B
        loadSlot.issued := true.B
        loadSlot.completed := false.B
      }
      t.waitCondition(loadSlot.completed)
      when(loadSlot.completed) {
        loadSlot.issued := false.B
        SysCall.Inline(lock.Release())
      }
    }

    def storeByte(base: UInt, offset: UInt, data: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_store_byte") { t =>
      val lock = SysCall.Inline(storeSlotLock.RequestLease(0))
      SysCall.Inline(lock.Acquire())
      when(!storeSlot.issued) {
        printf(p"[EXEC] storeByte base=${Hexadecimal(base)} offset=${Hexadecimal(offset)} data=${Hexadecimal(data)}\n")
        storeSlot.kind := STORE_BYTE
        storeSlot.base := base
        storeSlot.offset := offset
        storeSlot.data := data
        storeSlot.pending := true.B
        storeSlot.issued := true.B
        storeSlot.completed := false.B
      }
      t.waitCondition(storeSlot.completed)
      when(storeSlot.completed) {
        storeSlot.issued := false.B
        SysCall.Inline(lock.Release())
      }
    }

    def storeHalf(base: UInt, offset: UInt, data: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_store_half") { t =>
      val lock = SysCall.Inline(storeSlotLock.RequestLease(0))
      SysCall.Inline(lock.Acquire())
      when(!storeSlot.issued) {
        printf(p"[EXEC] storeHalf base=${Hexadecimal(base)} offset=${Hexadecimal(offset)} data=${Hexadecimal(data)}\n")
        storeSlot.kind := STORE_HALF
        storeSlot.base := base
        storeSlot.offset := offset
        storeSlot.data := data
        storeSlot.pending := true.B
        storeSlot.issued := true.B
        storeSlot.completed := false.B
      }
      t.waitCondition(storeSlot.completed)
      when(storeSlot.completed) {
        storeSlot.issued := false.B
        SysCall.Inline(lock.Release())
      }
    }

    def auipc(rd: UInt, pc: UInt, imm: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_auipc") { _ =>
      val result = SysCall.Inline(aluApi.add(pc, imm))
      printf(p"[EXEC] auipc pc=${Hexadecimal(pc)} imm=${Hexadecimal(imm)} rd=${Decimal(rd)} result=${Hexadecimal(result)}\n")
      SysCall.Inline(wbApi.writeReg(rd, result))
    }

    def jal(rd: UInt, pc: UInt, offset: SInt): HwInline[Unit] = HwInline.atomic(s"${name}_jal") { _ =>
      val ret = SysCall.Inline(aluApi.add(pc, 4.U(XLEN.W)))
      val target = (pc.asSInt + offset).asUInt
      printf(p"[EXEC] jal pc=${Hexadecimal(pc)} offset=${Hexadecimal(offset.asUInt)} rd=${Decimal(rd)} ret=${Hexadecimal(ret)} target=${Hexadecimal(target)}\n")
      SysCall.Inline(wbApi.writeReg(rd, ret))
      SysCall.Inline(wbApi.redirect(target))
    }

    def jalr(rd: UInt, pc: UInt, base: UInt, offset: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_jalr") { _ =>
      val ret = SysCall.Inline(aluApi.add(pc, 4.U(XLEN.W)))
      val rawTarget = SysCall.Inline(aluApi.add(base, offset))
      val target = rawTarget & (~1.U(XLEN.W))
      printf(p"[EXEC] jalr pc=${Hexadecimal(pc)} base=${Hexadecimal(base)} offset=${Hexadecimal(offset)} rd=${Decimal(rd)} ret=${Hexadecimal(ret)} target=${Hexadecimal(target)}\n")
      SysCall.Inline(wbApi.writeReg(rd, ret))
      SysCall.Inline(wbApi.redirect(target))
    }

    def csrRw(rd: UInt, addr: UInt, src: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_csr_rw") { _ =>
      val oldValue = SysCall.Inline(csrApi.rw(addr, src))
      printf(p"[EXEC] csrRw rd=${Decimal(rd)} addr=${Hexadecimal(addr)} src=${Hexadecimal(src)} old=${Hexadecimal(oldValue)}\n")
      SysCall.Inline(wbApi.writeReg(rd, oldValue))
    }

    def csrRs(rd: UInt, addr: UInt, src: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_csr_rs") { _ =>
      val oldValue = SysCall.Inline(csrApi.rs(addr, src))
      printf(p"[EXEC] csrRs rd=${Decimal(rd)} addr=${Hexadecimal(addr)} src=${Hexadecimal(src)} old=${Hexadecimal(oldValue)}\n")
      SysCall.Inline(wbApi.writeReg(rd, oldValue))
    }

    def csrRc(rd: UInt, addr: UInt, src: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_csr_rc") { _ =>
      val oldValue = SysCall.Inline(csrApi.rc(addr, src))
      printf(p"[EXEC] csrRc rd=${Decimal(rd)} addr=${Hexadecimal(addr)} src=${Hexadecimal(src)} old=${Hexadecimal(oldValue)}\n")
      SysCall.Inline(wbApi.writeReg(rd, oldValue))
    }
  }

  def RequestExecuteApi(): HwInline[ExecuteApiDecl] = HwInline.bindings(s"${name}_execute_api") { _ =>
    api
  }

  val csrProbeApi: CsrProbeApiDecl = csr.probeApi

  private def RequestLsuApi(): HwInline[LsuApiDecl] = HwInline.bindings(s"${name}_lsu_api") { _ =>
    lsuRef.get
  }

}
