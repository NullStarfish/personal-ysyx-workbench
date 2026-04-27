package mycpu.pipeline

import HwOS.kernel._
import chisel3._
import chisel3.util._
import mycpu.common._

final class ExecuteProcess(
    lsuRef: ApiRef[LsuApiDecl],
    writebackRef: ApiRef[WritebackApiDecl],
    hazardRef: ApiRef[ControlHazardApiDecl],
    traceRef: ApiRef[TraceApiDecl],
    localName: String = "Execute",
)(implicit kernel: Kernel)
    extends HwProcess(localName) {

  private val EXEC_FAMILY_ALU = 0.U(3.W)
  private val EXEC_FAMILY_WB_MISC = 1.U(3.W)
  private val EXEC_FAMILY_BRANCH = 2.U(3.W)
  private val EXEC_FAMILY_MEM = 3.U(3.W)
  private val EXEC_FAMILY_UPPER = 4.U(3.W)
  private val EXEC_FAMILY_JUMP = 5.U(3.W)
  private val EXEC_FAMILY_CSR = 6.U(3.W)

  private val EXEC_OP_ADD = 0.U(4.W)
  private val EXEC_OP_SUB = 1.U(4.W)
  private val EXEC_OP_AND = 2.U(4.W)
  private val EXEC_OP_OR = 3.U(4.W)
  private val EXEC_OP_XOR = 4.U(4.W)
  private val EXEC_OP_SLL = 5.U(4.W)
  private val EXEC_OP_SRL = 6.U(4.W)
  private val EXEC_OP_SRA = 7.U(4.W)
  private val EXEC_OP_SLT = 8.U(4.W)
  private val EXEC_OP_SLTU = 9.U(4.W)

  private val EXEC_OP_WRITE_REG = 0.U(4.W)
  private val EXEC_OP_REDIRECT = 1.U(4.W)
  private val EXEC_OP_REDIRECT_REL = 2.U(4.W)

  private val EXEC_OP_BRANCH_EQ = 0.U(4.W)
  private val EXEC_OP_BRANCH_NE = 1.U(4.W)
  private val EXEC_OP_BRANCH_LT = 2.U(4.W)
  private val EXEC_OP_BRANCH_LTU = 3.U(4.W)
  private val EXEC_OP_BRANCH_GE = 4.U(4.W)
  private val EXEC_OP_BRANCH_GEU = 5.U(4.W)

  private val EXEC_OP_MEM_LOAD = 0.U(4.W)
  private val EXEC_OP_MEM_STORE = 1.U(4.W)
  private val EXEC_SUBOP_WORD = 0.U(3.W)
  private val EXEC_SUBOP_BYTE = 1.U(3.W)
  private val EXEC_SUBOP_HALF = 2.U(3.W)

  private val EXEC_OP_UPPER_LUI = 0.U(4.W)
  private val EXEC_OP_UPPER_AUIPC = 1.U(4.W)

  private val EXEC_OP_JAL = 0.U(4.W)
  private val EXEC_OP_JALR = 1.U(4.W)

  private val EXEC_OP_CSR_RW = 0.U(4.W)
  private val EXEC_OP_CSR_RS = 1.U(4.W)
  private val EXEC_OP_CSR_RC = 2.U(4.W)

  final class ExecuteReq extends Bundle {
    val family = UInt(3.W)
    val op = UInt(4.W)
    val subop = UInt(3.W)
    val rd = UInt(5.W)
    val wbToken = UInt(4.W)
    val src1 = UInt(XLEN.W)
    val src2 = UInt(XLEN.W)
    val pc = UInt(XLEN.W)
    val imm = UInt(XLEN.W)
    val data = UInt(XLEN.W)
    val csrAddr = UInt(12.W)
    val unsigned = Bool()
  }

  private val alu = spawn(new AluProcess("Alu"))
  private val csr = spawn(new CsrProcess("Csr"))
  private val executeReqBuffer = spawn(new PipelineBuffer(new ExecuteReq, "ExecuteReqBuffer"))
  private val executeWorker = createThread("ExecuteWorker")

  private val launchExecReqReg = RegInit(0.U.asTypeOf(new ExecuteReq))
  private val execReqReg = RegInit(0.U.asTypeOf(new ExecuteReq))
  private val computeResultReg = RegInit(0.U(XLEN.W))
  private val computeTargetReg = RegInit(0.U(XLEN.W))
  private val computeRelativeDeltaReg = RegInit(0.S(XLEN.W))
  private val computeDoWriteReg = RegInit(false.B)
  private val computeDoHazardRedirect = RegInit(false.B)
  private val computeDoHazardRedirectRelative = RegInit(false.B)
  private val computeDoHazardRedirectNoCommit = RegInit(false.B)
  private val computeDoTraceCommit = RegInit(false.B)
  private val execAcquireRefName = s"${name}_Exec_Acquire"

  override def entry(): Unit = {
    executeWorker.entry {
      val aluApi = alu.api
      val csrApi = csr.api
      val lsuApi = SysCall.Inline(writeBoundLsuApi())
      val wbApi = SysCall.Inline(writeBoundWritebackApi())
      val hazardApi = SysCall.Inline(writeBoundHazardApi())
      val traceApi = SysCall.Inline(writeBoundTraceApi())

      executeWorker.Step("WaitReq") {
        executeWorker.waitCondition(executeReqBuffer.valid)
      }

      executeWorker.Step("TakeReq") {
        execReqReg := SysCall.Inline(executeReqBuffer.pop())
      }

      executeWorker.Step("Dispatch") {
        switch(execReqReg.family) {
          is(EXEC_FAMILY_ALU) { executeWorker.jump(executeWorker.stepRef("ExecuteWorker_Compute")) }
          is(EXEC_FAMILY_WB_MISC) { executeWorker.jump(executeWorker.stepRef("ExecuteWorker_Compute")) }
          is(EXEC_FAMILY_BRANCH) { executeWorker.jump(executeWorker.stepRef("ExecuteWorker_Compute")) }
          is(EXEC_FAMILY_MEM) { executeWorker.jump(executeWorker.stepRef("ExecuteWorker_MemLaunch")) }
          is(EXEC_FAMILY_UPPER) { executeWorker.jump(executeWorker.stepRef("ExecuteWorker_Compute")) }
          is(EXEC_FAMILY_JUMP) { executeWorker.jump(executeWorker.stepRef("ExecuteWorker_Compute")) }
          is(EXEC_FAMILY_CSR) { executeWorker.jump(executeWorker.stepRef("ExecuteWorker_Compute")) }
        }
      }

      def issueWriteReg(result: UInt): Unit = {
        SysCall.Inline(wbApi.writeReg(execReqReg.wbToken, result))
      }

      def issueHazardRedirect(nextPc: UInt): Unit = {
        SysCall.Inline(hazardApi.redirect(nextPc))
      }

      def issueHazardRedirectRelative(delta: SInt): Unit = {
        SysCall.Inline(hazardApi.redirectRelative(delta))
      }

      def issueHazardRedirectNoCommit(nextPc: UInt): Unit = {
        SysCall.Inline(hazardApi.redirectNoCommit(nextPc))
      }

      def branchTarget: UInt = SysCall.Inline(aluApi.add(execReqReg.pc, execReqReg.imm))

      executeWorker.Step("ExecuteWorker_Compute") {
        val result = WireDefault(0.U(XLEN.W))
        val taken = WireDefault(false.B)
        val jumpTarget = WireDefault(0.U(XLEN.W))
        val relativeDelta = WireDefault(0.S(XLEN.W))
        val doWriteReg = WireDefault(false.B)
        val doHazardRedirect = WireDefault(false.B)
        val doHazardRedirectRelative = WireDefault(false.B)
        val doHazardRedirectNoCommit = WireDefault(false.B)
        val doTraceCommit = WireDefault(false.B)

        switch(execReqReg.family) {
          is(EXEC_FAMILY_ALU) {
            switch(execReqReg.op) {
              is(EXEC_OP_ADD) { result := SysCall.Inline(aluApi.add(execReqReg.src1, execReqReg.src2)) }
              is(EXEC_OP_SUB) { result := SysCall.Inline(aluApi.sub(execReqReg.src1, execReqReg.src2)) }
              is(EXEC_OP_AND) { result := SysCall.Inline(aluApi.and(execReqReg.src1, execReqReg.src2)) }
              is(EXEC_OP_OR) { result := SysCall.Inline(aluApi.or(execReqReg.src1, execReqReg.src2)) }
              is(EXEC_OP_XOR) { result := SysCall.Inline(aluApi.xor(execReqReg.src1, execReqReg.src2)) }
              is(EXEC_OP_SLL) { result := SysCall.Inline(aluApi.sll(execReqReg.src1, execReqReg.src2)) }
              is(EXEC_OP_SRL) { result := SysCall.Inline(aluApi.srl(execReqReg.src1, execReqReg.src2)) }
              is(EXEC_OP_SRA) { result := SysCall.Inline(aluApi.sra(execReqReg.src1, execReqReg.src2)) }
              is(EXEC_OP_SLT) { result := SysCall.Inline(aluApi.slt(execReqReg.src1, execReqReg.src2)) }
              is(EXEC_OP_SLTU) { result := SysCall.Inline(aluApi.sltu(execReqReg.src1, execReqReg.src2)) }
            }
            when(execReqReg.op === EXEC_OP_ADD) {
              printf(p"[EXEC] add lhs=${Hexadecimal(execReqReg.src1)} rhs=${Hexadecimal(execReqReg.src2)} result=${Hexadecimal(result)}\n")
            }
            doWriteReg := true.B
          }
          is(EXEC_FAMILY_BRANCH) {
            switch(execReqReg.op) {
              is(EXEC_OP_BRANCH_EQ) { taken := execReqReg.src1 === execReqReg.src2 }
              is(EXEC_OP_BRANCH_NE) { taken := execReqReg.src1 =/= execReqReg.src2 }
              is(EXEC_OP_BRANCH_LT) { taken := execReqReg.src1.asSInt < execReqReg.src2.asSInt }
              is(EXEC_OP_BRANCH_LTU) { taken := execReqReg.src1 < execReqReg.src2 }
              is(EXEC_OP_BRANCH_GE) { taken := execReqReg.src1.asSInt >= execReqReg.src2.asSInt }
              is(EXEC_OP_BRANCH_GEU) { taken := execReqReg.src1 >= execReqReg.src2 }
            }
            when(taken) {
              jumpTarget := branchTarget
              doHazardRedirect := true.B
            }.otherwise {
              doTraceCommit := true.B
            }
          }
          is(EXEC_FAMILY_UPPER) {
            result := execReqReg.imm
            when(execReqReg.op === EXEC_OP_UPPER_AUIPC) {
              result := SysCall.Inline(aluApi.add(execReqReg.pc, execReqReg.imm))
            }
            doWriteReg := true.B
          }
          is(EXEC_FAMILY_JUMP) {
            result := execReqReg.pc + 4.U(XLEN.W)
            when(execReqReg.op === EXEC_OP_JAL) {
              jumpTarget := (execReqReg.pc.asSInt + execReqReg.imm.asSInt).asUInt
            }.otherwise {
              val rawTarget = SysCall.Inline(aluApi.add(execReqReg.src1, execReqReg.src2))
              jumpTarget := rawTarget & (~1.U(XLEN.W))
            }
            doWriteReg := true.B
            doHazardRedirectNoCommit := true.B
          }
          is(EXEC_FAMILY_CSR) {
            switch(execReqReg.op) {
              is(EXEC_OP_CSR_RW) { result := SysCall.Inline(csrApi.rw(execReqReg.csrAddr, execReqReg.data)) }
              is(EXEC_OP_CSR_RS) { result := SysCall.Inline(csrApi.rs(execReqReg.csrAddr, execReqReg.data)) }
              is(EXEC_OP_CSR_RC) { result := SysCall.Inline(csrApi.rc(execReqReg.csrAddr, execReqReg.data)) }
            }
            doWriteReg := true.B
          }
          is(EXEC_FAMILY_WB_MISC) {
            switch(execReqReg.op) {
              is(EXEC_OP_WRITE_REG) {
                result := execReqReg.data
                doWriteReg := true.B
              }
              is(EXEC_OP_REDIRECT) {
                jumpTarget := execReqReg.data
                doHazardRedirect := true.B
              }
              is(EXEC_OP_REDIRECT_REL) {
                relativeDelta := execReqReg.imm.asSInt
                doHazardRedirectRelative := true.B
              }
            }
          }
        }
        computeResultReg := result
        computeTargetReg := jumpTarget
        computeRelativeDeltaReg := relativeDelta
        computeDoWriteReg := doWriteReg
        computeDoHazardRedirect := doHazardRedirect
        computeDoHazardRedirectRelative := doHazardRedirectRelative
        computeDoHazardRedirectNoCommit := doHazardRedirectNoCommit
        computeDoTraceCommit := doTraceCommit
        executeWorker.jump(executeWorker.stepRef("ExecuteWorker_Apply"))
      }

      executeWorker.Step("ExecuteWorker_Apply") {
        when(computeDoWriteReg) {
          issueWriteReg(computeResultReg)
        }
        when(computeDoHazardRedirect) {
          issueHazardRedirect(computeTargetReg)
        }
        when(computeDoHazardRedirectRelative) {
          issueHazardRedirectRelative(computeRelativeDeltaReg)
        }
        when(computeDoHazardRedirectNoCommit) {
          issueHazardRedirectNoCommit(computeTargetReg)
        }
        when(computeDoTraceCommit) {
          SysCall.Inline(traceApi.commit())
        }
        executeWorker.jump(executeWorker.stepRef("WaitReq"))
      }

      executeWorker.Step("ExecuteWorker_MemLaunch") {
        val addr = SysCall.Inline(aluApi.add(execReqReg.src1, execReqReg.src2))
        when(execReqReg.op === EXEC_OP_MEM_LOAD) {
          switch(execReqReg.subop) {
            is(EXEC_SUBOP_WORD) { SysCall.Inline(lsuApi.loadWord(execReqReg.wbToken, addr)) }
            is(EXEC_SUBOP_BYTE) { SysCall.Inline(lsuApi.loadByte(execReqReg.wbToken, addr, execReqReg.unsigned)) }
            is(EXEC_SUBOP_HALF) { SysCall.Inline(lsuApi.loadHalf(execReqReg.wbToken, addr, execReqReg.unsigned)) }
          }
          executeWorker.jump(executeWorker.stepRef("ExecuteWorker_MemLoadPath"))
        }.otherwise {
          switch(execReqReg.subop) {
            is(EXEC_SUBOP_WORD) { SysCall.Inline(lsuApi.storeWord(addr, execReqReg.data)) }
            is(EXEC_SUBOP_BYTE) { SysCall.Inline(lsuApi.storeByte(addr, execReqReg.data)) }
            is(EXEC_SUBOP_HALF) { SysCall.Inline(lsuApi.storeHalf(addr, execReqReg.data)) }
          }
          executeWorker.jump(executeWorker.stepRef("ExecuteWorker_MemStorePath"))
        }
      }
      executeWorker.Step("ExecuteWorker_MemLoadPath") {}
      SysCall.Inline(lsuApi.loadPath())
      executeWorker.Step("ExecuteWorker_MemLoadDone") { executeWorker.jump(executeWorker.stepRef("WaitReq")) }
      executeWorker.Step("ExecuteWorker_MemStorePath") {}
      SysCall.Inline(lsuApi.storePath())
      executeWorker.Step("ExecuteWorker_MemStoreDone") { executeWorker.jump(executeWorker.stepRef("WaitReq")) }

    }

    val daemon = createLogic("Daemon")
    daemon.run {
      when(!executeWorker.active) {
        SysCall.Inline(SysCall.start(executeWorker))
      }
    }
  }

  private def writeBoundTraceApi(): HwInline[TraceApiDecl] = HwInline.bindings(s"${name}_trace_link") { _ =>
    traceRef.get
  }

  val api: ExecuteApiDecl = new ExecuteApiDecl {
    override def execPath(): HwInline[Unit] = HwInline.thread(s"${name}_exec_path") { t =>
      t.Step(execAcquireRefName) {}
      t.Step(s"${name}_Exec_PushReq") {
        SysCall.Inline(executeReqBuffer.push(launchExecReqReg))
      }
      t.Step(s"${name}_Exec_Release") {}
    }

    override def memPath(): HwInline[Unit] = execPath()

    private def enqueue(
        tag: String,
        family: UInt,
        op: UInt,
        subop: UInt = 0.U,
        rd: UInt = 0.U,
        wbToken: UInt = 0.U,
        src1: UInt = 0.U,
        src2: UInt = 0.U,
        pc: UInt = 0.U,
        imm: UInt = 0.U,
        data: UInt = 0.U,
        csrAddr: UInt = 0.U,
        unsigned: Bool = false.B,
    ): HwInline[Unit] = HwInline.atomic(tag) { t =>
      launchExecReqReg.family := family
      launchExecReqReg.op := op
      launchExecReqReg.subop := subop
      launchExecReqReg.rd := rd
      launchExecReqReg.wbToken := wbToken
      launchExecReqReg.src1 := src1
      launchExecReqReg.src2 := src2
      launchExecReqReg.pc := pc
      launchExecReqReg.imm := imm
      launchExecReqReg.data := data
      launchExecReqReg.csrAddr := csrAddr
      launchExecReqReg.unsigned := unsigned
      t.jump(t.stepRef(execAcquireRefName))
    }

    override def add(rd: UInt, wbToken: UInt, lhs: UInt, rhs: UInt): HwInline[Unit] =
      enqueue(s"${name}_enqueue_add", EXEC_FAMILY_ALU, EXEC_OP_ADD, rd = rd, wbToken = wbToken, src1 = lhs, src2 = rhs)
    override def sub(rd: UInt, wbToken: UInt, lhs: UInt, rhs: UInt): HwInline[Unit] =
      enqueue(s"${name}_enqueue_sub", EXEC_FAMILY_ALU, EXEC_OP_SUB, rd = rd, wbToken = wbToken, src1 = lhs, src2 = rhs)
    override def and(rd: UInt, wbToken: UInt, lhs: UInt, rhs: UInt): HwInline[Unit] =
      enqueue(s"${name}_enqueue_and", EXEC_FAMILY_ALU, EXEC_OP_AND, rd = rd, wbToken = wbToken, src1 = lhs, src2 = rhs)
    override def or(rd: UInt, wbToken: UInt, lhs: UInt, rhs: UInt): HwInline[Unit] =
      enqueue(s"${name}_enqueue_or", EXEC_FAMILY_ALU, EXEC_OP_OR, rd = rd, wbToken = wbToken, src1 = lhs, src2 = rhs)
    override def xor(rd: UInt, wbToken: UInt, lhs: UInt, rhs: UInt): HwInline[Unit] =
      enqueue(s"${name}_enqueue_xor", EXEC_FAMILY_ALU, EXEC_OP_XOR, rd = rd, wbToken = wbToken, src1 = lhs, src2 = rhs)
    override def sll(rd: UInt, wbToken: UInt, lhs: UInt, rhs: UInt): HwInline[Unit] =
      enqueue(s"${name}_enqueue_sll", EXEC_FAMILY_ALU, EXEC_OP_SLL, rd = rd, wbToken = wbToken, src1 = lhs, src2 = rhs)
    override def srl(rd: UInt, wbToken: UInt, lhs: UInt, rhs: UInt): HwInline[Unit] =
      enqueue(s"${name}_enqueue_srl", EXEC_FAMILY_ALU, EXEC_OP_SRL, rd = rd, wbToken = wbToken, src1 = lhs, src2 = rhs)
    override def sra(rd: UInt, wbToken: UInt, lhs: UInt, rhs: UInt): HwInline[Unit] =
      enqueue(s"${name}_enqueue_sra", EXEC_FAMILY_ALU, EXEC_OP_SRA, rd = rd, wbToken = wbToken, src1 = lhs, src2 = rhs)
    override def slt(rd: UInt, wbToken: UInt, lhs: UInt, rhs: UInt): HwInline[Unit] =
      enqueue(s"${name}_enqueue_slt", EXEC_FAMILY_ALU, EXEC_OP_SLT, rd = rd, wbToken = wbToken, src1 = lhs, src2 = rhs)
    override def sltu(rd: UInt, wbToken: UInt, lhs: UInt, rhs: UInt): HwInline[Unit] =
      enqueue(s"${name}_enqueue_sltu", EXEC_FAMILY_ALU, EXEC_OP_SLTU, rd = rd, wbToken = wbToken, src1 = lhs, src2 = rhs)
    override def writeReg(rd: UInt, wbToken: UInt, data: UInt): HwInline[Unit] =
      enqueue(s"${name}_enqueue_write_reg", EXEC_FAMILY_WB_MISC, EXEC_OP_WRITE_REG, rd = rd, wbToken = wbToken, data = data)
    override def redirect(nextPc: UInt): HwInline[Unit] =
      enqueue(s"${name}_enqueue_redirect", EXEC_FAMILY_WB_MISC, EXEC_OP_REDIRECT, data = nextPc)
    override def redirectRelative(delta: SInt): HwInline[Unit] =
      enqueue(s"${name}_enqueue_redirect_relative", EXEC_FAMILY_WB_MISC, EXEC_OP_REDIRECT_REL, imm = delta.asUInt)
    override def eq(lhs: UInt, rhs: UInt, pc: UInt, offset: UInt): HwInline[Unit] =
      enqueue(s"${name}_enqueue_branch_eq", EXEC_FAMILY_BRANCH, EXEC_OP_BRANCH_EQ, src1 = lhs, src2 = rhs, pc = pc, imm = offset)
    override def ne(lhs: UInt, rhs: UInt, pc: UInt, offset: UInt): HwInline[Unit] =
      enqueue(s"${name}_enqueue_branch_ne", EXEC_FAMILY_BRANCH, EXEC_OP_BRANCH_NE, src1 = lhs, src2 = rhs, pc = pc, imm = offset)
    override def lt(lhs: UInt, rhs: UInt, pc: UInt, offset: UInt): HwInline[Unit] =
      enqueue(s"${name}_enqueue_branch_lt", EXEC_FAMILY_BRANCH, EXEC_OP_BRANCH_LT, src1 = lhs, src2 = rhs, pc = pc, imm = offset)
    override def ltu(lhs: UInt, rhs: UInt, pc: UInt, offset: UInt): HwInline[Unit] =
      enqueue(s"${name}_enqueue_branch_ltu", EXEC_FAMILY_BRANCH, EXEC_OP_BRANCH_LTU, src1 = lhs, src2 = rhs, pc = pc, imm = offset)
    override def ge(lhs: UInt, rhs: UInt, pc: UInt, offset: UInt): HwInline[Unit] =
      enqueue(s"${name}_enqueue_branch_ge", EXEC_FAMILY_BRANCH, EXEC_OP_BRANCH_GE, src1 = lhs, src2 = rhs, pc = pc, imm = offset)
    override def geu(lhs: UInt, rhs: UInt, pc: UInt, offset: UInt): HwInline[Unit] =
      enqueue(s"${name}_enqueue_branch_geu", EXEC_FAMILY_BRANCH, EXEC_OP_BRANCH_GEU, src1 = lhs, src2 = rhs, pc = pc, imm = offset)
    override def loadWord(rd: UInt, wbToken: UInt, base: UInt, offset: UInt): HwInline[Unit] =
      enqueue(s"${name}_enqueue_load_word", EXEC_FAMILY_MEM, EXEC_OP_MEM_LOAD, EXEC_SUBOP_WORD, rd, wbToken, src1 = base, src2 = offset)
    override def storeWord(base: UInt, offset: UInt, data: UInt): HwInline[Unit] =
      enqueue(s"${name}_enqueue_store_word", EXEC_FAMILY_MEM, EXEC_OP_MEM_STORE, EXEC_SUBOP_WORD, src1 = base, src2 = offset, data = data)
    override def loadByte(rd: UInt, wbToken: UInt, base: UInt, offset: UInt, unsigned: Bool): HwInline[Unit] =
      enqueue(s"${name}_enqueue_load_byte", EXEC_FAMILY_MEM, EXEC_OP_MEM_LOAD, EXEC_SUBOP_BYTE, rd, wbToken, src1 = base, src2 = offset, unsigned = unsigned)
    override def loadHalf(rd: UInt, wbToken: UInt, base: UInt, offset: UInt, unsigned: Bool): HwInline[Unit] =
      enqueue(s"${name}_enqueue_load_half", EXEC_FAMILY_MEM, EXEC_OP_MEM_LOAD, EXEC_SUBOP_HALF, rd, wbToken, src1 = base, src2 = offset, unsigned = unsigned)
    override def storeByte(base: UInt, offset: UInt, data: UInt): HwInline[Unit] =
      enqueue(s"${name}_enqueue_store_byte", EXEC_FAMILY_MEM, EXEC_OP_MEM_STORE, EXEC_SUBOP_BYTE, src1 = base, src2 = offset, data = data)
    override def storeHalf(base: UInt, offset: UInt, data: UInt): HwInline[Unit] =
      enqueue(s"${name}_enqueue_store_half", EXEC_FAMILY_MEM, EXEC_OP_MEM_STORE, EXEC_SUBOP_HALF, src1 = base, src2 = offset, data = data)
    override def mem(isLoad: Bool, rd: UInt, wbToken: UInt, base: UInt, offset: UInt, data: UInt, kind: UInt, unsigned: Bool): HwInline[Unit] =
      if (isLoad.litToBooleanOption.contains(true)) load(rd, wbToken, base, offset, kind, unsigned)
      else store(base, offset, data, kind)

    override def load(rd: UInt, wbToken: UInt, base: UInt, offset: UInt, kind: UInt, unsigned: Bool): HwInline[Unit] = HwInline.atomic(
      s"${name}_enqueue_load",
    ) { _ =>
      when(kind === 0.U) {
        SysCall.Inline(loadWord(rd, wbToken, base, offset))
      }.elsewhen(kind === 1.U) {
        SysCall.Inline(loadByte(rd, wbToken, base, offset, unsigned))
      }.otherwise {
        SysCall.Inline(loadHalf(rd, wbToken, base, offset, unsigned))
      }
    }
    override def store(base: UInt, offset: UInt, data: UInt, kind: UInt): HwInline[Unit] = HwInline.atomic(
      s"${name}_enqueue_store",
    ) { _ =>
      when(kind === 0.U) {
        SysCall.Inline(storeWord(base, offset, data))
      }.elsewhen(kind === 1.U) {
        SysCall.Inline(storeByte(base, offset, data))
      }.otherwise {
        SysCall.Inline(storeHalf(base, offset, data))
      }
    }
    override def auipc(rd: UInt, wbToken: UInt, pc: UInt, imm: UInt): HwInline[Unit] =
      enqueue(s"${name}_enqueue_auipc", EXEC_FAMILY_UPPER, EXEC_OP_UPPER_AUIPC, rd = rd, wbToken = wbToken, pc = pc, imm = imm)
    override def jal(rd: UInt, wbToken: UInt, pc: UInt, offset: SInt): HwInline[Unit] =
      enqueue(s"${name}_enqueue_jal", EXEC_FAMILY_JUMP, EXEC_OP_JAL, rd = rd, wbToken = wbToken, pc = pc, imm = offset.asUInt)
    override def jalr(rd: UInt, wbToken: UInt, pc: UInt, base: UInt, offset: UInt): HwInline[Unit] =
      enqueue(s"${name}_enqueue_jalr", EXEC_FAMILY_JUMP, EXEC_OP_JALR, rd = rd, wbToken = wbToken, pc = pc, src1 = base, src2 = offset)
    override def csrRw(rd: UInt, wbToken: UInt, addr: UInt, src: UInt): HwInline[Unit] =
      enqueue(s"${name}_enqueue_csr_rw", EXEC_FAMILY_CSR, EXEC_OP_CSR_RW, rd = rd, wbToken = wbToken, data = src, csrAddr = addr)
    override def csrRs(rd: UInt, wbToken: UInt, addr: UInt, src: UInt): HwInline[Unit] =
      enqueue(s"${name}_enqueue_csr_rs", EXEC_FAMILY_CSR, EXEC_OP_CSR_RS, rd = rd, wbToken = wbToken, data = src, csrAddr = addr)
    override def csrRc(rd: UInt, wbToken: UInt, addr: UInt, src: UInt): HwInline[Unit] =
      enqueue(s"${name}_enqueue_csr_rc", EXEC_FAMILY_CSR, EXEC_OP_CSR_RC, rd = rd, wbToken = wbToken, data = src, csrAddr = addr)
  }

  def RequestExecuteApi(): HwInline[ExecuteApiDecl] = HwInline.bindings(s"${name}_execute_api") { _ =>
    api
  }

  val csrProbeApi: CsrProbeApiDecl = csr.probeApi

  private def writeBoundLsuApi(): HwInline[LsuApiDecl] = HwInline.bindings(s"${name}_lsu_api") { _ =>
    lsuRef.get
  }

  private def writeBoundWritebackApi(): HwInline[WritebackApiDecl] = HwInline.bindings(s"${name}_writeback_api") { _ =>
    writebackRef.get
  }

  def clearExecuteReqBuffer(): HwInline[Unit] = executeReqBuffer.clear()

  private def writeBoundHazardApi(): HwInline[ControlHazardApiDecl] = HwInline.bindings(s"${name}_hazard_api") { _ =>
    hazardRef.get
  }
}
