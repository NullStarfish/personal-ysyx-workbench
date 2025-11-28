package mycpu.peripherals

import chisel3._
import chisel3.util._
import mycpu.utils._

class SRAMAXIWrapper extends Module {
  val io = IO(new Bundle {
    val bus = Flipped(new AXI4LiteBundle(32, 32)) // 标准 AXI 接口
  })

  // 实例化 BlackBox
  val ram = Module(new SRAM)
  
  ram.io.clk := clock
  ram.io.rst := reset.asBool

  // --- AW Channel ---
  ram.io.sram_axi_if_awaddr  := io.bus.aw.bits.addr
  ram.io.sram_axi_if_awvalid := io.bus.aw.valid
  io.bus.aw.ready            := ram.io.sram_axi_if_awready
  
  // --- W Channel ---
  ram.io.sram_axi_if_wdata   := io.bus.w.bits.data
  ram.io.sram_axi_if_wstrb   := io.bus.w.bits.strb
  ram.io.sram_axi_if_wvalid  := io.bus.w.valid
  io.bus.w.ready             := ram.io.sram_axi_if_wready

  // --- B Channel ---
  io.bus.b.bits.resp         := ram.io.sram_axi_if_bresp
  io.bus.b.valid             := ram.io.sram_axi_if_bvalid
  ram.io.sram_axi_if_bready  := io.bus.b.ready

  // --- AR Channel ---
  ram.io.sram_axi_if_araddr  := io.bus.ar.bits.addr
  ram.io.sram_axi_if_arvalid := io.bus.ar.valid
  io.bus.ar.ready            := ram.io.sram_axi_if_arready

  // --- R Channel ---
  io.bus.r.bits.data         := ram.io.sram_axi_if_rdata
  io.bus.r.bits.resp         := ram.io.sram_axi_if_rresp
  io.bus.r.valid             := ram.io.sram_axi_if_rvalid
  ram.io.sram_axi_if_rready  := io.bus.r.ready

  // --- Fire 信号生成 (因为你的 BlackBox 需要这些辅助信号) ---
  ram.io.sram_axi_if_aw_fire := io.bus.aw.fire
  ram.io.sram_axi_if_w_fire  := io.bus.w.fire
  ram.io.sram_axi_if_b_fire  := io.bus.b.fire
  ram.io.sram_axi_if_ar_fire := io.bus.ar.fire
  ram.io.sram_axi_if_r_fire  := io.bus.r.fire
}