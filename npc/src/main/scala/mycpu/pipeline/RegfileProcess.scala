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

  val probeApi: RegfileProbeApiDecl = new RegfileProbeApiDecl {
    override def read(addr: UInt): HwInline[UInt] = base.Read(addr)

    override def readAllFlat(): HwInline[UInt] = base.ObserveFlat()
  }

  def RequestRegfileApi(): HwInline[RegfileApiDecl] = HwInline.bindings(s"${name}_regfile_api") { _ =>
    api
  }

  def RequestRegfileProbeApi(): HwInline[RegfileProbeApiDecl] = HwInline.bindings(s"${name}_regfile_probe_api") { _ =>
    probeApi
  }

  override def entry(): Unit = {}
}
