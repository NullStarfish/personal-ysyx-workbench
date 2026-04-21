package labcpu.core

import mycpu.core.bundles._
import org.scalatest.flatspec.AnyFlatSpec

class TracePacketShapeSpec extends AnyFlatSpec {
  "Pipeline packets" should "exclude tracer-only carry fields from runtime bundles" in {
    val fetch = new FetchPacket
    val decode = new DecodePacket
    val execute = new ExecutePacket
    val memory = new MemoryPacket

    assert(!fetch.elements.contains("dnpc"))
    assert(!decode.elements.contains("retire"))
    assert(!execute.elements.contains("retire"))
    assert(!memory.elements.contains("retire"))
  }
}
