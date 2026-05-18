package ysyx

import chisel3._
import chisel3.util._

import freechips.rocketchip.amba.axi4._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.util._
import org.chipsalliance.cde.config.Parameters

class DpiClintMtime extends BlackBox with HasBlackBoxInline {
  val io = IO(new Bundle {
    val clock = Input(Clock())
    val mtime = Output(UInt(64.W))
  })

  setInline(
    "DpiClintMtime.sv",
    """module DpiClintMtime(
      |  input  logic        clock,
      |  output reg [63:0] mtime
      |);
      |  import "DPI-C" function void clint_mtime_read(output longint unsigned mtime);
      |
      |  always_ff @(posedge clock) begin
      |    clint_mtime_read(mtime);
      |  end
      |endmodule
      |""".stripMargin
  )
}

class AXI4CLINT(address: Seq[AddressSet])(implicit p: Parameters) extends LazyModule {
  val beatBytes = 4
  val node = AXI4SlaveNode(Seq(AXI4SlavePortParameters(
    Seq(AXI4SlaveParameters(
      address       = address,
      executable    = false,
      supportsWrite = TransferSizes.none,
      supportsRead  = TransferSizes(1, beatBytes),
      interleavedId = Some(0))
    ),
    beatBytes = beatBytes)))

  lazy val module = new Impl
  class Impl extends LazyModuleImp(this) {
    val (in, _) = node.in(0)

    val mtimeDpi = Module(new DpiClintMtime)
    mtimeDpi.io.clock := clock
    val mtime = mtimeDpi.io.mtime

    val sIdle :: sWaitRready :: Nil = Enum(2)
    val state = RegInit(sIdle)

    val rdata = Reg(UInt(32.W))
    val rid = Reg(chiselTypeOf(in.ar.bits.id))

    val offset = in.ar.bits.addr(15, 0)
    val readData = MuxLookup(offset, 0.U)(Seq(
      0x0.U -> mtime(31, 0),
      0x4.U -> mtime(63, 32)
    ))

    in.ar.ready := state === sIdle
    when(state === sIdle && in.ar.fire) {
      rdata := readData
      rid := in.ar.bits.id
      state := sWaitRready
    }.elsewhen(state === sWaitRready && in.r.fire) {
      state := sIdle
    }

    in.r.valid := state === sWaitRready
    in.r.bits.data := rdata
    in.r.bits.id := rid
    in.r.bits.resp := AXI4Parameters.RESP_OKAY
    in.r.bits.last := true.B

    in.aw.ready := false.B
    in.w.ready := false.B
    in.b.valid := false.B

    assert(!(in.ar.valid && in.ar.bits.len =/= 0.U), "CLINT only supports single-beat reads")
    assert(!(in.ar.valid && in.ar.bits.size > 2.U), "CLINT only supports reads up to 4 bytes")
    assert(!in.aw.valid, "CLINT mtime is read-only")
    assert(!in.w.valid, "CLINT mtime is read-only")
  }
}
