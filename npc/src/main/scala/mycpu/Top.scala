package mycpu

import chisel3._
import mycpu.core.Core
import mycpu.peripherals.{SRAM}
import mycpu.utils._
// [新增] 引入 CIRCT 阶段生成器
import circt.stage.ChiselStage 

class Top extends Module {
  // ... (Top 的内容保持不变) ...
    val io = IO(new Bundle {})

    val core = Module(new Core)
    val arbiter = Module(new SimpleAXIArbiter)
    val sram = Module(new SRAM)

    // Core -> Arbiter
    arbiter.io.ifu <> core.io.imem
    arbiter.io.lsu <> core.io.dmem

    // Arbiter -> SRAM (BlackBox 连接)
    sram.io.clk := clock
    sram.io.rst := reset.asBool

    sram.io.sram_axi_if_awaddr  := arbiter.io.mem.aw.bits.addr
    sram.io.sram_axi_if_awvalid := arbiter.io.mem.aw.valid
    arbiter.io.mem.aw.ready     := sram.io.sram_axi_if_awready

    sram.io.sram_axi_if_wdata   := arbiter.io.mem.w.bits.data
    sram.io.sram_axi_if_wstrb   := arbiter.io.mem.w.bits.strb
    sram.io.sram_axi_if_wvalid  := arbiter.io.mem.w.valid
    arbiter.io.mem.w.ready      := sram.io.sram_axi_if_wready

    arbiter.io.mem.b.bits.resp  := sram.io.sram_axi_if_bresp
    arbiter.io.mem.b.valid      := sram.io.sram_axi_if_bvalid
    sram.io.sram_axi_if_bready  := arbiter.io.mem.b.ready

    sram.io.sram_axi_if_araddr  := arbiter.io.mem.ar.bits.addr
    sram.io.sram_axi_if_arvalid := arbiter.io.mem.ar.valid
    arbiter.io.mem.ar.ready     := sram.io.sram_axi_if_arready

    arbiter.io.mem.r.bits.data  := sram.io.sram_axi_if_rdata
    arbiter.io.mem.r.bits.resp  := sram.io.sram_axi_if_rresp
    arbiter.io.mem.r.valid      := sram.io.sram_axi_if_rvalid
    sram.io.sram_axi_if_rready  := arbiter.io.mem.r.ready

    // Fire 信号连接
    sram.io.sram_axi_if_aw_fire := arbiter.io.mem.aw.fire
    sram.io.sram_axi_if_w_fire  := arbiter.io.mem.w.fire
    sram.io.sram_axi_if_b_fire  := arbiter.io.mem.b.fire
    sram.io.sram_axi_if_ar_fire := arbiter.io.mem.ar.fire
    sram.io.sram_axi_if_r_fire  := arbiter.io.mem.r.fire
}

object Main extends App {
  // 使用 ChiselStage.emitSystemVerilogFile
  // 它可以接受两个参数数组：args (给 Chisel 的) 和 firtoolOpts (直接透传给 firtool 的)
  ChiselStage.emitSystemVerilogFile(
    new Top,
    args = Array("--target-dir", "src/main/verilog/gen"),
    firtoolOpts = Array(
      "--disable-all-randomization", // 可选：禁用随机初始化，让波形更干净
      //"--disable-layers=Verification" // [关键] 禁用验证层，防止生成额外文件
    )
  )
}