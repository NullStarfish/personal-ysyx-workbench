package mycpu.core.bundles
import chisel3._
import mycpu.common._
class ExecuteDataBundle extends Bundle {
  val pc = XLenU
  val rs1 = XLenU
  val rs2 = XLenU
  val imm = XLenU
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


class ExecutePacket(enableTraceFields: Boolean = ENABLE_TRACE_FIELDS) extends Bundle with withRetireTrace with ForwardSourceView {
  val result = XLenU
  val rhs = XLenU
  val wb = new WritebackCtrlBundle
  val mem = new MemCtrlBundle
  val redirect = Bool()

  // EX forwarding only exposes pure execute results. Memory reads must wait for WB data.
  override def valid: Bool = wb.wen && !mem.valid && (wb.rd =/= 0.U)
  override def addr: UInt = wb.rd
  override def data: UInt = result
}