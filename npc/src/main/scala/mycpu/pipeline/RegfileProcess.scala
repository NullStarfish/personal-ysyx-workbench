package mycpu.pipeline

import HwOS.kernel._
import HwOS.lib.regfile.RegfileLib._
import chisel3._
import mycpu.common._

final class RegfileProcess(
    localName: String = "Regfile",
)(implicit kernel: Kernel)
    extends HwProcess(localName) {

  private val regfile =
    spawn(new AgeOrderedScoreboardRegfileProcess(depth = 32, width = XLEN, maxWriters = 1, maxInFlight = 8, zeroReg = true, localName = "Base"))

  val api: RegfileApiDecl = new RegfileApiDecl {
    override def read(addr: UInt): HwInline[UInt] = {printf(p"[REGFILE TOP] reg read $addr\n");regfile.Read(addr)}

    override def write(addr: UInt, data: UInt): HwInline[Unit] = {printf(p"[REGFILE TOP] reg write $addr, $data\n");regfile.Write(0, addr, data)}
  }

  val probeApi: RegfileProbeApiDecl = new RegfileProbeApiDecl {
    override def read(addr: UInt): HwInline[UInt] = regfile.ReadCommitted(addr)

    override def readAllFlat(): HwInline[UInt] = regfile.ObserveFlat()
  }

  def RequestRegfileApi(): HwInline[RegfileApiDecl] = HwInline.bindings(s"${name}_regfile_api") { _ =>
    api
  }

  def RequestRegfileProbeApi(): HwInline[RegfileProbeApiDecl] = HwInline.bindings(s"${name}_regfile_probe_api") { _ =>
    probeApi
  }

  override def entry(): Unit = {}
}
