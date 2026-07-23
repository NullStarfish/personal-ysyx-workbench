package mycpu.dpi

import chisel3._
import chisel3.experimental.{IntParam, StringParam}
import chisel3.util.HasBlackBoxInline


final class IlaProbeDPI(source: String, schema: String, packedWidth: Int)
    extends BlackBox(Map(
      "SOURCE" -> StringParam(source),
      "SCHEMA" -> StringParam(schema),
      "PACKED_WIDTH" -> IntParam(packedWidth),
    ))
    with HasBlackBoxInline {
  val io = IO(new Bundle {
    val clock = Input(Clock())
    val reset = Input(Bool())
    val sampleData = Input(UInt(packedWidth.W))
  })

  setInline(
    "IlaProbeDPI.sv",
    """module IlaProbeDPI #(
      |  parameter string SOURCE = "",
      |  parameter string SCHEMA = "",
      |  parameter integer PACKED_WIDTH = 64
      |) (
      |  input logic clock,
      |  input logic reset,
      |  input logic [PACKED_WIDTH-1:0] sampleData
      |);
      |  localparam integer WORD_COUNT = PACKED_WIDTH / 32;
      |  bit [31:0] sampleWords [0:WORD_COUNT-1];
      |
      |  import "DPI-C" function int ila_source_allocate(
      |    input string source_name,
      |    input string schema,
      |    input int packed_width
      |  );
      |  import "DPI-C" function void ila_sample(
      |    input int id,
      |    input bit [31:0] sample_words[]
      |  );
      |
      |  genvar wordIndex;
      |  generate
      |    for (wordIndex = 0; wordIndex < WORD_COUNT; wordIndex = wordIndex + 1) begin : gen_sample_words
      |      assign sampleWords[wordIndex] = sampleData[wordIndex * 32 +: 32];
      |    end
      |  endgenerate
      |
      |  integer id;
      |  initial id = ila_source_allocate(SOURCE, SCHEMA, PACKED_WIDTH);
      |  always_ff @(posedge clock) begin
      |    if (!reset) ila_sample(id, sampleWords);
      |  end
      |endmodule
      |""".stripMargin,
  )
}

final class SimCounterPushDPI(tag: String, name: String)
    extends BlackBox(Map("TAG" -> StringParam(tag), "NAME" -> StringParam(name)))
    with HasBlackBoxInline {
  val io = IO(new Bundle {
    val clock = Input(Clock())
    val reset = Input(Bool())
    val valid = Input(Bool())
    val delta = Input(UInt(64.W))
  })

  setInline(
    "SimCounterPushDPI.sv",
    """module SimCounterPushDPI #(
      |    parameter string TAG = "",
      |    parameter string NAME = ""
      |) (
      |    input logic clock,
      |    input logic reset,
      |    input logic valid,
      |    input logic [63:0] delta
      |);
      |    import "DPI-C" function int sim_counter_alloc(input string tag, input string name);
      |    import "DPI-C" function void sim_counter_add(input int id, input longint unsigned delta);
      |
      |    integer id;
      |    initial id = sim_counter_alloc(TAG, NAME);
      |
      |    always_ff @(posedge clock) begin
      |        if (!reset && valid) begin
      |            sim_counter_add(id, delta);
      |        end
      |    end
      |endmodule
      |""".stripMargin,
  )
}

final class SimCounterReadDPI(tag: String, name: String)
    extends BlackBox(Map("TAG" -> StringParam(tag), "NAME" -> StringParam(name)))
    with HasBlackBoxInline {
  val io = IO(new Bundle {
    val clock = Input(Clock())
    val value = Output(UInt(64.W))
  })

  setInline(
    "SimCounterReadDPI.sv",
    """module SimCounterReadDPI #(
      |    parameter string TAG = "",
      |    parameter string NAME = ""
      |) (
      |    input logic clock,
      |    output logic [63:0] value
      |);
      |    import "DPI-C" function int sim_counter_alloc(input string tag, input string name);
      |    import "DPI-C" function longint unsigned sim_counter_read(input int id);
      |
      |    integer id;
      |    initial begin
      |        id = sim_counter_alloc(TAG, NAME);
      |        value = 64'd0;
      |    end
      |
      |    always_ff @(negedge clock) begin
      |        value <= sim_counter_read(id);
      |    end
      |endmodule
      |""".stripMargin,
  )
}

