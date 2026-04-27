package mycpu.core.components

import chisel3._
import chisel3.util._

object ReadyValidHelper {
  def push[T <: Data](bus: ReadyValidIO[T], payload: T): Bool = {
    bus.valid := true.B
    bus.bits := payload
    bus.ready
  }
}
