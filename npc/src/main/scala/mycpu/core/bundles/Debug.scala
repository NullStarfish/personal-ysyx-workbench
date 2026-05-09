package mycpu.core.bundles

import chisel3._
import mycpu.common._


class CsrDebugBundle extends Bundle {
  val mtvec = XLenU
  val mepc = XLenU
  val mstatus = XLenU
  val mcause = XLenU
}