final class SimCounterRatioDPI(
    tag: String,
    name: String,
    numeratorTag: String,
    numeratorName: String,
    denominatorTag: String,
    denominatorName: String,
    percentage: Boolean,
) extends BlackBox(Map(
      "TAG" -> StringParam(tag),
      "NAME" -> StringParam(name),
      "NUMERATOR_TAG" -> StringParam(numeratorTag),
      "NUMERATOR_NAME" -> StringParam(numeratorName),
      "DENOMINATOR_TAG" -> StringParam(denominatorTag),
      "DENOMINATOR_NAME" -> StringParam(denominatorName),
      "PERCENTAGE" -> IntParam(if (percentage) 1 else 0),
    ))
    with HasBlackBoxInline {
  val io = IO(new Bundle {})

  setInline(
    "SimCounterRatioDPI.sv",
    """module SimCounterRatioDPI #(
      |    parameter string TAG = "",
      |    parameter string NAME = "",
      |    parameter string NUMERATOR_TAG = "",
      |    parameter string NUMERATOR_NAME = "",
      |    parameter string DENOMINATOR_TAG = "",
      |    parameter string DENOMINATOR_NAME = "",
      |    parameter integer PERCENTAGE = 0
      |) ();
      |    import "DPI-C" function void sim_counter_register_ratio(
      |        input string tag,
      |        input string name,
      |        input string numerator_tag,
      |        input string numerator_name,
      |        input string denominator_tag,
      |        input string denominator_name,
      |        input int percentage
      |    );
      |
      |    initial begin
      |        sim_counter_register_ratio(
      |            TAG,
      |            NAME,
      |            NUMERATOR_TAG,
      |            NUMERATOR_NAME,
      |            DENOMINATOR_TAG,
      |            DENOMINATOR_NAME,
      |            PERCENTAGE
      |        );
      |    end
      |endmodule
      |""".stripMargin,
  )
}

final class DifftestSkipDPI extends BlackBox with HasBlackBoxInline {
  val io = IO(new Bundle {
    val clock = Input(Clock())
    val skip = Input(Bool())
  })

  setInline(
    "DifftestSkipDPI.sv",
    """module DifftestSkipDPI(
      |    input  logic        clock,
      |    input  logic        skip
      |);
      |    import "DPI-C" function void difftest_skip_ref_cpp();
      |    always_ff @(posedge clock) begin
      |        if (skip) begin
      |            difftest_skip_ref_cpp();
      |        end
      |    end
      |endmodule
      |""".stripMargin,
  )
}

final class SimEbreakDPI extends BlackBox with HasBlackBoxInline {
  val io = IO(new Bundle {
    val clock = Input(Clock())
    val reset = Input(Bool())
    val valid = Input(Bool())
    val is_ebreak = Input(UInt(32.W))
  })

  setInline(
    "SimEbreakDPI.sv",
    """module SimEbreakDPI(
      |    input logic        clock,
      |    input logic        reset,
      |    input logic        valid,
      |    input logic [31:0] is_ebreak
      |);
      |    import "DPI-C" function void ebreak();
      |
      |    always_ff @(posedge clock) begin
      |        if (!reset && valid) begin
      |            ebreak();
      |        end
      |    end
      |endmodule
      |""".stripMargin,
  )
}

final class FlushDPI extends BlackBox with HasBlackBoxInline {
  val io = IO(new Bundle {
    val clock = Input(Clock())
    val reset = Input(Bool())
    val valid = Input(Bool())
  })

  setInline(
    "FlushDPI.sv",
    """module FlushDPI(
      |    input logic clock,
      |    input logic reset,
      |    input logic valid
      |);
      |    import "DPI-C" function void dpi_record_flush();
      |
      |    always_ff @(posedge clock) begin
      |        if (!reset && valid) begin
      |            dpi_record_flush();
      |        end
      |    end
      |endmodule
      |""".stripMargin,
  )
}

final class SimStateDPI extends BlackBox with HasBlackBoxInline {
  val io = IO(new Bundle {
    val clk = Input(Clock())
    val reset = Input(Bool())
    val valid = Input(Bool())
    val pc = Input(UInt(32.W))
    val dnpc = Input(UInt(32.W))
    val reg_wen = Input(Bool())
    val reg_addr = Input(UInt(5.W))
    val reg_data = Input(UInt(32.W))
    val regs_flat = Input(UInt(1024.W))
    val mtvec = Input(UInt(32.W))
    val mepc = Input(UInt(32.W))
    val mstatus = Input(UInt(32.W))
    val mcause = Input(UInt(32.W))
    val inst = Input(UInt(32.W))
    val instType = Input(UInt(2.W))
  })

  setInline(
    "SimStateDPI.sv",
    """module SimStateDPI(
      |    input logic          clk,
      |    input logic          reset,
      |    input logic          valid,
      |    input logic [31:0]   pc,
      |    input logic [31:0]   dnpc,
      |    input logic          reg_wen,
      |    input logic [4:0]    reg_addr,
      |    input logic [31:0]   reg_data,
      |    input logic [1023:0] regs_flat,
      |    input logic [31:0]   mtvec,
      |    input logic [31:0]   mepc,
      |    input logic [31:0]   mstatus,
      |    input logic [31:0]   mcause,
      |    input logic [31:0]   inst,
      |    input logic [2:0]    instType
      |);
      |    import "DPI-C" function void dpi_update_state(
      |        input int pc,
      |        input int dnpc,
      |        input int reg_wen,
      |        input int reg_addr,
      |        input int reg_data,
      |        input bit [1023:0] gprs,
      |        input int mtvec,
      |        input int mepc,
      |        input int mstatus,
      |        input int mcause,
      |        input int inst,
      |        input int instType
      |    );
      |
      |    always_ff @(posedge clk) begin
      |        if (!reset && valid) begin
      |            dpi_update_state(pc, dnpc, reg_wen, reg_addr, reg_data, regs_flat, mtvec, mepc, mstatus, mcause, inst, instType);
      |        end
      |    end
      |endmodule
      |""".stripMargin,
  )


}
