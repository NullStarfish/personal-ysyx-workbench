package mycpu.core.bundles

import chisel3._
import chisel3.util._
import mycpu.common._

object ExecFamily {
  val Alu    = "b000".U(3.W)
  val Branch = "b001".U(3.W)
  val Jump   = "b010".U(3.W)
  val Upper  = "b011".U(3.W)
  val Csr    = "b100".U(3.W)
  val Mem    = "b101".U(3.W)
}

object ExecOp {
  val Nop   = "b0000".U(4.W)
  val Add   = "b0001".U(4.W)
  val Sub   = "b0010".U(4.W)
  val And   = "b0011".U(4.W)
  val Or    = "b0100".U(4.W)
  val Xor   = "b0101".U(4.W)
  val Slt   = "b0110".U(4.W)
  val Sltu  = "b0111".U(4.W)
  val Sll   = "b1000".U(4.W)
  val Srl   = "b1001".U(4.W)
  val Sra   = "b1010".U(4.W)
  val Lui   = "b1011".U(4.W)
  val Auipc = "b1100".U(4.W)
  val Jal   = "b1101".U(4.W)
  val Jalr  = "b1110".U(4.W)
  val Load  = "b1111".U(4.W)
  val Store = "b0000".U(4.W)
  val CsrRw = "b0001".U(4.W)
  val CsrRs = "b0010".U(4.W)
  val CsrRc = "b0011".U(4.W)
  val Beq   = "b0100".U(4.W)
  val Bne   = "b0101".U(4.W)
  val Blt   = "b0110".U(4.W)
  val Bge   = "b0111".U(4.W)
  val Bltu  = "b1000".U(4.W)
  val Bgeu  = "b1001".U(4.W)
}

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
  val lhs = XLenU
  val rhs = XLenU
  val offset = XLenU
}

class ExecuteOpBundle extends Bundle {
  val family = UInt(3.W)
  val op = UInt(4.W)
  val subop = UInt(3.W)
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
}

class TraceCarryBundle extends Bundle {
  val pc = XLenU
  val inst = UInt(32.W)
  val dnpc = XLenU
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

class DecodePacket(enableTraceFields: Boolean = ENABLE_TRACE_FIELDS) extends Bundle {
  val data = new ExecuteDataBundle
  val exec = new ExecuteOpBundle
  val wb = new WritebackCtrlBundle
  val mem = new MemCtrlBundle
  val sys = new SysCtrlBundle
  val pred = new BranchPredictionBundle
  val trace = if (enableTraceFields) Some(new TraceCarryBundle) else None
}

class ExecutePacket(enableTraceFields: Boolean = ENABLE_TRACE_FIELDS) extends Bundle {
  val result = XLenU
  val rhs = XLenU
  val wb = new WritebackCtrlBundle
  val mem = new MemCtrlBundle
  val redirect = Valid(UInt(XLEN.W))
  val trace = if (enableTraceFields) Some(new TraceCarryBundle) else None
}

class MemoryPacket(enableTraceFields: Boolean = ENABLE_TRACE_FIELDS) extends Bundle {
  val wbData = XLenU
  val wb = new WritebackCtrlBundle
  val trace = if (enableTraceFields) Some(new TraceCarryBundle) else None
}

class LsuStatusBundle extends Bundle {
  val pendingLoad = Bool()
  val pendingRd = UInt(5.W)
}

class RetireEventBundle extends Bundle {
  val valid = Bool()
  val pc = XLenU
  val dnpc = XLenU
  val inst = UInt(32.W)
  val regWen = Bool()
  val rd = UInt(5.W)
  val data = XLenU
}

class CoreTraceBundle extends Bundle {
  val ifValid = Bool()
  val idValid = Bool()
  val exValid = Bool()
  val memValid = Bool()
  val retireCount = UInt(32.W)
  val lastRetire = new RetireEventBundle
  val branchCount = UInt(32.W)
  val branchCorrectCount = UInt(32.W)
}

class WriteBackIO extends Bundle {
  val wen  = Bool()
  val addr = UInt(5.W)
  val data = XLenU
}

class ForwardInfo extends Bundle {
  val valid = Bool()
  val addr  = UInt(5.W)
  val data  = XLenU
}

class BranchPredictUpdateBundle extends Bundle {
  val valid = Bool()
  val pc = XLenU
  val actualTaken = Bool()
  val predictedTaken = Bool()
}
