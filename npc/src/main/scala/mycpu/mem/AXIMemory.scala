package mycpu.mem
import chisel3._
import HwOS.kernel._
import HwOS.stdlib.sync._
import mycpu.axi._
import mycpu.axi.AXI4Api._
import mycpu.pipeline.MemoryApiDecl

//全局单例
class Memory(bus: AXI4Bundle, maxClients: Int)(implicit kernel: Kernel) extends HwProcess("Memory Process") {
    private val busLock = spawn(new MutexProcess(maxClients, "BusLock"))

    class MemoryApi(id: Int) extends MemoryApiDecl {
        override def read_once(addr: UInt, size: UInt): HwInline[UInt] = HwInline.thread("mem read") { t=>
            val stepTag = s"mem_read_${id}_${System.identityHashCode(new Object())}"
            val lock = SysCall.Inline(busLock.RequestLease(id))
            t.Step(s"${stepTag}_AcquireLock") {
                SysCall.Inline(lock.Acquire())
            }
            val result = SysCall.Inline(axi_read_once(bus, id.U, addr, size))
            t.Prev.edge.add {
                SysCall.Inline(lock.Release())
            }
            result
        }
        override def write_once(addr: UInt, size: UInt, data: UInt, strb: UInt): HwInline[Unit] = HwInline.thread("axi_write") { t =>
            val stepTag = s"mem_write_${id}_${System.identityHashCode(new Object())}"
            val lock = SysCall.Inline(busLock.RequestLease(id))

            t.Step(s"${stepTag}_AcquireLock") {
                SysCall.Inline(lock.Acquire())
            }
            SysCall.Inline(axi_write_once(bus, id.U, addr, size, data, strb))
            t.Prev.edge.add {
                SysCall.Inline(lock.Release())
            }
        }
    }

    val apiVec = Array.tabulate(maxClients)(i => new MemoryApi(i))
    def api(id: Int): MemoryApiDecl = apiVec(id)
    def RequestMemoryApi(id: Int): HwInline[MemoryApiDecl] = HwInline.bindings(s"Req Mem_$id") { _ =>
        apiVec(id)
    }
}
