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

  val api: RegfileApiDecl = new RegfileApiDecl {
    override def read(addr: UInt): HwInline[UInt] = base.Read(addr)

    override def write(addr: UInt, data: UInt): HwInline[Unit] = base.Write(addr, data)
  }

  def RequestRegfileApi(): HwInline[RegfileApiDecl] = HwInline.bindings(s"${name}_regfile_api") { _ =>
    api
  }

  override def entry(): Unit = {}
}
