package mycpu.core.bundles

import chisel3._
import mycpu.common._

class TraceBase extends Bundle

object TraceVal {
  def apply[T <: Data](gen: T) = if (ENABLE_TRACE_FIELDS) Some(gen) else None
}

class RetireTrace extends TraceBase {
  val pc = XLenU
  val inst = UInt(32.W)
  val dnpc = XLenU
  val icacheHit = Bool()
  val regWrite = new RegWriteMeta
  val instType = InstType()
  val csrs = new CsrDebugBundle
}

trait withRetireTrace {
  val retireTrace = if (ENABLE_TRACE_FIELDS) Some(new RetireTrace) else None
}
