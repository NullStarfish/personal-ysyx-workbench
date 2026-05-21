package mycpu.core.bundles
import chisel3._
import mycpu.common._
import chisel3.util._

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
  val csrs = new CsrDebugBundle
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
    val reqBlocked = Input(Bool())
    val outBlocked = Input(Bool())
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
      |    input logic reqBlocked,
      |    input logic outBlocked,
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
      | import "DPI-C" function void fetch_unit_trace(
      |   input reqBlocked,
      |   input outBlocked,
      |   input flush
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
      | if(reset) begin
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
      |   if(reqBlocked || outBlocked || flush) begin
      |     fetch_unit_trace(reqBlocked, outBlocked, flush);
      |   end
      |
      |   if(flush) begin
      |     memLatency <= 32'd0;
      |     inflight <= 1'b0;
      |     head <= 3'd0;
      |     tail <= 3'd0;
      |     count <= 4'd0;
      |     for(i = 0; i < 8; i = i + 1) begin
      |       memQ[i] <= 32'd0;
      |       waitQ[i] <= 32'd0;
      |     end
      |   end else begin
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
      | import "DPI-C" function void dcache_trace(
      |   input bit hit,
      |   input bit miss,
      |   input int latency
      |);
      | import "DPI-C" function void lsu_backpressure_trace(
      |   input blocked
      |);
      |
      |logic [31:0] latency;
      |logic [31:0] doneLatency;
      |logic inflight;
      |logic inflightWrite;
      |
      |assign doneLatency = inflight ? latency + 32'd1 : 32'd1;
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
      |     lsu_trace(doneLatency, inflightWrite);
      |     dcache_trace(1'b0, 1'b1, doneLatency);
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

final class DCacheTrace extends BlackBox with HasBlackBoxInline {
  val io = IO(new Bundle {
    val clk = Input(Clock())
    val reset = Input(Bool())
    val hit = Input(Bool())
    val miss = Input(Bool())
    val latency = Input(UInt(32.W))
  })
  setInline(
    "DCacheTrace.sv",
    """module DCacheTrace(
      |    input logic clk,
      |    input logic reset,
      |    input logic hit,
      |    input logic miss,
      |    input logic [31:0] latency
      |);
      | import "DPI-C" function void dcache_trace(
      |   input bit hit,
      |   input bit miss,
      |   input int latency
      |);
      |
      |always_ff @(posedge clk) begin
      | if(!reset && (hit || miss)) begin
      |   dcache_trace(hit, miss, latency);
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
    val fetchOut = Flipped(Valid(new Bundle {
      val pc = UInt(32.W)
      val inst = UInt(32.W)
    }))
    val decodeOut = Flipped(Valid(new Bundle {
      val pc = UInt(32.W)
      val inst = UInt(32.W)
    }))
    val executeOut = Flipped(Valid(new Bundle {
      val pc = UInt(32.W)
      val inst = UInt(32.W)
    }))
    val lsuOut = Flipped(Valid(new Bundle {
      val pc = UInt(32.W)
      val inst = UInt(32.W)
    }))
    val retire = Flipped(Valid(new Bundle {
      val pc = UInt(32.W)
      val inst = UInt(32.W)
    }))
  })

  setInline(
    "PipelineTrace.sv",
    """module PipelineTrace(
      |  input logic clk,
      |  input logic reset,
      |  input logic fetchOut_valid,
      |  input logic [31:0] fetchOut_bits_pc,
      |  input logic [31:0] fetchOut_bits_inst,
      |  input logic decodeOut_valid,
      |  input logic [31:0] decodeOut_bits_pc,
      |  input logic [31:0] decodeOut_bits_inst,
      |  input logic executeOut_valid,
      |  input logic [31:0] executeOut_bits_pc,
      |  input logic [31:0] executeOut_bits_inst,
      |  input logic lsuOut_valid,
      |  input logic [31:0] lsuOut_bits_pc,
      |  input logic [31:0] lsuOut_bits_inst,
      |  input logic retire_valid,
      |  input logic [31:0] retire_bits_pc,
      |  input logic [31:0] retire_bits_inst
      |);
      | import "DPI-C" function void pipeline_trace(
      |   input fetchOut,
      |   input int fetchPc,
      |   input int fetchInst,
      |   input decodeOut,
      |   input int decodePc,
      |   input int decodeInst,
      |   input executeOut,
      |   input int executePc,
      |   input int executeInst,
      |   input lsuOut,
      |   input int lsuPc,
      |   input int lsuInst,
      |   input retire,
      |   input int retirePc,
      |   input int retireInst
      |);
      |
      | always_ff @(posedge clk) begin
      |   if(!reset) begin
      |     pipeline_trace(
      |       fetchOut_valid,
      |       fetchOut_bits_pc,
      |       fetchOut_bits_inst,
      |       decodeOut_valid,
      |       decodeOut_bits_pc,
      |       decodeOut_bits_inst,
      |       executeOut_valid,
      |       executeOut_bits_pc,
      |       executeOut_bits_inst,
      |       lsuOut_valid,
      |       lsuOut_bits_pc,
      |       lsuOut_bits_inst,
      |       retire_valid,
      |       retire_bits_pc,
      |       retire_bits_inst
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
