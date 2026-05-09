package mycpu.core.bundles
import chisel3._
import mycpu.common._


class MemCtrlBundle extends Bundle {
  val valid = Bool()
  val write = Bool()
  val unsigned = Bool()
  val subop = UInt(3.W)
}



class MemoryPacket(enableTraceFields: Boolean = ENABLE_TRACE_FIELDS) extends Bundle with withRetireTrace with ForwardSourceView {
  val wbData = XLenU
  val wb = new WritebackCtrlBundle

  override def valid: Bool = wb.wen && (wb.rd =/= 0.U)
  override def addr: UInt = wb.rd
  override def data: UInt = wbData
}