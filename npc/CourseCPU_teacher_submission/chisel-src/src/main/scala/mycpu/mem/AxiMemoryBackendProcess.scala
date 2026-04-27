package mycpu.mem

import HwOS.kernel._
import chisel3._
import mycpu.axi._
import mycpu.axi.AXI4Api._

class AxiMemoryBackendProcess(bus: AXI4Bundle, localName: String = "AxiBackend")(implicit kernel: Kernel)
    extends HwProcess(localName) {

  val api: MemoryBackendApiDecl = new MemoryBackendApiDecl {
    override def read_once(addr: UInt, size: UInt): HwInline[UInt] =
      AXI4Api.axi_read_once(bus, 0.U, addr, size)

    override def write_once(addr: UInt, size: UInt, data: UInt, strb: UInt): HwInline[Unit] =
      AXI4Api.axi_write_once(bus, 0.U, addr, size, data, strb)
  }
}
