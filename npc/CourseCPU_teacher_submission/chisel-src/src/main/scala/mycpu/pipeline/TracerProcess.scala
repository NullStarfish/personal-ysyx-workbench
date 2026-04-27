package mycpu.pipeline

import HwOS.kernel._
import chisel3._
import chisel3.util._
import mycpu.common._
import mycpu.dpi._

final class NoopTracerProcess(localName: String = "Tracer")(implicit kernel: Kernel) extends HwProcess(localName) {
  val api: TraceApiDecl = new TraceApiDecl {
    override def issue(pc: UInt, inst: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_issue") { _ => }
    override def commit(): HwInline[Unit] = HwInline.atomic(s"${name}_commit") { _ => }
  }

  override def entry(): Unit = {}
}

final class DummyTracerProcess(depth: Int = 16, localName: String = "Tracer")(implicit kernel: Kernel) extends HwProcess(localName) {
  private val issuePcQ = RegInit(VecInit(Seq.fill(depth)(0.U(32.W))))
  private val issueInstQ = RegInit(VecInit(Seq.fill(depth)(0.U(32.W))))
  private val issueHead = RegInit(0.U(log2Ceil(depth).W))
  private val issueTail = RegInit(0.U(log2Ceil(depth).W))
  private val issueCount = RegInit(0.U(log2Ceil(depth + 1).W))

  private val lastCommittedPcReg = RegInit(0.U(32.W))
  private val lastCommittedInstReg = RegInit(0.U(32.W))
  private val commitCountReg = RegInit(0.U(32.W))

  val api: TraceApiDecl = new TraceApiDecl {
    override def issue(pc: UInt, inst: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_issue") { _ =>
      when(issueCount =/= depth.U) {
        issuePcQ(issueTail) := pc
        issueInstQ(issueTail) := inst
        issueTail := Mux(issueTail === (depth - 1).U, 0.U, issueTail + 1.U)
        issueCount := issueCount + 1.U
      }
    }

    override def commit(): HwInline[Unit] = HwInline.atomic(s"${name}_commit") { _ =>
      when(issueCount =/= 0.U) {
        lastCommittedPcReg := issuePcQ(issueHead)
        lastCommittedInstReg := issueInstQ(issueHead)
        issueHead := Mux(issueHead === (depth - 1).U, 0.U, issueHead + 1.U)
        issueCount := issueCount - 1.U
        commitCountReg := commitCountReg + 1.U
      }
    }
  }

  def lastCommittedPc: UInt = lastCommittedPcReg
  def lastCommittedInst: UInt = lastCommittedInstReg
  def commitCount: UInt = commitCountReg

  override def entry(): Unit = {}
}

final class TracerProcess(
    fetchRef: ApiRef[FetchApiDecl],
    regfileProbeRef: ApiRef[RegfileProbeApiDecl],
    csrProbeRef: ApiRef[CsrProbeApiDecl],
    hostClock: Clock,
    hostReset: Bool,
    depth: Int = 16,
    localName: String = "Tracer",
)(implicit kernel: Kernel)
    extends HwProcess(localName) {

  private val issuePcQ = RegInit(VecInit(Seq.fill(depth)(0.U(XLEN.W))))
  private val issueInstQ = RegInit(VecInit(Seq.fill(depth)(0.U(32.W))))
  private val issueHead = RegInit(0.U(log2Ceil(depth).W))
  private val issueTail = RegInit(0.U(log2Ceil(depth).W))
  private val issueCount = RegInit(0.U(log2Ceil(depth + 1).W))

  private val commitLock = spawn(new HwOS.stdlib.sync.MutexProcess(1, "CommitLock"))
  private val commitWorker = createThread("CommitWorker")

  private val dpi = spawn(new DpiProcess(hostClock, hostReset, "Dpi"))

  private val currentIssuePc = RegInit(0.U(XLEN.W))
  private val currentIssueInst = RegInit(0.U(32.W))
  private val commitIssued = RegInit(false.B)
  private val commitDone = RegInit(false.B)

  val api: TraceApiDecl = new TraceApiDecl {
    override def issue(pc: UInt, inst: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_issue") { _ =>
      when(issueCount =/= depth.U) {
        issuePcQ(issueTail) := pc
        issueInstQ(issueTail) := inst
        issueTail := Mux(issueTail === (depth - 1).U, 0.U, issueTail + 1.U)
        issueCount := issueCount + 1.U
      }
    }

    override def commit(): HwInline[Unit] = HwInline.atomic(s"${name}_commit") { t =>
      val lock = SysCall.Inline(commitLock.RequestLease(0))
      SysCall.Inline(lock.Acquire())
      when(!commitIssued && issueCount =/= 0.U) {
        currentIssuePc := issuePcQ(issueHead)
        currentIssueInst := issueInstQ(issueHead)
        issueHead := Mux(issueHead === (depth - 1).U, 0.U, issueHead + 1.U)
        issueCount := issueCount - 1.U
        commitDone := false.B
        commitIssued := true.B
        SysCall.Inline(SysCall.start(commitWorker))
      }
      t.waitCondition(commitDone)
      when(commitDone) {
        commitIssued := false.B
        SysCall.Inline(lock.Release())
      }
    }
  }

  def RequestTraceApi(): HwInline[TraceApiDecl] = HwInline.bindings(s"${name}_trace_api") { _ =>
    api
  }

  override def entry(): Unit = {
    commitWorker.entry {
      commitWorker.Step("SampleAndCommit") {
        val dpiApi = SysCall.Inline(dpi.RequestDpiApi())
        val fetchApi = SysCall.Inline(RequestFetchApi())
        val regProbe = SysCall.Inline(RequestRegfileProbeApi())
        val csrProbe = SysCall.Inline(RequestCsrProbeApi())
        val state = Wire(new SimStateBundle)
        state.valid := true.B
        state.pc := currentIssuePc
        state.inst := currentIssueInst
        state.dnpc := SysCall.Inline(fetchApi.currentPC())
        state.regsFlat := SysCall.Inline(regProbe.readAllFlat())
        state.mtvec := SysCall.Inline(csrProbe.mtvec())
        state.mepc := SysCall.Inline(csrProbe.mepc())
        state.mstatus := SysCall.Inline(csrProbe.mstatus())
        state.mcause := SysCall.Inline(csrProbe.mcause())
        SysCall.Inline(dpiApi.simState(state))
        commitDone := true.B
      }
      SysCall.Return()
    }
  }

  private def RequestFetchApi(): HwInline[FetchApiDecl] = HwInline.bindings(s"${name}_fetch_link") { _ =>
    fetchRef.get
  }

  private def RequestRegfileProbeApi(): HwInline[RegfileProbeApiDecl] = HwInline.bindings(s"${name}_regfile_probe_link") { _ =>
    regfileProbeRef.get
  }

  private def RequestCsrProbeApi(): HwInline[CsrProbeApiDecl] = HwInline.bindings(s"${name}_csr_probe_link") { _ =>
    csrProbeRef.get
  }
}
