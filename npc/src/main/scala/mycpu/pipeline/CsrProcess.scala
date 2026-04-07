package mycpu.pipeline

import HwOS.kernel._
import chisel3._
import mycpu.common._

final class CsrProcess(
    localName: String = "Csr",
)(implicit kernel: Kernel)
    extends HwProcess(localName) {

  private val csrFile = RegInit(VecInit(Seq.fill(4096)(0.U(XLEN.W))))

  val api: CsrApiDecl = new CsrApiDecl {
    override def rw(addr: UInt, src: UInt): HwInline[UInt] = HwInline.atomic(s"${name}_rw") { _ =>
      val oldValue = csrFile(addr)
      csrFile(addr) := src
      printf(p"[CSR] rw addr=${Hexadecimal(addr)} old=${Hexadecimal(oldValue)} new=${Hexadecimal(src)}\n")
      oldValue
    }

    override def rs(addr: UInt, src: UInt): HwInline[UInt] = HwInline.atomic(s"${name}_rs") { _ =>
      val oldValue = csrFile(addr)
      val newValue = oldValue | src
      csrFile(addr) := newValue
      printf(p"[CSR] rs addr=${Hexadecimal(addr)} old=${Hexadecimal(oldValue)} src=${Hexadecimal(src)} new=${Hexadecimal(newValue)}\n")
      oldValue
    }

    override def rc(addr: UInt, src: UInt): HwInline[UInt] = HwInline.atomic(s"${name}_rc") { _ =>
      val oldValue = csrFile(addr)
      val newValue = oldValue & (~src).asUInt
      csrFile(addr) := newValue
      printf(p"[CSR] rc addr=${Hexadecimal(addr)} old=${Hexadecimal(oldValue)} src=${Hexadecimal(src)} new=${Hexadecimal(newValue)}\n")
      oldValue
    }
  }

  val probeApi: CsrProbeApiDecl = new CsrProbeApiDecl {
    override def read(addr: UInt): HwInline[UInt] = HwInline.bindings(s"${name}_csr_probe_read") { _ =>
      csrFile(addr)
    }

    override def mtvec(): HwInline[UInt] = read("h305".U(12.W))
    override def mepc(): HwInline[UInt] = read("h341".U(12.W))
    override def mstatus(): HwInline[UInt] = read("h300".U(12.W))
    override def mcause(): HwInline[UInt] = read("h342".U(12.W))
  }

  def RequestCsrApi(): HwInline[CsrApiDecl] = HwInline.bindings(s"${name}_csr_api") { _ =>
    api
  }

  def RequestCsrProbeApi(): HwInline[CsrProbeApiDecl] = HwInline.bindings(s"${name}_csr_probe_api") { _ =>
    probeApi
  }

  override def entry(): Unit = {}
}
