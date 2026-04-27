package mycpu.mem

import HwOS.kernel._

class DummyMemory(
    readonlyWords: Seq[(BigInt, BigInt)],
    mutableWords: Seq[(BigInt, BigInt)],
    maxClients: Int,
    localName: String = "DummyMemory",
)(implicit kernel: Kernel)
    extends SharedMemoryProcess(localName, maxClients) {

  private val backend = spawn(new DirectSramBackendProcess(readonlyWords, mutableWords, "DirectSramBackend"))
  backendRef.bind(backend.api)

  val mutableData: Seq[chisel3.UInt] = backend.mutableData
}
