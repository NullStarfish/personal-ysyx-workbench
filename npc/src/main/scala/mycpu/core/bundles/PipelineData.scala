package mycpu.core.bundles

import chisel3._
import chisel3.util._
import mycpu.common._

object ExecSubop {
  val None = "b000".U(3.W)
  val Byte = "b001".U(3.W)
  val Half = "b010".U(3.W)
  val Word = "b011".U(3.W)
}

object DecodeFormat {
  val None         = "b0000".U(4.W)
  val RegReg       = "b0001".U(4.W)
  val RegImm       = "b0010".U(4.W)
  val PcImm        = "b0011".U(4.W)
  val PcOffset     = "b0100".U(4.W)
  val RegOffset    = "b0101".U(4.W)
  val RegRegOffset = "b0110".U(4.W)
  val CsrReg       = "b0111".U(4.W)
  val CsrImm       = "b1000".U(4.W)
  val Sys          = "b1001".U(4.W)
}

class FetchPacket extends Bundle {
  val pc = XLenU
  val inst = UInt(32.W)
  val isException = Bool()
}

class FetchControlBundle extends Bundle {
  val stall = Bool()
  val redirect = Valid(UInt(XLEN.W))
}

class ExecuteDataBundle extends Bundle {
  val pc = XLenU
  val rs1 = XLenU
  val rs2 = XLenU
  val imm = XLenU
}

object ALUSrcA {
  val Rs1 = "b0".U(1.W)
  val Pc  = "b1".U(1.W)
}

object ALUSrcB {
  val Rs2 = "b0".U(1.W)
  val Imm = "b1".U(1.W)
}

object WBSel {
  val Alu    = "b00".U(2.W)
  val Csr    = "b01".U(2.W)
  val PcPlus4 = "b10".U(2.W)
}

object BranchType {
  val None = "b000".U(3.W)
  val Eq   = "b001".U(3.W)
  val Ne   = "b010".U(3.W)
  val Lt   = "b011".U(3.W)
  val Ge   = "b100".U(3.W)
  val Ltu  = "b101".U(3.W)
  val Geu  = "b110".U(3.W)
}

class BypassCtrlBundle extends Bundle {
  val rs1Addr = UInt(5.W)
  val rs2Addr = UInt(5.W)
}

class ExecuteCtrlBundle extends Bundle {
  val aluOp = ALUOp()
  val aluSrcA = UInt(1.W)
  val aluSrcB = UInt(1.W)
  val wbSel = UInt(2.W)
  val branchType = UInt(3.W)
  val isJump = Bool()
  val isJalr = Bool()
}

class WritebackCtrlBundle extends Bundle {
  val regWen = Bool()
  val rd = UInt(5.W)
}

class MemCtrlBundle extends Bundle {
  val valid = Bool()
  val write = Bool()
  val unsigned = Bool()
  val subop = UInt(3.W)
}

class SysCtrlBundle extends Bundle {
  val csrOp = CSROp()
  val csrAddr = UInt(12.W)
  val isEcall = Bool()
  val isMret = Bool()
  val isEbreak = Bool()
}

class CsrDebugBundle extends Bundle {
  val mtvec = XLenU
  val mepc = XLenU
  val mstatus = XLenU
  val mcause = XLenU
}

class BranchPredictionBundle extends Bundle {
  val predictedTaken = Bool()
  val index = UInt(5.W)
}

class TraceCarryBundle extends Bundle {
  val pc = XLenU
  val inst = UInt(32.W)
  val dnpc = XLenU
  val regWen = Bool()
  val rd = UInt(5.W)
  val data = XLenU
  val ifValid = Bool()
  val idValid = Bool()
  val exValid = Bool()
  val memValid = Bool()
  val branchResolved = Bool()
  val branchCorrect = Bool()
  val redirectValid = Bool()
  val redirectTarget = XLenU
  val actualTaken = Bool()
  val predictedTaken = Bool()
}

trait ForwardSourceView { this: Bundle =>
  def valid: Bool
  def addr: UInt
  def data: UInt
}

class DecodePacket(enableTraceFields: Boolean = ENABLE_TRACE_FIELDS) extends Bundle {
  val data = new ExecuteDataBundle
  val bypass = new BypassCtrlBundle
  val exec = new ExecuteCtrlBundle
  val wb = new WritebackCtrlBundle
  val mem = new MemCtrlBundle
  val sys = new SysCtrlBundle
  val pred = new BranchPredictionBundle
  val trace = if (enableTraceFields) Some(new TraceCarryBundle) else None
}

class ExecutePacket(enableTraceFields: Boolean = ENABLE_TRACE_FIELDS) extends Bundle with ForwardSourceView {
  val result = XLenU
  val rhs = XLenU
  val wb = new WritebackCtrlBundle
  val mem = new MemCtrlBundle
  val redirect = Bool()
  val bpUpdate = new BranchPredictUpdateBundle
  val trace = if (enableTraceFields) Some(new TraceCarryBundle) else None

  // EX forwarding only exposes pure execute results. Memory reads must wait for WB data.
  override def valid: Bool = wb.regWen && !mem.valid && (wb.rd =/= 0.U)
  override def addr: UInt = wb.rd
  override def data: UInt = result
}

class MemoryPacket(enableTraceFields: Boolean = ENABLE_TRACE_FIELDS) extends Bundle with ForwardSourceView {
  val wbData = XLenU
  val wb = new WritebackCtrlBundle
  val trace = if (enableTraceFields) Some(new TraceCarryBundle) else None

  override def valid: Bool = wb.regWen && (wb.rd =/= 0.U)
  override def addr: UInt = wb.rd
  override def data: UInt = wbData
}

class LsuStatusBundle extends Bundle {
  val pendingLoad = Bool()
  val pendingRd = UInt(5.W)
}

class CoreTraceBundle extends Bundle {
  val ifValid = Bool()
  val idValid = Bool()
  val exValid = Bool()
  val memValid = Bool()
  val retireCount = UInt(32.W)
  val lastRetire = Valid(new TraceCarryBundle)
  val branchCount = UInt(32.W)
  val branchCorrectCount = UInt(32.W)
}

class WriteBackIO extends Bundle {
  val wen  = Bool()
  val addr = UInt(5.W)
  val data = XLenU
}

class BranchPredictUpdateBundle extends Bundle {
  val valid = Bool()
  val index = UInt(5.W)
  val actualTaken = Bool()
  val predictedTaken = Bool()
}
