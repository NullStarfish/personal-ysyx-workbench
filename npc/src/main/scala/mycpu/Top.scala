package mycpu

import chisel3._
import mycpu.core.Core
import mycpu.peripherals.{SRAM, Serial, Xbar} // 引入新增的模块
import mycpu.utils._
import circt.stage.ChiselStage 

class Top extends Module {
    val io = IO(new Bundle {})

    // 1. 实例化模块
    val core    = Module(new Core)
    val memArbiter = Module(new SimpleAXIArbiter)
    val xbar    = Module(new Xbar(MemMap.devices)) // 实例化 Xbar
    val sram    = Module(new SRAM)                 // BlackBox
    val serial  = Module(new Serial)               // Chisel Module

    // 2. Core -> Arbiter
    memArbiter.io.left <> core.io.imem
    memArbiter.io.right <> core.io.dmem

    // 3. Arbiter -> Xbar
    xbar.io.in <> memArbiter.io.out

    // 4. Xbar -> Peripherals
    // 获取设备索引
    val sramIdx   = MemMap.devices.indexWhere(_.name == "SRAM")
    val serialIdx = MemMap.devices.indexWhere(_.name == "SERIAL")
    
    // ==============================================================================
    // 连接 SRAM (BlackBox, 需要手动连线)
    // ==============================================================================
    val sramPort = xbar.io.slaves(sramIdx)

    sram.io.clk := clock
    sram.io.rst := reset.asBool

    // AW Channel
    sram.io.sram_axi_if_awaddr  := sramPort.aw.bits.addr
    sram.io.sram_axi_if_awvalid := sramPort.aw.valid
    sramPort.aw.ready           := sram.io.sram_axi_if_awready

    // W Channel
    sram.io.sram_axi_if_wdata   := sramPort.w.bits.data
    sram.io.sram_axi_if_wstrb   := sramPort.w.bits.strb
    sram.io.sram_axi_if_wvalid  := sramPort.w.valid
    sramPort.w.ready            := sram.io.sram_axi_if_wready

    // B Channel
    sramPort.b.bits.resp        := sram.io.sram_axi_if_bresp
    sramPort.b.valid            := sram.io.sram_axi_if_bvalid
    sram.io.sram_axi_if_bready  := sramPort.b.ready

    // AR Channel
    sram.io.sram_axi_if_araddr  := sramPort.ar.bits.addr
    sram.io.sram_axi_if_arvalid := sramPort.ar.valid
    sramPort.ar.ready           := sram.io.sram_axi_if_arready

    // R Channel
    sramPort.r.bits.data        := sram.io.sram_axi_if_rdata
    sramPort.r.bits.resp        := sram.io.sram_axi_if_rresp
    sramPort.r.valid            := sram.io.sram_axi_if_rvalid
    sram.io.sram_axi_if_rready  := sramPort.r.ready

    // Fire Signals (SRAM BlackBox 需要的辅助信号)
    sram.io.sram_axi_if_aw_fire := sramPort.aw.fire
    sram.io.sram_axi_if_w_fire  := sramPort.w.fire
    sram.io.sram_axi_if_b_fire  := sramPort.b.fire
    sram.io.sram_axi_if_ar_fire := sramPort.ar.fire
    sram.io.sram_axi_if_r_fire  := sramPort.r.fire

    // ==============================================================================
    // 连接 Serial (Chisel Module, 需要地址截断)
    // ==============================================================================
    val serialPort = xbar.io.slaves(serialIdx)
    val serialBus  = serial.io.bus

    // AR Channel (地址截断)
    serialBus.ar.valid     := serialPort.ar.valid
    serialBus.ar.bits.prot := serialPort.ar.bits.prot
    serialBus.ar.bits.addr := serialPort.ar.bits.addr(serial.localAddrWidth - 1, 0)
    serialPort.ar.ready    := serialBus.ar.ready

    // AW Channel (地址截断)
    serialBus.aw.valid     := serialPort.aw.valid
    serialBus.aw.bits.prot := serialPort.aw.bits.prot
    serialBus.aw.bits.addr := serialPort.aw.bits.addr(serial.localAddrWidth - 1, 0)
    serialPort.aw.ready    := serialBus.aw.ready

    // W, B, R Channels (数据位宽一致，直接连接)
    serialBus.w   <> serialPort.w
    serialPort.b  <> serialBus.b
    serialPort.r  <> serialBus.r
}

object Main extends App {
  ChiselStage.emitSystemVerilogFile(
    new Top,
    args = Array("--target-dir", "src/main/verilog/gen"),
    firtoolOpts = Array(
      "--disable-all-randomization"
    )
  )
}