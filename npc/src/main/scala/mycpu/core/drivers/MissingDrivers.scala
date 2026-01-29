package mycpu.core.drivers

import chisel3._
import chisel3.util._
import mycpu.core.kernel._
import mycpu.utils._
import mycpu.common._

class PipeDriver(val name: String, depth: Int) extends PhysicalDriver(
  DriverMeta(name, DriverTiming.Sequential, DriverTiming.Sequential)
) {
  val queue = Module(new Queue(UInt(KERNEL_DATA_WIDTH.W), depth)) 

  // 定义内部代理信号
  private var p_enq_valid: Bool = _
  private var p_enq_bits:  UInt = _
  private var p_deq_ready: Bool = _

  override def setup(agent: HardwareAgent): Unit = {
    // 使用 managed 机制，由 Kernel Arbiter 驱动
    p_enq_valid = agent.driveManaged(queue.io.enq.valid, false.B)
    p_enq_bits  = agent.driveManaged(queue.io.enq.bits,  0.U)
    p_deq_ready = agent.driveManaged(queue.io.deq.ready, false.B)
  }

  override def seqRead(addr: UInt, size: UInt): (UInt, UInt, Bool) = {
    p_deq_ready := true.B
    val data = queue.io.deq.bits
    val done = queue.io.deq.valid 
    val err  = Mux(done, Errno.ESUCCESS, Errno.EBUSY) 
    (data, err, done)
  }

  override def seqWrite(addr: UInt, data: UInt, size: UInt): (UInt, Bool) = {
    p_enq_valid := true.B
    p_enq_bits  := data
    
    val done = queue.io.enq.ready
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