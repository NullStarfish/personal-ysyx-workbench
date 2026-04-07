package mycpu.mem
import chisel3._
import HwOS.kernel._
import mycpu.axi._

//全局单例
class AXIMemory(bus: AXI4Bundle, maxClients: Int)(implicit kernel: Kernel)
    extends SharedMemoryProcess("Memory Process", maxClients) {
    private val backend = spawn(new AxiMemoryBackendProcess(bus, "AxiBackend"))
    backendRef.bind(backend.api)
}
