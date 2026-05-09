package mycpu.core.bundles
import chisel3._
import chisel3.util._
import mycpu.common._

class RegReadMeta extends Bundle {
  val addr = UInt(5.W)
  val rdata = XLenU
}

class DecodePacket extends Bundle with withRetireTrace{
  val rs1 = Valid(new RegReadMeta)
  val rs2 = Valid(new RegReadMeta)
  val pc  = XLenU
  val imm = UInt(32.W)
  val exec = new ExecuteCtrlBundle
  val wb = new WritebackCtrlBundle
  val mem = new MemCtrlBundle
  val sys = new SysCtrlBundle(true, true)
}


class SysCtrlBundle(enableSys: Boolean = true, enableSimEbreak: Boolean = true) extends Bundle {
  val csrOp = if (enableSys) Some(CSROp()) else None
  val csrAddr = if (enableSys) Some(UInt(12.W)) else None
  val isEcall = if (enableSys) Some(Bool()) else None
  val isMret = if (enableSys) Some(Bool()) else None
  val isEbreak = if (enableSimEbreak) Some(Bool()) else None
}