package mycpu.peripherals

import chisel3._
import chisel3.util._
import mycpu.utils._

// -----------------------------------------------------------------------------
// SRAM BlackBox Definition
// 对应 SystemVerilog 的端口定义，包含完整 AXI4 信号
// -----------------------------------------------------------------------------
class SRAM extends BlackBox {
  // 假设 ID 宽度为 4 (足够覆盖大多数 Lite/Full 混用场景)
  // 如果你的全局 ID 宽度不同，请调整这里的宽
  val idWidth = 4 

  val io = IO(new Bundle {
    val clk = Input(Clock())
    val rst = Input(Bool())
    
    // --- Write Address Channel (AW) ---
    val sram_axi_if_awid    = Input(UInt(idWidth.W))
    val sram_axi_if_awaddr  = Input(UInt(32.W))
    val sram_axi_if_awlen   = Input(UInt(8.W))
    val sram_axi_if_awsize  = Input(UInt(3.W))
    val sram_axi_if_awburst = Input(UInt(2.W))
    val sram_axi_if_awlock  = Input(Bool())
    val sram_axi_if_awcache = Input(UInt(4.W))
    val sram_axi_if_awprot  = Input(UInt(3.W))
    val sram_axi_if_awvalid = Input(Bool())
    val sram_axi_if_awready = Output(Bool())
    
    // --- Write Data Channel (W) ---
    val sram_axi_if_wdata   = Input(UInt(32.W))
    val sram_axi_if_wstrb   = Input(UInt(4.W))
    val sram_axi_if_wlast   = Input(Bool())
    val sram_axi_if_wvalid  = Input(Bool())
    val sram_axi_if_wready  = Output(Bool())
    
    // --- Write Response Channel (B) ---
    val sram_axi_if_bid     = Output(UInt(idWidth.W))
    val sram_axi_if_bresp   = Output(UInt(2.W))
    val sram_axi_if_bvalid  = Output(Bool())
    val sram_axi_if_bready  = Input(Bool())
    
    // --- Read Address Channel (AR) ---
    val sram_axi_if_arid    = Input(UInt(idWidth.W))
    val sram_axi_if_araddr  = Input(UInt(32.W))
    val sram_axi_if_arlen   = Input(UInt(8.W))
    val sram_axi_if_arsize  = Input(UInt(3.W))
    val sram_axi_if_arburst = Input(UInt(2.W))
    val sram_axi_if_arlock  = Input(Bool())
    val sram_axi_if_arcache = Input(UInt(4.W))
    val sram_axi_if_arprot  = Input(UInt(3.W))
    val sram_axi_if_arvalid = Input(Bool())
    val sram_axi_if_arready = Output(Bool())
    
    // --- Read Data Channel (R) ---
    val sram_axi_if_rid     = Output(UInt(idWidth.W))
    val sram_axi_if_rdata   = Output(UInt(32.W))
    val sram_axi_if_rresp   = Output(UInt(2.W))
    val sram_axi_if_rlast   = Output(Bool())
    val sram_axi_if_rvalid  = Output(Bool())
    val sram_axi_if_rready  = Input(Bool())
    
    // --- Fire Signals (Helper from Chisel) ---
    val sram_axi_if_aw_fire = Input(Bool())
    val sram_axi_if_w_fire  = Input(Bool())
    val sram_axi_if_b_fire  = Input(Bool())
    val sram_axi_if_ar_fire = Input(Bool())
    val sram_axi_if_r_fire  = Input(Bool())
  })
}

// -----------------------------------------------------------------------------
// SRAM Wrapper
// 连接 AXI4Bundle 到 BlackBox
// -----------------------------------------------------------------------------
class SRAMAXIWrapper extends Module {
  val io = IO(new Bundle {
    // 这里的 AXI4LiteBundle 继承自 AXI4Bundle，所以拥有全套信号
    // 假设你的 AXI4Defs 中定义 idWidth=4，或者在这里强制转换
    val bus = Flipped(new AXI4LiteBundle(32, 32)) 
  })

  val ram = Module(new SRAM)
  
  ram.io.clk := clock
  ram.io.rst := reset.asBool

  // --- AW Channel ---
  ram.io.sram_axi_if_awid    := io.bus.aw.bits.id
  ram.io.sram_axi_if_awaddr  := io.bus.aw.bits.addr
  ram.io.sram_axi_if_awlen   := io.bus.aw.bits.len
  ram.io.sram_axi_if_awsize  := io.bus.aw.bits.size
  ram.io.sram_axi_if_awburst := io.bus.aw.bits.burst
  ram.io.sram_axi_if_awlock  := io.bus.aw.bits.lock
  ram.io.sram_axi_if_awcache := io.bus.aw.bits.cache
  ram.io.sram_axi_if_awprot  := io.bus.aw.bits.prot
  ram.io.sram_axi_if_awvalid := io.bus.aw.valid
  io.bus.aw.ready            := ram.io.sram_axi_if_awready
  
  // --- W Channel ---
  ram.io.sram_axi_if_wdata   := io.bus.w.bits.data
  ram.io.sram_axi_if_wstrb   := io.bus.w.bits.strb
  ram.io.sram_axi_if_wlast   := io.bus.w.bits.last
  ram.io.sram_axi_if_wvalid  := io.bus.w.valid
  io.bus.w.ready             := ram.io.sram_axi_if_wready

  // --- B Channel ---
  io.bus.b.bits.id           := ram.io.sram_axi_if_bid
  io.bus.b.bits.resp         := ram.io.sram_axi_if_bresp
  io.bus.b.valid             := ram.io.sram_axi_if_bvalid
  ram.io.sram_axi_if_bready  := io.bus.b.ready

  // --- AR Channel ---
  ram.io.sram_axi_if_arid    := io.bus.ar.bits.id
  ram.io.sram_axi_if_araddr  := io.bus.ar.bits.addr
  ram.io.sram_axi_if_arlen   := io.bus.ar.bits.len
  ram.io.sram_axi_if_arsize  := io.bus.ar.bits.size
  ram.io.sram_axi_if_arburst := io.bus.ar.bits.burst
  ram.io.sram_axi_if_arlock  := io.bus.ar.bits.lock
  ram.io.sram_axi_if_arcache := io.bus.ar.bits.cache
  ram.io.sram_axi_if_arprot  := io.bus.ar.bits.prot
  ram.io.sram_axi_if_arvalid := io.bus.ar.valid
  io.bus.ar.ready            := ram.io.sram_axi_if_arready

  // --- R Channel ---
  io.bus.r.bits.id           := ram.io.sram_axi_if_rid
  io.bus.r.bits.data         := ram.io.sram_axi_if_rdata
  io.bus.r.bits.resp         := ram.io.sram_axi_if_rresp
  io.bus.r.bits.last         := ram.io.sram_axi_if_rlast
  io.bus.r.valid             := ram.io.sram_axi_if_rvalid
  ram.io.sram_axi_if_rready  := io.bus.r.ready

  // --- Fire Signals ---
  ram.io.sram_axi_if_aw_fire := io.bus.aw.fire
  ram.io.sram_axi_if_w_fire  := io.bus.w.fire
  ram.io.sram_axi_if_b_fire  := io.bus.b.fire
  ram.io.sram_axi_if_ar_fire := io.bus.ar.fire
  ram.io.sram_axi_if_r_fire  := io.bus.r.fire
}