package mycpu.pipeline

import HwOS.kernel._
import chisel3._
import chisel3.util._
import mycpu.common._

final class ExecuteProcess(
    lsuRef: ApiRef[LsuApiDecl],
    writebackRef: ApiRef[WritebackApiDecl],
    localName: String = "Execute",
)(implicit kernel: Kernel)
    extends HwProcess(localName) {

  final class ExecuteReq extends Bundle {
    val kind = UInt(6.W)
    val rd = UInt(5.W)
    val wbToken = UInt(4.W)
    val lhs = UInt(XLEN.W)
    val rhs = UInt(XLEN.W)
    val pc = UInt(XLEN.W)
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

  private val EXEC_ADD = 0.U(6.W)
  private val EXEC_SUB = 1.U(6.W)
  private val EXEC_AND = 2.U(6.W)
  private val EXEC_OR = 3.U(6.W)
  private val EXEC_XOR = 4.U(6.W)
  private val EXEC_SLL = 5.U(6.W)
  private val EXEC_SRL = 6.U(6.W)
  private val EXEC_SRA = 7.U(6.W)
  private val EXEC_SLT = 8.U(6.W)
  private val EXEC_SLTU = 9.U(6.W)
  private val EXEC_WRITE_REG = 10.U(6.W)
  private val EXEC_REDIRECT = 11.U(6.W)
  private val EXEC_REDIRECT_REL = 12.U(6.W)
  private val EXEC_BRANCH_EQ = 13.U(6.W)
  private val EXEC_BRANCH_NE = 14.U(6.W)
  private val EXEC_BRANCH_LT = 15.U(6.W)
  private val EXEC_BRANCH_LTU = 16.U(6.W)
  private val EXEC_BRANCH_GE = 17.U(6.W)
  private val EXEC_BRANCH_GEU = 18.U(6.W)
  private val EXEC_MEM_LOAD_WORD = 19.U(6.W)
  private val EXEC_MEM_STORE_WORD = 20.U(6.W)
  private val EXEC_MEM_LOAD_BYTE = 21.U(6.W)
  private val EXEC_MEM_LOAD_HALF = 22.U(6.W)
  private val EXEC_MEM_STORE_BYTE = 23.U(6.W)
  private val EXEC_MEM_STORE_HALF = 24.U(6.W)
  private val EXEC_AUIPC = 25.U(6.W)
  private val EXEC_JAL = 26.U(6.W)
  private val EXEC_JALR = 27.U(6.W)
  private val EXEC_CSR_RW = 28.U(6.W)
  private val EXEC_CSR_RS = 29.U(6.W)
  private val EXEC_CSR_RC = 30.U(6.W)

  private val execAcquireRefName = s"${name}_Exec_Acquire"

  override def entry(): Unit = {
    executeWorker.entry {
      val aluApi = alu.api
      val csrApi = csr.api
      val lsuApi = SysCall.Inline(writeBoundLsuApi())
      val wbApi = SysCall.Inline(writeBoundWritebackApi())

      executeWorker.Step("WaitReq") {
        executeWorker.waitCondition(executeReqBuffer.valid)
      }

      executeWorker.Step("TakeReq") {
        execReqReg := SysCall.Inline(executeReqBuffer.pop())
      }

      executeWorker.Step("Dispatch") {
        switch(execReqReg.kind) {
          is(EXEC_ADD) { executeWorker.jump(executeWorker.stepRef("ExecAdd")) }
          is(EXEC_SUB) { executeWorker.jump(executeWorker.stepRef("ExecSub")) }
          is(EXEC_AND) { executeWorker.jump(executeWorker.stepRef("ExecAnd")) }
          is(EXEC_OR) { executeWorker.jump(executeWorker.stepRef("ExecOr")) }
          is(EXEC_XOR) { executeWorker.jump(executeWorker.stepRef("ExecXor")) }
          is(EXEC_SLL) { executeWorker.jump(executeWorker.stepRef("ExecSll")) }
          is(EXEC_SRL) { executeWorker.jump(executeWorker.stepRef("ExecSrl")) }
          is(EXEC_SRA) { executeWorker.jump(executeWorker.stepRef("ExecSra")) }
          is(EXEC_SLT) { executeWorker.jump(executeWorker.stepRef("ExecSlt")) }
          is(EXEC_SLTU) { executeWorker.jump(executeWorker.stepRef("ExecSltu")) }
          is(EXEC_WRITE_REG) { executeWorker.jump(executeWorker.stepRef("ExecWriteReg")) }
          is(EXEC_REDIRECT) { executeWorker.jump(executeWorker.stepRef("ExecRedirect")) }
          is(EXEC_REDIRECT_REL) { executeWorker.jump(executeWorker.stepRef("ExecRedirectRelative")) }
          is(EXEC_BRANCH_EQ) { executeWorker.jump(executeWorker.stepRef("ExecBranchEq")) }
          is(EXEC_BRANCH_NE) { executeWorker.jump(executeWorker.stepRef("ExecBranchNe")) }
          is(EXEC_BRANCH_LT) { executeWorker.jump(executeWorker.stepRef("ExecBranchLt")) }
          is(EXEC_BRANCH_LTU) { executeWorker.jump(executeWorker.stepRef("ExecBranchLtu")) }
          is(EXEC_BRANCH_GE) { executeWorker.jump(executeWorker.stepRef("ExecBranchGe")) }
          is(EXEC_BRANCH_GEU) { executeWorker.jump(executeWorker.stepRef("ExecBranchGeu")) }
          is(EXEC_MEM_LOAD_WORD) { executeWorker.jump(executeWorker.stepRef("ExecLoadWord")) }
          is(EXEC_MEM_STORE_WORD) { executeWorker.jump(executeWorker.stepRef("ExecStoreWord")) }
          is(EXEC_MEM_LOAD_BYTE) { executeWorker.jump(executeWorker.stepRef("ExecLoadByte")) }
          is(EXEC_MEM_LOAD_HALF) { executeWorker.jump(executeWorker.stepRef("ExecLoadHalf")) }
          is(EXEC_MEM_STORE_BYTE) { executeWorker.jump(executeWorker.stepRef("ExecStoreByte")) }
          is(EXEC_MEM_STORE_HALF) { executeWorker.jump(executeWorker.stepRef("ExecStoreHalf")) }
          is(EXEC_AUIPC) { executeWorker.jump(executeWorker.stepRef("ExecAuipc")) }
          is(EXEC_JAL) { executeWorker.jump(executeWorker.stepRef("ExecJal")) }
          is(EXEC_JALR) { executeWorker.jump(executeWorker.stepRef("ExecJalr")) }
          is(EXEC_CSR_RW) { executeWorker.jump(executeWorker.stepRef("ExecCsrRw")) }
          is(EXEC_CSR_RS) { executeWorker.jump(executeWorker.stepRef("ExecCsrRs")) }
          is(EXEC_CSR_RC) { executeWorker.jump(executeWorker.stepRef("ExecCsrRc")) }
        }
      }

      def issueWriteReg(result: UInt): Unit = {
        SysCall.Inline(wbApi.writeReg(execReqReg.wbToken, result))
      }

      def issueRedirect(nextPc: UInt): Unit = {
        SysCall.Inline(wbApi.redirect(nextPc))
      }

      def issueWriteRegAndRedirect(result: UInt, nextPc: UInt): Unit = {
        SysCall.Inline(wbApi.writeRegAndRedirect(execReqReg.wbToken, result, nextPc))
      }

      def branchTarget: UInt = SysCall.Inline(aluApi.add(execReqReg.pc, execReqReg.data))

      executeWorker.Step("ExecAdd") {
        val result = SysCall.Inline(aluApi.add(execReqReg.lhs, execReqReg.rhs))
        printf(p"[EXEC] add lhs=${Hexadecimal(execReqReg.lhs)} rhs=${Hexadecimal(execReqReg.rhs)} result=${Hexadecimal(result)}\n")
        issueWriteReg(result)
      }
      SysCall.Inline(wbApi.wbPath())
      executeWorker.Step("AfterExecAdd") { executeWorker.jump(executeWorker.stepRef("WaitReq")) }

      executeWorker.Step("ExecSub") {
        val result = SysCall.Inline(aluApi.sub(execReqReg.lhs, execReqReg.rhs))
        issueWriteReg(result)
      }
      SysCall.Inline(wbApi.wbPath())
      executeWorker.Step("AfterExecSub") { executeWorker.jump(executeWorker.stepRef("WaitReq")) }

      executeWorker.Step("ExecAnd") {
        issueWriteReg(SysCall.Inline(aluApi.and(execReqReg.lhs, execReqReg.rhs)))
      }
      SysCall.Inline(wbApi.wbPath())
      executeWorker.Step("AfterExecAnd") { executeWorker.jump(executeWorker.stepRef("WaitReq")) }

      executeWorker.Step("ExecOr") {
        issueWriteReg(SysCall.Inline(aluApi.or(execReqReg.lhs, execReqReg.rhs)))
      }
      SysCall.Inline(wbApi.wbPath())
      executeWorker.Step("AfterExecOr") { executeWorker.jump(executeWorker.stepRef("WaitReq")) }

      executeWorker.Step("ExecXor") {
        issueWriteReg(SysCall.Inline(aluApi.xor(execReqReg.lhs, execReqReg.rhs)))
      }
      SysCall.Inline(wbApi.wbPath())
      executeWorker.Step("AfterExecXor") { executeWorker.jump(executeWorker.stepRef("WaitReq")) }

      executeWorker.Step("ExecSll") {
        issueWriteReg(SysCall.Inline(aluApi.sll(execReqReg.lhs, execReqReg.rhs)))
      }
      SysCall.Inline(wbApi.wbPath())
      executeWorker.Step("AfterExecSll") { executeWorker.jump(executeWorker.stepRef("WaitReq")) }

      executeWorker.Step("ExecSrl") {
        issueWriteReg(SysCall.Inline(aluApi.srl(execReqReg.lhs, execReqReg.rhs)))
      }
      SysCall.Inline(wbApi.wbPath())
      executeWorker.Step("AfterExecSrl") { executeWorker.jump(executeWorker.stepRef("WaitReq")) }

      executeWorker.Step("ExecSra") {
        issueWriteReg(SysCall.Inline(aluApi.sra(execReqReg.lhs, execReqReg.rhs)))
      }
      SysCall.Inline(wbApi.wbPath())
      executeWorker.Step("AfterExecSra") { executeWorker.jump(executeWorker.stepRef("WaitReq")) }

      executeWorker.Step("ExecSlt") {
        issueWriteReg(SysCall.Inline(aluApi.slt(execReqReg.lhs, execReqReg.rhs)))
      }
      SysCall.Inline(wbApi.wbPath())
      executeWorker.Step("AfterExecSlt") { executeWorker.jump(executeWorker.stepRef("WaitReq")) }

      executeWorker.Step("ExecSltu") {
        issueWriteReg(SysCall.Inline(aluApi.sltu(execReqReg.lhs, execReqReg.rhs)))
      }
      SysCall.Inline(wbApi.wbPath())
      executeWorker.Step("AfterExecSltu") { executeWorker.jump(executeWorker.stepRef("WaitReq")) }

      executeWorker.Step("ExecWriteReg") {
        issueWriteReg(execReqReg.data)
      }
      SysCall.Inline(wbApi.wbPath())
      executeWorker.Step("AfterExecWriteReg") { executeWorker.jump(executeWorker.stepRef("WaitReq")) }

      executeWorker.Step("ExecRedirect") {
        issueRedirect(execReqReg.data)
      }
      SysCall.Inline(wbApi.wbPath())
      executeWorker.Step("AfterExecRedirect") { executeWorker.jump(executeWorker.stepRef("WaitReq")) }

      executeWorker.Step("ExecRedirectRelative") {
        SysCall.Inline(wbApi.redirectRelative(execReqReg.data.asSInt))
      }
      SysCall.Inline(wbApi.wbPath())
      executeWorker.Step("AfterExecRedirectRelative") { executeWorker.jump(executeWorker.stepRef("WaitReq")) }

      executeWorker.Step("ExecBranchEq") {
        when(execReqReg.lhs === execReqReg.rhs) {
          issueRedirect(branchTarget)
        }.otherwise {
          SysCall.Inline(wbApi.commit())
        }
      }
      SysCall.Inline(wbApi.wbPath())
      executeWorker.Step("AfterExecBranchEq") { executeWorker.jump(executeWorker.stepRef("WaitReq")) }

      executeWorker.Step("ExecBranchNe") {
        when(execReqReg.lhs =/= execReqReg.rhs) {
          issueRedirect(branchTarget)
        }.otherwise {
          SysCall.Inline(wbApi.commit())
        }
      }
      SysCall.Inline(wbApi.wbPath())
      executeWorker.Step("AfterExecBranchNe") { executeWorker.jump(executeWorker.stepRef("WaitReq")) }

      executeWorker.Step("ExecBranchLt") {
        when(execReqReg.lhs.asSInt < execReqReg.rhs.asSInt) {
          issueRedirect(branchTarget)
        }.otherwise {
          SysCall.Inline(wbApi.commit())
        }
      }
      SysCall.Inline(wbApi.wbPath())
      executeWorker.Step("AfterExecBranchLt") { executeWorker.jump(executeWorker.stepRef("WaitReq")) }

      executeWorker.Step("ExecBranchLtu") {
        when(execReqReg.lhs < execReqReg.rhs) {
          issueRedirect(branchTarget)
        }.otherwise {
          SysCall.Inline(wbApi.commit())
        }
      }
      SysCall.Inline(wbApi.wbPath())
      executeWorker.Step("AfterExecBranchLtu") { executeWorker.jump(executeWorker.stepRef("WaitReq")) }

      executeWorker.Step("ExecBranchGe") {
        when(execReqReg.lhs.asSInt >= execReqReg.rhs.asSInt) {
          issueRedirect(branchTarget)
        }.otherwise {
          SysCall.Inline(wbApi.commit())
        }
      }
      SysCall.Inline(wbApi.wbPath())
      executeWorker.Step("AfterExecBranchGe") { executeWorker.jump(executeWorker.stepRef("WaitReq")) }

      executeWorker.Step("ExecBranchGeu") {
        when(execReqReg.lhs >= execReqReg.rhs) {
          issueRedirect(branchTarget)
        }.otherwise {
          SysCall.Inline(wbApi.commit())
        }
      }
      SysCall.Inline(wbApi.wbPath())
      executeWorker.Step("AfterExecBranchGeu") { executeWorker.jump(executeWorker.stepRef("WaitReq")) }

      executeWorker.Step("ExecLoadWord") {
        SysCall.Inline(lsuApi.loadWord(execReqReg.wbToken, SysCall.Inline(aluApi.add(execReqReg.lhs, execReqReg.rhs))))
      }
      SysCall.Inline(lsuApi.loadPath())
      executeWorker.Step("AfterExecLoadWord") { executeWorker.jump(executeWorker.stepRef("WaitReq")) }

      executeWorker.Step("ExecStoreWord") {
        SysCall.Inline(lsuApi.storeWord(SysCall.Inline(aluApi.add(execReqReg.lhs, execReqReg.rhs)), execReqReg.data))
      }
      SysCall.Inline(lsuApi.storePath())
      executeWorker.Step("AfterExecStoreWord") { executeWorker.jump(executeWorker.stepRef("WaitReq")) }

      executeWorker.Step("ExecLoadByte") {
        SysCall.Inline(
          lsuApi.loadByte(execReqReg.wbToken, SysCall.Inline(aluApi.add(execReqReg.lhs, execReqReg.rhs)), execReqReg.unsigned),
        )
      }
      SysCall.Inline(lsuApi.loadPath())
      executeWorker.Step("AfterExecLoadByte") { executeWorker.jump(executeWorker.stepRef("WaitReq")) }

      executeWorker.Step("ExecLoadHalf") {
        SysCall.Inline(
          lsuApi.loadHalf(execReqReg.wbToken, SysCall.Inline(aluApi.add(execReqReg.lhs, execReqReg.rhs)), execReqReg.unsigned),
        )
      }
      SysCall.Inline(lsuApi.loadPath())
      executeWorker.Step("AfterExecLoadHalf") { executeWorker.jump(executeWorker.stepRef("WaitReq")) }

      executeWorker.Step("ExecStoreByte") {
        SysCall.Inline(lsuApi.storeByte(SysCall.Inline(aluApi.add(execReqReg.lhs, execReqReg.rhs)), execReqReg.data))
      }
      SysCall.Inline(lsuApi.storePath())
      executeWorker.Step("AfterExecStoreByte") { executeWorker.jump(executeWorker.stepRef("WaitReq")) }

      executeWorker.Step("ExecStoreHalf") {
        SysCall.Inline(lsuApi.storeHalf(SysCall.Inline(aluApi.add(execReqReg.lhs, execReqReg.rhs)), execReqReg.data))
      }
      SysCall.Inline(lsuApi.storePath())
      executeWorker.Step("AfterExecStoreHalf") { executeWorker.jump(executeWorker.stepRef("WaitReq")) }

      executeWorker.Step("ExecAuipc") {
        issueWriteReg(SysCall.Inline(aluApi.add(execReqReg.pc, execReqReg.data)))
      }
      SysCall.Inline(wbApi.wbPath())
      executeWorker.Step("AfterExecAuipc") { executeWorker.jump(executeWorker.stepRef("WaitReq")) }

      executeWorker.Step("ExecJal") {
        val ret = execReqReg.pc + 4.U(XLEN.W)
        val target = (execReqReg.pc.asSInt + execReqReg.data.asSInt).asUInt
        issueWriteRegAndRedirect(ret, target)
      }
      SysCall.Inline(wbApi.wbPath())
      executeWorker.Step("AfterExecJal") { executeWorker.jump(executeWorker.stepRef("WaitReq")) }

      executeWorker.Step("ExecJalr") {
        val ret = execReqReg.pc + 4.U(XLEN.W)
        val rawTarget = SysCall.Inline(aluApi.add(execReqReg.lhs, execReqReg.rhs))
        issueWriteRegAndRedirect(ret, rawTarget & (~1.U(XLEN.W)))
      }
      SysCall.Inline(wbApi.wbPath())
      executeWorker.Step("AfterExecJalr") { executeWorker.jump(executeWorker.stepRef("WaitReq")) }

      executeWorker.Step("ExecCsrRw") {
        issueWriteReg(SysCall.Inline(csrApi.rw(execReqReg.csrAddr, execReqReg.data)))
      }
      SysCall.Inline(wbApi.wbPath())
      executeWorker.Step("AfterExecCsrRw") { executeWorker.jump(executeWorker.stepRef("WaitReq")) }

      executeWorker.Step("ExecCsrRs") {
        issueWriteReg(SysCall.Inline(csrApi.rs(execReqReg.csrAddr, execReqReg.data)))
      }
      SysCall.Inline(wbApi.wbPath())
      executeWorker.Step("AfterExecCsrRs") { executeWorker.jump(executeWorker.stepRef("WaitReq")) }

      executeWorker.Step("ExecCsrRc") {
        issueWriteReg(SysCall.Inline(csrApi.rc(execReqReg.csrAddr, execReqReg.data)))
      }
      SysCall.Inline(wbApi.wbPath())
      executeWorker.Step("AfterExecCsrRc") { executeWorker.jump(executeWorker.stepRef("WaitReq")) }
    }

    val daemon = createLogic("Daemon")
    daemon.run {
      when(!executeWorker.active) {
        SysCall.Inline(SysCall.start(executeWorker))
      }
    }
  }

  val api: ExecuteApiDecl = new ExecuteApiDecl {
    private val addPacket = WireDefault(0.U.asTypeOf(new ExecuteReq))
    private val subPacket = WireDefault(0.U.asTypeOf(new ExecuteReq))
    private val andPacket = WireDefault(0.U.asTypeOf(new ExecuteReq))
    private val orPacket = WireDefault(0.U.asTypeOf(new ExecuteReq))
    private val xorPacket = WireDefault(0.U.asTypeOf(new ExecuteReq))
    private val sllPacket = WireDefault(0.U.asTypeOf(new ExecuteReq))
    private val srlPacket = WireDefault(0.U.asTypeOf(new ExecuteReq))
    private val sraPacket = WireDefault(0.U.asTypeOf(new ExecuteReq))
    private val sltPacket = WireDefault(0.U.asTypeOf(new ExecuteReq))
    private val sltuPacket = WireDefault(0.U.asTypeOf(new ExecuteReq))
    private val writeRegPacket = WireDefault(0.U.asTypeOf(new ExecuteReq))
    private val redirectPacket = WireDefault(0.U.asTypeOf(new ExecuteReq))
    private val redirectRelativePacket = WireDefault(0.U.asTypeOf(new ExecuteReq))
    private val branchEqPacket = WireDefault(0.U.asTypeOf(new ExecuteReq))
    private val branchNePacket = WireDefault(0.U.asTypeOf(new ExecuteReq))
    private val branchLtPacket = WireDefault(0.U.asTypeOf(new ExecuteReq))
    private val branchLtuPacket = WireDefault(0.U.asTypeOf(new ExecuteReq))
    private val branchGePacket = WireDefault(0.U.asTypeOf(new ExecuteReq))
    private val branchGeuPacket = WireDefault(0.U.asTypeOf(new ExecuteReq))
    private val loadWordPacket = WireDefault(0.U.asTypeOf(new ExecuteReq))
    private val storeWordPacket = WireDefault(0.U.asTypeOf(new ExecuteReq))
    private val loadBytePacket = WireDefault(0.U.asTypeOf(new ExecuteReq))
    private val loadHalfPacket = WireDefault(0.U.asTypeOf(new ExecuteReq))
    private val storeBytePacket = WireDefault(0.U.asTypeOf(new ExecuteReq))
    private val storeHalfPacket = WireDefault(0.U.asTypeOf(new ExecuteReq))
    private val auipcPacket = WireDefault(0.U.asTypeOf(new ExecuteReq))
    private val jalPacket = WireDefault(0.U.asTypeOf(new ExecuteReq))
    private val jalrPacket = WireDefault(0.U.asTypeOf(new ExecuteReq))
    private val csrRwPacket = WireDefault(0.U.asTypeOf(new ExecuteReq))
    private val csrRsPacket = WireDefault(0.U.asTypeOf(new ExecuteReq))
    private val csrRcPacket = WireDefault(0.U.asTypeOf(new ExecuteReq))

    override def execPath(): HwInline[Unit] = HwInline.thread(s"${name}_exec_path") { t =>
      t.Step(execAcquireRefName) {}
      t.Step(s"${name}_Exec_PushReq") {
        SysCall.Inline(executeReqBuffer.push(launchExecReqReg))
      }
      t.Step(s"${name}_Exec_Release") {}
    }

    override def memPath(): HwInline[Unit] = execPath()

    private def enqueue(tag: String, packet: ExecuteReq): HwInline[Unit] = HwInline.atomic(tag) { t =>
      launchExecReqReg := packet
      t.jump(t.stepRef(execAcquireRefName))
    }

    override def add(rd: UInt, wbToken: UInt, lhs: UInt, rhs: UInt): HwInline[Unit] =
      { addPacket.kind := EXEC_ADD; addPacket.rd := rd; addPacket.wbToken := wbToken; addPacket.lhs := lhs; addPacket.rhs := rhs; enqueue(s"${name}_enqueue_add", addPacket) }
    override def sub(rd: UInt, wbToken: UInt, lhs: UInt, rhs: UInt): HwInline[Unit] =
      { subPacket.kind := EXEC_SUB; subPacket.rd := rd; subPacket.wbToken := wbToken; subPacket.lhs := lhs; subPacket.rhs := rhs; enqueue(s"${name}_enqueue_sub", subPacket) }
    override def and(rd: UInt, wbToken: UInt, lhs: UInt, rhs: UInt): HwInline[Unit] =
      { andPacket.kind := EXEC_AND; andPacket.rd := rd; andPacket.wbToken := wbToken; andPacket.lhs := lhs; andPacket.rhs := rhs; enqueue(s"${name}_enqueue_and", andPacket) }
    override def or(rd: UInt, wbToken: UInt, lhs: UInt, rhs: UInt): HwInline[Unit] =
      { orPacket.kind := EXEC_OR; orPacket.rd := rd; orPacket.wbToken := wbToken; orPacket.lhs := lhs; orPacket.rhs := rhs; enqueue(s"${name}_enqueue_or", orPacket) }
    override def xor(rd: UInt, wbToken: UInt, lhs: UInt, rhs: UInt): HwInline[Unit] =
      { xorPacket.kind := EXEC_XOR; xorPacket.rd := rd; xorPacket.wbToken := wbToken; xorPacket.lhs := lhs; xorPacket.rhs := rhs; enqueue(s"${name}_enqueue_xor", xorPacket) }
    override def sll(rd: UInt, wbToken: UInt, lhs: UInt, rhs: UInt): HwInline[Unit] =
      { sllPacket.kind := EXEC_SLL; sllPacket.rd := rd; sllPacket.wbToken := wbToken; sllPacket.lhs := lhs; sllPacket.rhs := rhs; enqueue(s"${name}_enqueue_sll", sllPacket) }
    override def srl(rd: UInt, wbToken: UInt, lhs: UInt, rhs: UInt): HwInline[Unit] =
      { srlPacket.kind := EXEC_SRL; srlPacket.rd := rd; srlPacket.wbToken := wbToken; srlPacket.lhs := lhs; srlPacket.rhs := rhs; enqueue(s"${name}_enqueue_srl", srlPacket) }
    override def sra(rd: UInt, wbToken: UInt, lhs: UInt, rhs: UInt): HwInline[Unit] =
      { sraPacket.kind := EXEC_SRA; sraPacket.rd := rd; sraPacket.wbToken := wbToken; sraPacket.lhs := lhs; sraPacket.rhs := rhs; enqueue(s"${name}_enqueue_sra", sraPacket) }
    override def slt(rd: UInt, wbToken: UInt, lhs: UInt, rhs: UInt): HwInline[Unit] =
      { sltPacket.kind := EXEC_SLT; sltPacket.rd := rd; sltPacket.wbToken := wbToken; sltPacket.lhs := lhs; sltPacket.rhs := rhs; enqueue(s"${name}_enqueue_slt", sltPacket) }
    override def sltu(rd: UInt, wbToken: UInt, lhs: UInt, rhs: UInt): HwInline[Unit] =
      { sltuPacket.kind := EXEC_SLTU; sltuPacket.rd := rd; sltuPacket.wbToken := wbToken; sltuPacket.lhs := lhs; sltuPacket.rhs := rhs; enqueue(s"${name}_enqueue_sltu", sltuPacket) }
    override def writeReg(rd: UInt, wbToken: UInt, data: UInt): HwInline[Unit] =
      { writeRegPacket.kind := EXEC_WRITE_REG; writeRegPacket.rd := rd; writeRegPacket.wbToken := wbToken; writeRegPacket.data := data; enqueue(s"${name}_enqueue_write_reg", writeRegPacket) }
    override def redirect(nextPc: UInt): HwInline[Unit] =
      { redirectPacket.kind := EXEC_REDIRECT; redirectPacket.data := nextPc; enqueue(s"${name}_enqueue_redirect", redirectPacket) }
    override def redirectRelative(delta: SInt): HwInline[Unit] =
      { redirectRelativePacket.kind := EXEC_REDIRECT_REL; redirectRelativePacket.data := delta.asUInt; enqueue(s"${name}_enqueue_redirect_relative", redirectRelativePacket) }
    override def eq(lhs: UInt, rhs: UInt, pc: UInt, offset: UInt): HwInline[Unit] =
      { branchEqPacket.kind := EXEC_BRANCH_EQ; branchEqPacket.lhs := lhs; branchEqPacket.rhs := rhs; branchEqPacket.pc := pc; branchEqPacket.data := offset; enqueue(s"${name}_enqueue_branch_eq", branchEqPacket) }
    override def ne(lhs: UInt, rhs: UInt, pc: UInt, offset: UInt): HwInline[Unit] =
      { branchNePacket.kind := EXEC_BRANCH_NE; branchNePacket.lhs := lhs; branchNePacket.rhs := rhs; branchNePacket.pc := pc; branchNePacket.data := offset; enqueue(s"${name}_enqueue_branch_ne", branchNePacket) }
    override def lt(lhs: UInt, rhs: UInt, pc: UInt, offset: UInt): HwInline[Unit] =
      { branchLtPacket.kind := EXEC_BRANCH_LT; branchLtPacket.lhs := lhs; branchLtPacket.rhs := rhs; branchLtPacket.pc := pc; branchLtPacket.data := offset; enqueue(s"${name}_enqueue_branch_lt", branchLtPacket) }
    override def ltu(lhs: UInt, rhs: UInt, pc: UInt, offset: UInt): HwInline[Unit] =
      { branchLtuPacket.kind := EXEC_BRANCH_LTU; branchLtuPacket.lhs := lhs; branchLtuPacket.rhs := rhs; branchLtuPacket.pc := pc; branchLtuPacket.data := offset; enqueue(s"${name}_enqueue_branch_ltu", branchLtuPacket) }
    override def ge(lhs: UInt, rhs: UInt, pc: UInt, offset: UInt): HwInline[Unit] =
      { branchGePacket.kind := EXEC_BRANCH_GE; branchGePacket.lhs := lhs; branchGePacket.rhs := rhs; branchGePacket.pc := pc; branchGePacket.data := offset; enqueue(s"${name}_enqueue_branch_ge", branchGePacket) }
    override def geu(lhs: UInt, rhs: UInt, pc: UInt, offset: UInt): HwInline[Unit] =
      { branchGeuPacket.kind := EXEC_BRANCH_GEU; branchGeuPacket.lhs := lhs; branchGeuPacket.rhs := rhs; branchGeuPacket.pc := pc; branchGeuPacket.data := offset; enqueue(s"${name}_enqueue_branch_geu", branchGeuPacket) }
    override def loadWord(rd: UInt, wbToken: UInt, base: UInt, offset: UInt): HwInline[Unit] =
      { loadWordPacket.kind := EXEC_MEM_LOAD_WORD; loadWordPacket.rd := rd; loadWordPacket.wbToken := wbToken; loadWordPacket.lhs := base; loadWordPacket.rhs := offset; enqueue(s"${name}_enqueue_load_word", loadWordPacket) }
    override def storeWord(base: UInt, offset: UInt, data: UInt): HwInline[Unit] =
      { storeWordPacket.kind := EXEC_MEM_STORE_WORD; storeWordPacket.lhs := base; storeWordPacket.rhs := offset; storeWordPacket.data := data; enqueue(s"${name}_enqueue_store_word", storeWordPacket) }
    override def loadByte(rd: UInt, wbToken: UInt, base: UInt, offset: UInt, unsigned: Bool): HwInline[Unit] =
      { loadBytePacket.kind := EXEC_MEM_LOAD_BYTE; loadBytePacket.rd := rd; loadBytePacket.wbToken := wbToken; loadBytePacket.lhs := base; loadBytePacket.rhs := offset; loadBytePacket.unsigned := unsigned; enqueue(s"${name}_enqueue_load_byte", loadBytePacket) }
    override def loadHalf(rd: UInt, wbToken: UInt, base: UInt, offset: UInt, unsigned: Bool): HwInline[Unit] =
      { loadHalfPacket.kind := EXEC_MEM_LOAD_HALF; loadHalfPacket.rd := rd; loadHalfPacket.wbToken := wbToken; loadHalfPacket.lhs := base; loadHalfPacket.rhs := offset; loadHalfPacket.unsigned := unsigned; enqueue(s"${name}_enqueue_load_half", loadHalfPacket) }
    override def storeByte(base: UInt, offset: UInt, data: UInt): HwInline[Unit] =
      { storeBytePacket.kind := EXEC_MEM_STORE_BYTE; storeBytePacket.lhs := base; storeBytePacket.rhs := offset; storeBytePacket.data := data; enqueue(s"${name}_enqueue_store_byte", storeBytePacket) }
    override def storeHalf(base: UInt, offset: UInt, data: UInt): HwInline[Unit] =
      { storeHalfPacket.kind := EXEC_MEM_STORE_HALF; storeHalfPacket.lhs := base; storeHalfPacket.rhs := offset; storeHalfPacket.data := data; enqueue(s"${name}_enqueue_store_half", storeHalfPacket) }
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
      { auipcPacket.kind := EXEC_AUIPC; auipcPacket.rd := rd; auipcPacket.wbToken := wbToken; auipcPacket.pc := pc; auipcPacket.data := imm; enqueue(s"${name}_enqueue_auipc", auipcPacket) }
    override def jal(rd: UInt, wbToken: UInt, pc: UInt, offset: SInt): HwInline[Unit] =
      { jalPacket.kind := EXEC_JAL; jalPacket.rd := rd; jalPacket.wbToken := wbToken; jalPacket.pc := pc; jalPacket.data := offset.asUInt; enqueue(s"${name}_enqueue_jal", jalPacket) }
    override def jalr(rd: UInt, wbToken: UInt, pc: UInt, base: UInt, offset: UInt): HwInline[Unit] =
      { jalrPacket.kind := EXEC_JALR; jalrPacket.rd := rd; jalrPacket.wbToken := wbToken; jalrPacket.pc := pc; jalrPacket.lhs := base; jalrPacket.rhs := offset; enqueue(s"${name}_enqueue_jalr", jalrPacket) }
    override def csrRw(rd: UInt, wbToken: UInt, addr: UInt, src: UInt): HwInline[Unit] =
      { csrRwPacket.kind := EXEC_CSR_RW; csrRwPacket.rd := rd; csrRwPacket.wbToken := wbToken; csrRwPacket.csrAddr := addr; csrRwPacket.data := src; enqueue(s"${name}_enqueue_csr_rw", csrRwPacket) }
    override def csrRs(rd: UInt, wbToken: UInt, addr: UInt, src: UInt): HwInline[Unit] =
      { csrRsPacket.kind := EXEC_CSR_RS; csrRsPacket.rd := rd; csrRsPacket.wbToken := wbToken; csrRsPacket.csrAddr := addr; csrRsPacket.data := src; enqueue(s"${name}_enqueue_csr_rs", csrRsPacket) }
    override def csrRc(rd: UInt, wbToken: UInt, addr: UInt, src: UInt): HwInline[Unit] =
      { csrRcPacket.kind := EXEC_CSR_RC; csrRcPacket.rd := rd; csrRcPacket.wbToken := wbToken; csrRcPacket.csrAddr := addr; csrRcPacket.data := src; enqueue(s"${name}_enqueue_csr_rc", csrRcPacket) }
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
}
