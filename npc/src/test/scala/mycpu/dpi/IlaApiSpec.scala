package mycpu.dpi

import chisel3._
import circt.stage.ChiselStage
import org.scalatest.flatspec.AnyFlatSpec

class IlaApiSpec extends AnyFlatSpec {
  private class Harness(source: String, duplicate: Boolean = false)
      extends Module {
    val io = IO(new Bundle {
      val a = Input(UInt(8.W))
      val b = Input(UInt(33.W))
    })

    val probes =
      if (duplicate) {
        Seq(
          DpiApi.ilaProbe("value", io.a),
          DpiApi.ilaProbe("value", io.b),
        )
      } else {
        Seq(
          DpiApi.ilaProbe("small", io.a),
          DpiApi.ilaProbe("wide", io.b),
        )
      }

    DpiApi.ila(
      clock,
      reset.asBool,
      enabled = true,
      source = source,
      probes = probes,
    )
  }

  "DpiApi.ila" should "elaborate one atomic packed sampler" in {
    val sv = ChiselStage.emitSystemVerilog(new Harness("test_source"))
    assert(sv.contains("IlaProbeDPI"))
    assert(sv.contains("PACKED_WIDTH"))
  }

  it should "reject invalid and duplicate identifiers" in {
    assertThrows[IllegalArgumentException](
      ChiselStage.emitSystemVerilog(new Harness("bad.source")),
    )
    assertThrows[IllegalArgumentException](
      ChiselStage.emitSystemVerilog(new Harness("good", duplicate = true)),
    )
  }
}