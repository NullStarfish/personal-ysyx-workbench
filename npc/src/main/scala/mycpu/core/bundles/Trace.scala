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
    val gotReply = Input(Bool())
    val gotInst = Input(Bool())
    val flush = Input(Bool())
    val pc = Input(UInt(XLEN.W))
    val inst = Input(UInt(32.W))
  })
  setInline(
    "FetchTrace.sv",
    """module FetchTrace(
      |    input logic   clk,
      |    input logic reset,
      |    input logic reqInst,
      |    input logic gotReply,
      |    input logic gotInst,
      |    input logic flush,
      |    input logic [31:0] pc,
      |    input logic [31:0] inst
      |);
      | import "DPI-C" function void fetch_trace(
      |   input gotInst,
      |   input int pc,
      |   input int inst,
      |   input int memLatency,
      |   input int waitLatency
      |);
      |
      |logic [31:0] memLatency;
      |logic inflight;
      |logic [31:0] memQ [0:7];
      |logic [31:0] waitQ [0:7];
      |logic [2:0] head;
      |logic [2:0] tail;
      |logic [3:0] count;
      |integer i;
      |
      |always_ff @(posedge clk) begin
      | if(reset || flush) begin
      |   memLatency <= 32'd0;
      |   inflight <= 1'b0;
      |   head <= 3'd0;
      |   tail <= 3'd0;
      |   count <= 4'd0;
      |   for(i = 0; i < 8; i = i + 1) begin
      |     memQ[i] <= 32'd0;
      |     waitQ[i] <= 32'd0;
      |   end
      | end else begin
      |   if(inflight) begin
      |     memLatency <= memLatency + 32'd1;
      |   end
      |
      |   for(i = 0; i < 8; i = i + 1) begin
      |     if(i < count) begin
      |       waitQ[(head + i) & 3'h7] <= waitQ[(head + i) & 3'h7] + 32'd1;
      |     end
      |   end
      |
      |   if(reqInst) begin
      |     memLatency <= 32'd0;
      |     inflight <= 1'b1;
      |   end
      |
      |   if(gotReply && gotInst) begin
      |     inflight <= 1'b0;
      |     if(count != 4'd0) begin
      |       fetch_trace(gotInst, pc, inst, memQ[head], waitQ[head]);
      |       head <= head + 3'd1;
      |       memQ[tail] <= memLatency;
      |       waitQ[tail] <= 32'd0;
      |       tail <= tail + 3'd1;
      |     end else begin
      |       fetch_trace(gotInst, pc, inst, 32'd0, 32'd0);
      |       memQ[tail] <= memLatency;
      |       waitQ[tail] <= 32'd0;
      |       tail <= tail + 3'd1;
      |       count <= 4'd1;
      |     end
      |   end else if(gotReply) begin
      |     inflight <= 1'b0;
      |     if(count < 4'd8) begin
      |       memQ[tail] <= memLatency;
      |       waitQ[tail] <= 32'd0;
      |       tail <= tail + 3'd1;
      |       count <= count + 4'd1;
      |     end
      |   end else if(gotInst) begin
      |     if(count != 4'd0) begin
      |       fetch_trace(gotInst, pc, inst, memQ[head], waitQ[head]);
      |       head <= head + 3'd1;
      |       count <= count - 4'd1;
      |     end else begin
      |       fetch_trace(gotInst, pc, inst, 32'd0, 32'd0);
      |     end
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
    val blocked = Input(Bool())
  })
  setInline(
    "LSUTrace.sv",
    """module LSUTrace(
      |    input logic clk,
      |    input logic reset,
      |    input logic reqReadData,
      |    input logic reqWriteData,
      |    input logic gotData,
      |    input logic blocked
      |);
      | import "DPI-C" function void lsu_trace(
      |   input int latency,
      |   input bit write
      |);
      | import "DPI-C" function void lsu_backpressure_trace(
      |   input blocked
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
      |   if(blocked) begin
      |     lsu_backpressure_trace(blocked);
      |   end
      |
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

final class PipelineTrace extends BlackBox with HasBlackBoxInline {
  val io = IO(new Bundle {
    val clk = Input(Clock())
    val reset = Input(Bool())
    val ifIdValid = Input(Bool())
    val idExValid = Input(Bool())
    val exMemValid = Input(Bool())
    val memWbValid = Input(Bool())
  })

  setInline(
    "PipelineTrace.sv",
    """module PipelineTrace(
      |  input logic clk,
      |  input logic reset,
      |  input logic ifIdValid,
      |  input logic idExValid,
      |  input logic exMemValid,
      |  input logic memWbValid
      |);
      | import "DPI-C" function void pipeline_trace(
      |   input ifIdValid,
      |   input idExValid,
      |   input exMemValid,
      |   input memWbValid
      |);
      |
      | always_ff @(posedge clk) begin
      |   if(!reset) begin
      |     pipeline_trace(
      |       ifIdValid,
      |       idExValid,
      |       exMemValid,
      |       memWbValid
      |     );
      |   end
      | end
      |endmodule
      |""".stripMargin,
  )
}

final class HazardTrace extends BlackBox with HasBlackBoxInline {
  val io = IO(new Bundle {
    val clk = Input(Clock())
    val reset = Input(Bool())
    val loadUseStall = Input(Bool())
    val redirectFlush = Input(Bool())
  })

  setInline(
    "HazardTrace.sv",
    """module HazardTrace(
      |  input logic clk,
      |  input logic reset,
      |  input logic loadUseStall,
      |  input logic redirectFlush
      |);
      | import "DPI-C" function void hazard_trace(
      |   input loadUseStall,
      |   input redirectFlush
      |);
      |
      | always_ff @(posedge clk) begin
      |   if(!reset) begin
      |     hazard_trace(loadUseStall, redirectFlush);
      |   end
      | end
      |endmodule
      |""".stripMargin,
  )
}
