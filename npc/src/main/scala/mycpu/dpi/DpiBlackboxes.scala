package mycpu.dpi

import chisel3._
import chisel3.util.HasBlackBoxInline

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
    val valid = Input(Bool())
    val is_ebreak = Input(UInt(32.W))
  })

  setInline(
    "SimEbreakDPI.sv",
    """module SimEbreakDPI(
      |    input logic        valid,
      |    input logic [31:0] is_ebreak
      |);
      |    import "DPI-C" function void ebreak();
      |
      |    always @(*) begin
      |        if (valid) begin
      |            ebreak();
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
    val regs_flat = Input(UInt(1024.W))
    val mtvec = Input(UInt(32.W))
    val mepc = Input(UInt(32.W))
    val mstatus = Input(UInt(32.W))
    val mcause = Input(UInt(32.W))
    val inst = Input(UInt(32.W))
  })

  setInline(
    "SimStateDPI.sv",
    """module SimStateDPI(
      |    input logic          clk,
      |    input logic          reset,
      |    input logic          valid,
      |    input logic [31:0]   pc,
      |    input logic [31:0]   dnpc,
      |    input logic [1023:0] regs_flat,
      |    input logic [31:0]   mtvec,
      |    input logic [31:0]   mepc,
      |    input logic [31:0]   mstatus,
      |    input logic [31:0]   mcause,
      |    input logic [31:0]   inst
      |);
      |    import "DPI-C" function void dpi_update_state(
      |        input int pc,
      |        input int dnpc,
      |        input bit [1023:0] gprs,
      |        input int mtvec,
      |        input int mepc,
      |        input int mstatus,
      |        input int mcause,
      |        input int inst
      |    );
      |
      |    always_ff @(posedge clk) begin
      |        if (!reset && valid) begin
      |            dpi_update_state(pc, dnpc, regs_flat, mtvec, mepc, mstatus, mcause, inst);
      |        end
      |    end
      |endmodule
      |""".stripMargin,
  )
}
