package mycpu.memory

import chisel3._

class SRAM extends BlackBox {
  val io = IO(new Bundle {
    val clk = Input(Clock())
    val rst = Input(Bool())
    
    // AXI Slave Interface 对应 SRAM.sv 的端口
    val sram_axi_if_awaddr  = Input(UInt(32.W))
    val sram_axi_if_awvalid = Input(Bool())
    val sram_axi_if_awready = Output(Bool())
    
    val sram_axi_if_wdata   = Input(UInt(32.W))
    val sram_axi_if_wstrb   = Input(UInt(4.W))
    val sram_axi_if_wvalid  = Input(Bool())
    val sram_axi_if_wready  = Output(Bool())
    
    val sram_axi_if_bresp   = Output(UInt(2.W))
    val sram_axi_if_bvalid  = Output(Bool())
    val sram_axi_if_bready  = Input(Bool())
    
    val sram_axi_if_araddr  = Input(UInt(32.W))
    val sram_axi_if_arvalid = Input(Bool())
    val sram_axi_if_arready = Output(Bool())
    
    val sram_axi_if_rdata   = Output(UInt(32.W))
    val sram_axi_if_rresp   = Output(UInt(2.W))
    val sram_axi_if_rvalid  = Output(Bool())
    val sram_axi_if_rready  = Input(Bool())
    
    // Fire signals required by your SRAM.sv logic
    val sram_axi_if_aw_fire = Input(Bool())
    val sram_axi_if_w_fire  = Input(Bool())
    val sram_axi_if_b_fire  = Input(Bool())
    val sram_axi_if_ar_fire = Input(Bool())
    val sram_axi_if_r_fire  = Input(Bool())
  })
}