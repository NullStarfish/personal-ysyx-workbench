package mycpu.core.components

import chisel3._
import chisel3.util._


class FlushableStage[T <: Data](gen: T, entries:Int = 1) extends Module {
  val io = IO(new Bundle {
    val enq = Flipped(Decoupled(gen))
    val deq = Decoupled(gen)
    val flush = Input(Bool())
    val stall = Input(Bool())
  })

  val queue = Module(new Queue(gen, entries = entries, pipe = true, hasFlush = true))
  queue.io.enq <> io.enq
  io.deq <> queue.io.deq
  queue.flush <> io.flush


  when (io.stall) {
    queue.io.enq.valid := false.B
    io.enq.ready := false.B
  }

}
