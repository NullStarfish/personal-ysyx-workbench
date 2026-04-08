package mycpu.pipeline

import HwOS.kernel._
import chisel3._

final class PipelineEntry[T <: Data](gen: T) extends Bundle {
  val valid = Bool()
  val bits = gen.cloneType
}

final class PipelineBuffer[T <: Data](
    gen: T,
    localName: String = "PipelineBuffer",
)(implicit kernel: Kernel)
    extends HwProcess(localName) {

  private val entryReg = RegInit(0.U.asTypeOf(new PipelineEntry(gen)))
  private val clearPendingReg = RegInit(false.B)
  private val canPushNode = !entryReg.valid
  private val canPopNode = entryReg.valid

  when(clearPendingReg) {
    entryReg.valid := false.B
    clearPendingReg := false.B
  }

  def packet: PipelineEntry[T] = entryReg
  def bits: T = entryReg.bits
  def valid: Bool = entryReg.valid
  def ready: Bool = canPushNode

  def push(data: T): HwInline[Unit] =
    HwInline.atomic(s"${name}_push") { t =>
      t.waitCondition(canPushNode)
      t.Prev.edge.add {
        entryReg.bits := data
        entryReg.valid := true.B
      }
    }

  def pushAssign(assign: T => Unit): HwInline[Unit] =
    HwInline.atomic(s"${name}_push_assign") { t =>
      t.waitCondition(canPushNode)
      t.Prev.edge.add {
        assign(entryReg.bits)
        entryReg.valid := true.B
      }
    }

  def pop(): HwInline[T] =
    HwInline.atomic(s"${name}_pop") { t =>
      t.waitCondition(canPopNode)
      val out = entryReg.bits
      t.Prev.edge.add {
        entryReg.valid := false.B
      }
      out
    }

  def clear(): HwInline[Unit] =
    HwInline.stateless(s"${name}_clear") { _ =>
      clearPendingReg := true.B
    }
}
