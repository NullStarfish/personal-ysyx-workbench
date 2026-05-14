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
    val reqInst = Input(Bool())
    val gotInst = Input(Bool())
    val pc = Input(UInt(XLEN.W))
    val inst = Input(UInt(32.W))
  })
  setInline(
    "FetchTrace.sv",
    """module FetchTrace(
      |    input logic   clk,
      |    input logic reset,
      |    input logic reqInst,
      |    input logic gotInst,
      |    input logic [31:0] pc,
      |    input logic [31:0] inst
      |);
      | import "DPI-C" function void fetch_trace(
      |   input gotInst,
      |   input int pc,
      |   input int inst,
      |   input int latency
      |);
      |
      |logic [31:0] latency;
      |logic inflight;
      |
      |always_ff @(posedge clk) begin
      | if(reset) begin
      |   latency <= 32'd0;
      |   inflight <= 1'b0;
      | end else begin
      |   if(inflight) begin
      |     latency <= latency + 32'd1;
      |   end
      |
      |   if(reqInst) begin
      |     latency <= 32'd0;
      |     inflight <= 1'b1;
      |   end
      |
      |   if(gotInst) begin
      |     fetch_trace(gotInst, pc, inst, latency);
      |     inflight <= 1'b0;
      |     latency <= 32'd0;
      |   end
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
    val reqReadData = Input(Bool())
    val reqWriteData = Input(Bool())
    val gotData = Input(Bool())
  })
  setInline(
    "LSUTrace.sv",
    """module LSUTrace(
      |    input logic clk,
      |    input logic reset,
      |    input logic reqReadData,
      |    input logic reqWriteData,
      |    input logic gotData
      |);
      | import "DPI-C" function void lsu_trace(
      |   input int latency,
      |   input bit write
      |);
      |
      |logic [31:0] latency;
      |logic inflight;
      |logic inflightWrite;
      |
      |always_ff @(posedge clk) begin
      | if(reset) begin
      |   latency <= 32'd0;
      |   inflight <= 1'b0;
      |   inflightWrite <= 1'b0;
      | end else begin
      |   if(inflight) begin
      |     latency <= latency + 32'd1;
      |   end
      |
      |   if(reqReadData || reqWriteData) begin
      |     latency <= 32'd0;
      |     inflight <= 1'b1;
      |     inflightWrite <= reqWriteData;
      |   end
      |
      |   if(gotData) begin
      |     lsu_trace(latency, inflightWrite);
      |     inflight <= 1'b0;
      |     latency <= 32'd0;
      |     inflightWrite <= 1'b0;
      |   end
      | end
      |end
      |
      |endmodule
      |""".stripMargin
  )
}

final class FlushTrace extends BlackBox with HasBlackBoxInline {
  val io  = IO(new Bundle {
    val clk = Input(Clock())
    val reset = Input(Bool())
    val flush = Input(Bool())
    val pc = Input(UInt(XLEN.W))
    val inst = Input(UInt(32.W))
  })
  setInline(
    "FlushTrace.sv",
    """module FlushTrace(
      |    input logic clk,
      |    input logic reset,
      |    input logic flush,
      |    input logic [31:0] pc,
      |    input logic [31:0] inst
      |);
      | import "DPI-C" function void flush_trace(
      |   input flush,
      |   input int pc,
      |   input int inst
      |);
      |
      |always_ff @(posedge clk) begin
      | if(!reset && flush) begin
      |   flush_trace(flush, pc, inst);
      | end
      |end
      |
      |endmodule
      |""".stripMargin
  )
}
