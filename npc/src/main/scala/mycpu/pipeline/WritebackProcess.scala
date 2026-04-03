package mycpu.pipeline

import HwOS.kernel._
import chisel3._

final class WritebackProcess(
    fetchRef: ApiRef[FetchApiDecl],
    regfileRef: ApiRef[RegfileApiDecl],
    localName: String = "Writeback",
)(implicit kernel: Kernel)
    extends HwProcess(localName) {

  val api: WritebackApiDecl = new WritebackApiDecl {
    private def fetchApi = fetchRef.get
    private def regApi = regfileRef.get

    override def writeReg(rd: UInt, data: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_write_reg") { _ =>
      printf(p"[WB] writeReg rd=${Decimal(rd)} data=${Hexadecimal(data)}\n")
      SysCall.Inline(regApi.write(rd, data))
    }

    override def redirect(nextPc: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_redirect") { _ =>
      printf(p"[WB] redirect nextPc=${Hexadecimal(nextPc)}\n")
      SysCall.Inline(fetchApi.writePC(nextPc))
    }

    override def redirectRelative(delta: SInt): HwInline[Unit] = HwInline.atomic(s"${name}_redirect_relative") { _ =>
      printf(p"[WB] redirectRelative delta=${Hexadecimal(delta.asUInt)}\n")
      SysCall.Inline(fetchApi.offsetPC(delta))
    }
  }

  def RequestWritebackApi(): HwInline[WritebackApiDecl] = HwInline.bindings(s"${name}_writeback_api") { _ =>
    api
  }

  override def entry(): Unit = {}
}
