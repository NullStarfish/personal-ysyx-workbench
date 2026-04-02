package mycpu.pipeline

import HwOS.kernel._
import HwOS.lib.regfile.RegfileLib._
import chisel3._
import mycpu.common._

final class RegfileProcess(
    localName: String = "Regfile",
)(implicit kernel: Kernel)
    extends HwProcess(localName) {

  private val base = spawn(new BaseRegfileProcess(depth = 32, width = XLEN, zeroReg = true, localName = "Base"))

  final class RegfileApi {
    def read(addr: UInt): HwInline[UInt] = base.Read(addr)

    def write(addr: UInt, data: UInt): HwInline[Unit] = base.Write(addr, data)
  }

  private val regfileApi = new RegfileApi

  def RequestRegfileApi(): HwInline[RegfileApi] = HwInline.bindings(s"${name}_regfile_api") { _ =>
    regfileApi
  }

  override def entry(): Unit = {}
}
