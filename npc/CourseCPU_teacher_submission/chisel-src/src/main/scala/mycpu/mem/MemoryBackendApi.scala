package mycpu.mem

import HwOS.kernel.function.HwInline
import chisel3._

trait MemoryBackendApiDecl {
  def read_once(addr: UInt, size: UInt): HwInline[UInt]
  def write_once(addr: UInt, size: UInt, data: UInt, strb: UInt): HwInline[Unit]
}
