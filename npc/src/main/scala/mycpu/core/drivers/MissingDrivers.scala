package mycpu.core.drivers

import chisel3._
import chisel3.util._
import mycpu.core.kernel._
import mycpu.utils._
import mycpu.common._

class PipeDriver(val name: String, depth: Int) extends PhysicalDriver(
  DriverMeta(name, DriverTiming.Sequential, DriverTiming.Sequential)
) {
  // [修复] Queue 使用 KERNEL_DATA_WIDTH (64位)
  val queue = Module(new Queue(UInt(KERNEL_DATA_WIDTH.W), depth)) 

  val io_enq = Wire(Decoupled(UInt(KERNEL_DATA_WIDTH.W)))
  val io_deq = Wire(Decoupled(UInt(KERNEL_DATA_WIDTH.W)))
  
  queue.io.enq <> io_enq
  queue.io.deq <> io_deq

  io_enq.valid := false.B; io_enq.bits := 0.U
  io_deq.ready := false.B

  override def seqRead(addr: UInt, size: UInt): (UInt, UInt, Bool) = {
    io_deq.ready := true.B
    val data = io_deq.bits
    val done = io_deq.valid 
    val err  = Mux(done, Errno.ESUCCESS, Errno.EBUSY) 
    (data, err, done)
  }

  override def seqWrite(addr: UInt, data: UInt, size: UInt): (UInt, Bool) = {
    io_enq.valid := true.B
    io_enq.bits  := data
    
    val done = io_enq.ready
    val err  = Mux(done, Errno.ESUCCESS, Errno.EBUSY)
    (err, done)
  }
}

class TerminalDriver(val name: String) extends PhysicalDriver(
  DriverMeta(name, DriverTiming.Sequential, DriverTiming.Sequential)
) {
  override def seqWrite(addr: UInt, data: UInt, size: UInt): (UInt, Bool) = {
    printf("%c", data(7,0))
    (Errno.ESUCCESS, true.B)
  }
}