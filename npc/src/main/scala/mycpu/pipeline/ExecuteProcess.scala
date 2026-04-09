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

  final class MemReq extends Bundle {
    val isLoad = Bool()
    val kind = UInt(2.W)
    val rd = UInt(5.W)
    val base = UInt(XLEN.W)
    val offset = UInt(XLEN.W)
    val data = UInt(XLEN.W)
    val unsigned = Bool()
  }

  private val alu = spawn(new AluProcess("Alu"))
  private val csr = spawn(new CsrProcess("Csr"))
  private val memSlotLock = spawn(new MutexProcess(1, "MemSlotLock"))
  private val memReqBuffer = spawn(new PipelineBuffer(new MemReq, "MemReqBuffer"))
  private val memWorker = createThread("MemWorker")

  private val LOAD_WORD = 0.U(2.W)
  private val LOAD_BYTE = 1.U(2.W)
  private val LOAD_HALF = 2.U(2.W)

  private val STORE_WORD = 0.U(2.W)
  private val STORE_BYTE = 1.U(2.W)
  private val STORE_HALF = 2.U(2.W)

  private val memCompleted = RegInit(false.B)
  private val memReqReg = RegInit(0.U.asTypeOf(new MemReq))
  private val memAddrReg = RegInit(0.U(XLEN.W))
  private val launchMemReqReg = RegInit(0.U.asTypeOf(new MemReq))

  override def entry(): Unit = {
    memWorker.entry {
      val lsuApi = SysCall.Inline(RequestLsuApi())
      val aluApi = alu.api

      memWorker.Step("WaitReq") {
        memWorker.waitCondition(memReqBuffer.valid)
      }
      memWorker.Step("TakeReq") {
        memReqReg := SysCall.Inline(memReqBuffer.pop())
      }
      memWorker.Step("ComputeAddr") {
        val computedAddr = SysCall.Inline(aluApi.add(memReqReg.base, memReqReg.offset))
        memAddrReg := computedAddr
        when(memReqReg.isLoad) {
          memWorker.jump(memWorker.stepRef("LsuLoadDispatch"))
        }.otherwise {
          memWorker.jump(memWorker.stepRef("LsuStoreDispatch"))
        }
      }

      memWorker.Step("LsuLoadDispatch") {
        when(memReqReg.kind === LOAD_WORD) {
          SysCall.Inline(lsuApi.loadWord(memReqReg.rd, memAddrReg))
        }.elsewhen(memReqReg.kind === LOAD_BYTE) {
          SysCall.Inline(lsuApi.loadByte(memReqReg.rd, memAddrReg, memReqReg.unsigned))
        }.otherwise {
          SysCall.Inline(lsuApi.loadHalf(memReqReg.rd, memAddrReg, memReqReg.unsigned))
        }
      }
      SysCall.Inline(lsuApi.loadPath())
      memWorker.Step("AfterLoadPath") {
        memWorker.jump(memWorker.stepRef("AfterMem"))
      }

      memWorker.Step("LsuStoreDispatch") {
        when(memReqReg.kind === STORE_WORD) {
          SysCall.Inline(lsuApi.storeWord(memAddrReg, memReqReg.data))
        }.elsewhen(memReqReg.kind === STORE_BYTE) {
          SysCall.Inline(lsuApi.storeByte(memAddrReg, memReqReg.data))
        }.otherwise {
          SysCall.Inline(lsuApi.storeHalf(memAddrReg, memReqReg.data))
        }
      }
      SysCall.Inline(lsuApi.storePath())
      memWorker.Step("AfterStorePath") {
        memWorker.jump(memWorker.stepRef("AfterMem"))
      }

      memWorker.Step("AfterMem") {
        memCompleted := true.B
        memWorker.jump(memWorker.stepRef("WaitReq"))
      }
    }

    val daemon = createLogic("Daemon")
    daemon.run {
      when(!memWorker.active) {
        SysCall.Inline(SysCall.start(memWorker))
      }
    }
  }

  val api: ExecuteApiDecl = new ExecuteApiDecl {
    private def aluApi = alu.api
    private def csrApi = csr.api
    private def wbApi = writebackRef.get
    private val memAcquireRefName = s"${name}_Mem_AcquireSlot"
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

    def memPath(): HwInline[Unit] = HwInline.thread(s"${name}_mem_path") { t =>
      val lock = SysCall.Inline(memSlotLock.RequestLease(0))

      t.Step(memAcquireRefName) {
        SysCall.Inline(lock.Acquire())
        memCompleted := false.B
      }

      t.Step(s"${name}_Mem_PushReq") {
        printf(
          p"[EXEC] mem isLoad=${launchMemReqReg.isLoad} kind=${Decimal(launchMemReqReg.kind)} base=${Hexadecimal(launchMemReqReg.base)} offset=${Hexadecimal(launchMemReqReg.offset)} data=${Hexadecimal(launchMemReqReg.data)} unsigned=${launchMemReqReg.unsigned} rd=${Decimal(launchMemReqReg.rd)}\n",
        )
        SysCall.Inline(memReqBuffer.push(launchMemReqReg))
      }

      t.Step(s"${name}_Mem_WaitDone") {
        t.waitCondition(memCompleted)
      }

      t.Step(s"${name}_Mem_ReleaseSlot") {
        SysCall.Inline(lock.Release())
      }
    }

    def mem(isLoad: Bool, rd: UInt, base: UInt, offset: UInt, data: UInt, kind: UInt, unsigned: Bool): HwInline[Unit] =
      HwInline.atomic(s"${name}_mem") { t =>
        launchMemReqReg.isLoad := isLoad
        launchMemReqReg.kind := kind
        launchMemReqReg.rd := rd
        launchMemReqReg.base := base
        launchMemReqReg.offset := offset
        launchMemReqReg.data := data
        launchMemReqReg.unsigned := unsigned
        t.jump(t.stepRef(memAcquireRefName))
      }

    def load(rd: UInt, base: UInt, offset: UInt, kind: UInt, unsigned: Bool): HwInline[Unit] =
      HwInline.atomic(s"${name}_load") { t =>
        launchMemReqReg.isLoad := true.B
        launchMemReqReg.kind := kind
        launchMemReqReg.rd := rd
        launchMemReqReg.base := base
        launchMemReqReg.offset := offset
        launchMemReqReg.data := 0.U
        launchMemReqReg.unsigned := unsigned
        t.jump(t.stepRef(memAcquireRefName))
      }

    def store(base: UInt, offset: UInt, data: UInt, kind: UInt): HwInline[Unit] =
      HwInline.atomic(s"${name}_store") { t =>
        launchMemReqReg.isLoad := false.B
        launchMemReqReg.kind := kind
        launchMemReqReg.rd := 0.U
        launchMemReqReg.base := base
        launchMemReqReg.offset := offset
        launchMemReqReg.data := data
        launchMemReqReg.unsigned := false.B
        t.jump(t.stepRef(memAcquireRefName))
      }

    def loadWord(rd: UInt, base: UInt, offset: UInt): HwInline[Unit] =
      HwInline.atomic(s"${name}_load_word") { t =>
        launchMemReqReg.isLoad := true.B
        launchMemReqReg.kind := LOAD_WORD
        launchMemReqReg.rd := rd
        launchMemReqReg.base := base
        launchMemReqReg.offset := offset
        launchMemReqReg.data := 0.U
        launchMemReqReg.unsigned := false.B
        t.jump(t.stepRef(memAcquireRefName))
      }

    def storeWord(base: UInt, offset: UInt, data: UInt): HwInline[Unit] =
      HwInline.atomic(s"${name}_store_word") { t =>
        launchMemReqReg.isLoad := false.B
        launchMemReqReg.kind := STORE_WORD
        launchMemReqReg.rd := 0.U
        launchMemReqReg.base := base
        launchMemReqReg.offset := offset
        launchMemReqReg.data := data
        launchMemReqReg.unsigned := false.B
        t.jump(t.stepRef(memAcquireRefName))
      }

    def loadByte(rd: UInt, base: UInt, offset: UInt, unsigned: Bool): HwInline[Unit] =
      HwInline.atomic(s"${name}_load_byte") { t =>
        launchMemReqReg.isLoad := true.B
        launchMemReqReg.kind := LOAD_BYTE
        launchMemReqReg.rd := rd
        launchMemReqReg.base := base
        launchMemReqReg.offset := offset
        launchMemReqReg.data := 0.U
        launchMemReqReg.unsigned := unsigned
        t.jump(t.stepRef(memAcquireRefName))
      }

    def loadHalf(rd: UInt, base: UInt, offset: UInt, unsigned: Bool): HwInline[Unit] =
      HwInline.atomic(s"${name}_load_half") { t =>
        launchMemReqReg.isLoad := true.B
        launchMemReqReg.kind := LOAD_HALF
        launchMemReqReg.rd := rd
        launchMemReqReg.base := base
        launchMemReqReg.offset := offset
        launchMemReqReg.data := 0.U
        launchMemReqReg.unsigned := unsigned
        t.jump(t.stepRef(memAcquireRefName))
      }

    def storeByte(base: UInt, offset: UInt, data: UInt): HwInline[Unit] =
      HwInline.atomic(s"${name}_store_byte") { t =>
        launchMemReqReg.isLoad := false.B
        launchMemReqReg.kind := STORE_BYTE
        launchMemReqReg.rd := 0.U
        launchMemReqReg.base := base
        launchMemReqReg.offset := offset
        launchMemReqReg.data := data
        launchMemReqReg.unsigned := false.B
        t.jump(t.stepRef(memAcquireRefName))
      }

    def storeHalf(base: UInt, offset: UInt, data: UInt): HwInline[Unit] =
      HwInline.atomic(s"${name}_store_half") { t =>
        launchMemReqReg.isLoad := false.B
        launchMemReqReg.kind := STORE_HALF
        launchMemReqReg.rd := 0.U
        launchMemReqReg.base := base
        launchMemReqReg.offset := offset
        launchMemReqReg.data := data
        launchMemReqReg.unsigned := false.B
        t.jump(t.stepRef(memAcquireRefName))
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
