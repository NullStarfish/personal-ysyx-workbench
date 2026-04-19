package labcpu.mem

import chisel3._
import chisel3.util.HasBlackBoxInline
import mycpu.common._

class SRAM extends BlackBox with HasBlackBoxInline {
  val io = IO(new Bundle {
    val clock = Input(Clock())

    val imem_addr = Input(UInt(XLEN.W))
    val imem_rdata = Output(UInt(32.W))

    val dmem_addr = Input(UInt(XLEN.W))
    val dmem_ren = Input(Bool())
    val dmem_wen = Input(Bool())
    val dmem_subop = Input(UInt(3.W))
    val dmem_unsigned = Input(Bool())
    val dmem_wdata = Input(UInt(XLEN.W))
    val dmem_rdata = Output(UInt(XLEN.W))
  })

  setInline(
    "CourseSRAM.sv",
    """module SRAM(
      |  input  logic        clock,
      |  input  logic [31:0] imem_addr,
      |  output logic [31:0] imem_rdata,
      |  input  logic [31:0] dmem_addr,
      |  input  logic        dmem_ren,
      |  input  logic        dmem_wen,
      |  input  logic [2:0]  dmem_subop,
      |  input  logic        dmem_unsigned,
      |  input  logic [31:0] dmem_wdata,
      |  output logic [31:0] dmem_rdata
      |);
      |  import "DPI-C" function int course_sram_ifetch(input int addr);
      |  import "DPI-C" function int course_sram_read(input int addr, input byte subop, input byte unsigned_load);
      |  import "DPI-C" function void course_sram_write(input int addr, input int data, input byte subop);
      |
      |  always_comb begin
      |    imem_rdata = course_sram_ifetch(imem_addr);
      |    dmem_rdata = dmem_ren ? course_sram_read(dmem_addr, {5'b0, dmem_subop}, {7'b0, dmem_unsigned}) : 32'b0;
      |  end
      |
      |  always_ff @(posedge clock) begin
      |    if (dmem_wen) begin
      |      course_sram_write(dmem_addr, dmem_wdata, {5'b0, dmem_subop});
      |    end
      |  end
      |endmodule
      |""".stripMargin,
  )
}
