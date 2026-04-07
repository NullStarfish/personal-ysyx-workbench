package mycpu.mem

import HwOS.kernel._
import HwOS.stdlib.sync._
import chisel3._
import mycpu.pipeline.{ApiRef, MemoryApiDecl}

abstract class SharedMemoryProcess(localName: String, maxClients: Int)(implicit kernel: Kernel)
    extends HwProcess(localName) {

  protected val backendRef = new ApiRef[MemoryBackendApiDecl]
  private val busLock = spawn(new MutexProcess(maxClients, "BusLock"))

  class MemoryApi(id: Int) extends MemoryApiDecl {
    override def read_once(addr: UInt, size: UInt): HwInline[UInt] = HwInline.thread("mem read") { t =>
      val stepTag = s"mem_read_${id}_${System.identityHashCode(new Object())}"
      val lock = SysCall.Inline(busLock.RequestLease(id))
      val backend = backendRef.get
      t.Step(s"${stepTag}_AcquireLock") {
        SysCall.Inline(lock.Acquire())
      }
      val result = SysCall.Inline(backend.read_once(addr, size))
      t.Prev.edge.add {
        SysCall.Inline(lock.Release())
      }
      result
    }

    override def write_once(addr: UInt, size: UInt, data: UInt, strb: UInt): HwInline[Unit] = HwInline.thread("mem_write") { t =>
      val stepTag = s"mem_write_${id}_${System.identityHashCode(new Object())}"
      val lock = SysCall.Inline(busLock.RequestLease(id))
      val backend = backendRef.get
      t.Step(s"${stepTag}_AcquireLock") {
        SysCall.Inline(lock.Acquire())
      }
      SysCall.Inline(backend.write_once(addr, size, data, strb))
      t.Prev.edge.add {
        SysCall.Inline(lock.Release())
      }
    }
  }

  protected val apiVec = Array.tabulate(maxClients)(i => new MemoryApi(i))

  def api(id: Int): MemoryApiDecl = apiVec(id)

  def RequestMemoryApi(id: Int): HwInline[MemoryApiDecl] = HwInline.bindings(s"Req Mem_$id") { _ =>
    apiVec(id)
  }
}
