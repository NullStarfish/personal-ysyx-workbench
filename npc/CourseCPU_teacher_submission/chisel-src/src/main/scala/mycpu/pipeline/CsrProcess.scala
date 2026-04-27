package mycpu.pipeline

import HwOS.kernel._
import chisel3._
import chisel3.util._
import mycpu.common._

final class CsrProcess(
    localName: String = "Csr",
)(implicit kernel: Kernel)
    extends HwProcess(localName) {

  private val MSTATUS = "h300".U(12.W)
  private val MTVEC = "h305".U(12.W)
  private val MEPC = "h341".U(12.W)
  private val MCAUSE = "h342".U(12.W)

  private val mstatusReg = RegInit("h00001800".U(XLEN.W))
  private val mtvecReg = RegInit(0.U(XLEN.W))
  private val mepcReg = RegInit(0.U(XLEN.W))
  private val mcauseReg = RegInit(0.U(XLEN.W))

  private def readCsr(addr: UInt): UInt =
    MuxLookup(addr, 0.U(XLEN.W))(Seq(
      MSTATUS -> mstatusReg,
      MTVEC -> mtvecReg,
      MEPC -> mepcReg,
      MCAUSE -> mcauseReg,
    ))

  private def writeCsr(addr: UInt, value: UInt): Unit = {
    when(addr === MSTATUS) {
      mstatusReg := value
    }.elsewhen(addr === MTVEC) {
      mtvecReg := value
    }.elsewhen(addr === MEPC) {
      mepcReg := value
    }.elsewhen(addr === MCAUSE) {
      mcauseReg := value
    }
  }

  val api: CsrApiDecl = new CsrApiDecl {
    override def rw(addr: UInt, src: UInt): HwInline[UInt] = HwInline.atomic(s"${name}_rw") { _ =>
      val oldValue = readCsr(addr)
      writeCsr(addr, src)
      printf(p"[CSR] rw addr=${Hexadecimal(addr)} old=${Hexadecimal(oldValue)} new=${Hexadecimal(src)}\n")
      oldValue
    }

    override def rs(addr: UInt, src: UInt): HwInline[UInt] = HwInline.atomic(s"${name}_rs") { _ =>
      val oldValue = readCsr(addr)
      val newValue = oldValue | src
      writeCsr(addr, newValue)
      printf(p"[CSR] rs addr=${Hexadecimal(addr)} old=${Hexadecimal(oldValue)} src=${Hexadecimal(src)} new=${Hexadecimal(newValue)}\n")
      oldValue
    }

    override def rc(addr: UInt, src: UInt): HwInline[UInt] = HwInline.atomic(s"${name}_rc") { _ =>
      val oldValue = readCsr(addr)
      val newValue = oldValue & (~src).asUInt
      writeCsr(addr, newValue)
      printf(p"[CSR] rc addr=${Hexadecimal(addr)} old=${Hexadecimal(oldValue)} src=${Hexadecimal(src)} new=${Hexadecimal(newValue)}\n")
      oldValue
    }
  }

  val probeApi: CsrProbeApiDecl = new CsrProbeApiDecl {
    override def read(addr: UInt): HwInline[UInt] = HwInline.bindings(s"${name}_csr_probe_read") { _ =>
      readCsr(addr)
    }

    override def mtvec(): HwInline[UInt] = read(MTVEC)
    override def mepc(): HwInline[UInt] = read(MEPC)
    override def mstatus(): HwInline[UInt] = read(MSTATUS)
    override def mcause(): HwInline[UInt] = read(MCAUSE)
  }

  def RequestCsrApi(): HwInline[CsrApiDecl] = HwInline.bindings(s"${name}_csr_api") { _ =>
    api
  }

  def RequestCsrProbeApi(): HwInline[CsrProbeApiDecl] = HwInline.bindings(s"${name}_csr_probe_api") { _ =>
    probeApi
  }

  override def entry(): Unit = {}
}
