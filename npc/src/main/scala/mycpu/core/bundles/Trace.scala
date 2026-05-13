package mycpu.core.bundles
import chisel3._
import mycpu.common._
import chisel3.util.HasBlackBoxInline

class TraceBase extends Bundle {
}

object TraceVal {
  def apply[T <: Data] (gen: T) = if (ENABLE_TRACE_FIELDS) Some(gen) else None
} 


class RetireTrace extends TraceBase {
  val pc = XLenU
  val inst = UInt(32.W)
  val dnpc = XLenU
  val regWrite = new RegWriteMeta 
  val instType =  InstType()
}



trait withRetireTrace {
  val retireTrace = if (ENABLE_TRACE_FIELDS) Some(new RetireTrace) else None 
}


final class FetchTrace extends BlackBox with HasBlackBoxInline {
  val io  = IO(new Bundle {
    val clk = Input(Clock())
    val reset = Input(Bool())
    val gotInst = Input(Bool())
  })
  setInline(
    "FetchTrace.sv",
    """module FetchTrace(
      |    input logic   clk,
      |    input logic reset,
      |    input logic gotInst
      |);
      | import "DPI-C" function void fetch_trace(
      |   input gotInst
      |);
      |
      |always_ff @(posedge clk) begin
      | if(!reset && gotInst) begin
      |   fetch_trace(gotInst);
      | end
      |end
      |
      |endmodule
      |
      |
      |
      |""".stripMargin
  )
}

final class ExecuteTrace extends BlackBox with HasBlackBoxInline {
  val io  = IO(new Bundle {
    val clk = Input(Clock())
    val reset = Input(Bool())
    val finished = Input(Bool())
  })
  setInline(
    "ExecuteTrace.sv",
    """module ExecuteTrace(
      |    input logic clk,
      |    input logic reset,
      |    input logic finished
      |);
      | import "DPI-C" function void execute_trace(
      |   input finished
      |);
      |
      |always_ff @(posedge clk) begin
      | if(!reset && finished) begin
      |   execute_trace(finished);
      | end
      |end
      |
      |endmodule
      |""".stripMargin
  )
}

final class LSUTrace extends BlackBox with HasBlackBoxInline {
  val io  = IO(new Bundle {
    val clk = Input(Clock())
    val reset = Input(Bool())
    val gotData = Input(Bool())
  })
  setInline(
    "LSUTrace.sv",
    """module LSUTrace(
      |    input logic clk,
      |    input logic reset,
      |    input logic gotData
      |);
      | import "DPI-C" function void lsu_trace(
      |   input gotData
      |);
      |
      |always_ff @(posedge clk) begin
      | if(!reset && gotData) begin
      |   lsu_trace(gotData);
      | end
      |end
      |
      |endmodule
      |""".stripMargin
  )
}
