package mycpu.compatibility
import chisel3._
import chisel3.util._
import HwOS.kernel._
object DecoupledApi {
    def push[T <: Data](bus: ReadyValidIO[T], data: T): HwInline[Unit] = HwInline.atomic("ready-valid push") { t =>
        bus.bits := data
        bus.valid := true.B
        t.waitCondition(bus.ready)
    }
    def pop[T <: Data](bus: ReadyValidIO[T]): HwInline[T] = HwInline.atomic("ready-valid pop") { t=>
        bus.ready := true.B
        t.waitCondition(bus.valid)
        bus.bits
    }
}