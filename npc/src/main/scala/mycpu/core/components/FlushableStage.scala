package mycpu.core.components

import chisel3._
import chisel3.util._

class FlushableStage[T <: Data](gen: T, entries: Int = 1) extends Module {
  val io = IO(new Bundle {
    val enq = Flipped(Decoupled(gen))
    val deq = Decoupled(gen)
    val flush = Input(Bool())
    val blockEnq = Input(Bool())
    val blockDeq = Input(Bool())
  })

  val queue = Module(new Queue(gen, entries = entries, pipe = true, hasFlush = true))

  queue.io.enq.valid := io.enq.valid && !io.blockEnq
  queue.io.enq.bits := io.enq.bits
  io.enq.ready := queue.io.enq.ready && !io.blockEnq

  io.deq.valid := queue.io.deq.valid
  io.deq.bits := queue.io.deq.bits
  queue.io.deq.ready := io.deq.ready && !io.blockDeq


  queue.flush <> io.flush
}
