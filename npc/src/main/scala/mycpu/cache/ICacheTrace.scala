package mycpu.cache

import chisel3._
import chisel3.util.HasBlackBoxInline

final class ICacheAccessTrace extends BlackBox with HasBlackBoxInline {
  val io = IO(new Bundle {
    val clk = Input(Clock())
    val reset = Input(Bool())
    val hit = Input(Bool())
    val miss = Input(Bool())
    val latency = Input(UInt(32.W))
  })
  setInline(
    "ICacheAccessTrace.sv",
    """module ICacheAccessTrace(
      |    input logic clk,
      |    input logic reset,
      |    input logic hit,
      |    input logic miss,
      |    input logic [31:0] latency
      |);
      | import "DPI-C" function void icache_access_trace(input bit hit, input bit miss, input int latency);
      |
      |always_ff @(posedge clk) begin
      | if(!reset && (hit || miss)) begin
      |   icache_access_trace(hit, miss, latency);
      | end
      |end
      |
      |endmodule
      |""".stripMargin
  )
}
