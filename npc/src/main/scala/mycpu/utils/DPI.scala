package mycpu.utils

import chisel3._
import chisel3.util._

// ==============================================================================
// Inline BlackBoxes: 直接嵌入 SystemVerilog DPI 接口
// ==============================================================================

class InlineSimState extends BlackBox with HasBlackBoxInline {
  val io = IO(new Bundle {
    val clk     = Input(Clock())
    val reset   = Input(Bool())
    val valid   = Input(Bool())
    val pc      = Input(UInt(32.W))
    val dnpc    = Input(UInt(32.W))
    val inst    = Input(UInt(32.W))
    val regs    = Input(UInt(1024.W))
    val mstatus = Input(UInt(32.W))
    val mtvec   = Input(UInt(32.W))
    val mepc    = Input(UInt(32.W))
    val mcause  = Input(UInt(32.W))
  })

  setInline("InlineSimState.sv",
    """module InlineSimState(
      |    input clk, input reset, input valid,
      |    input [31:0] pc, input [31:0] dnpc, input [31:0] inst,
      |    input [1023:0] regs,
      |    input [31:0] mstatus, input [31:0] mtvec, input [31:0] mepc, input [31:0] mcause
      |);
      |    import "DPI-C" function void dpi_update_state(
      |        input int pc, input int dnpc, input bit [1023:0] gprs,
      |        input int mtvec, input int mepc, input int mstatus, input int mcause, input int inst
      |    );
      |    always @(posedge clk) begin
      |        if (!reset && valid) dpi_update_state(pc, dnpc, regs, mtvec, mepc, mstatus, mcause, inst);
      |    end
      |endmodule
    """.stripMargin)
}

class InlineSimEbreak extends BlackBox with HasBlackBoxInline {
  val io = IO(new Bundle {
    val valid = Input(Bool())
    val is_ebreak = Input(UInt(32.W))
  })
  setInline("InlineSimEbreak.sv",
    """module InlineSimEbreak(input valid, input [31:0] is_ebreak);
      |    import "DPI-C" function void ebreak();
      |    always @(*) if (valid) ebreak();
      |endmodule
    """.stripMargin)
}

class InlineDifftestSkip extends BlackBox with HasBlackBoxInline {
  val io = IO(new Bundle {
    val clock = Input(Clock())
    val skip  = Input(Bool())
  })
  setInline("InlineDifftestSkip.sv",
    """module InlineDifftestSkip(input clock, input skip);
      |    import "DPI-C" function void difftest_skip_ref_cpp();
      |    always @(posedge clock) if (skip) difftest_skip_ref_cpp();
      |endmodule
    """.stripMargin)
}