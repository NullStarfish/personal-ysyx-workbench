package mycpu.pipeline

import HwOS.kernel._
import chisel3._

final class ControlHazardProcess(
    fetchApi: FetchApiDecl,
    traceApi: TraceApiDecl,
    clearActions: Seq[() => HwInline[Unit]],
    localName: String = "ControlHazard",
)(implicit kernel: Kernel)
    extends HwProcess(localName) {

  val api: ControlHazardApiDecl = new ControlHazardApiDecl {
    private def doClearAndWritePc(nextPc: UInt): Unit = {
      clearActions.foreach { clear =>
        SysCall.Inline(clear())
      }
      SysCall.Inline(fetchApi.writePC(nextPc))
    }

    private def doClearAndOffsetPc(delta: SInt): Unit = {
      clearActions.foreach { clear =>
        SysCall.Inline(clear())
      }
      SysCall.Inline(fetchApi.offsetPC(delta))
    }

    override def redirect(nextPc: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_redirect") { _ =>
      doClearAndWritePc(nextPc)
      SysCall.Inline(traceApi.commit())
    }

    override def redirectRelative(delta: SInt): HwInline[Unit] = HwInline.atomic(s"${name}_redirect_relative") { _ =>
      doClearAndOffsetPc(delta)
      SysCall.Inline(traceApi.commit())
    }

    override def redirectNoCommit(nextPc: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_redirect_no_commit") { _ =>
      doClearAndWritePc(nextPc)
    }

    override def redirectRelativeNoCommit(delta: SInt): HwInline[Unit] = HwInline.atomic(s"${name}_redirect_relative_no_commit") { _ =>
      doClearAndOffsetPc(delta)
    }
  }

  def RequestControlHazardApi(): HwInline[ControlHazardApiDecl] = HwInline.bindings(s"${name}_hazard_api") { _ =>
    api
  }

  override def entry(): Unit = {}
}
