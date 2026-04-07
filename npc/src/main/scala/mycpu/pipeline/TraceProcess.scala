package mycpu.pipeline

import HwOS.kernel._
import chisel3._
import chisel3.util._

final class NoopTraceProcess(localName: String = "Trace")(implicit kernel: Kernel) extends HwProcess(localName) {
  val api: TraceApiDecl = new TraceApiDecl {
    override def issue(pc: UInt, inst: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_issue") { _ => }
    override def commit(): HwInline[Unit] = HwInline.atomic(s"${name}_commit") { _ => }
  }

  override def entry(): Unit = {}
}

final class DummyTraceProcess(depth: Int = 16, localName: String = "Trace")(implicit kernel: Kernel) extends HwProcess(localName) {
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
