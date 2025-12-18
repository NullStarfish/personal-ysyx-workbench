package mycpu

import chisel3._
import chisel3.util._

import mycpu.core.Core
import mycpu.utils._
import mycpu.utils.AXI4Parameters
import _root_.circt.stage.ChiselStage

// ==============================================================================
// 顶层 Wrapper：符合 YSYX 接口命名规范
// ==============================================================================
class myCore extends Module {
  // 强制设置模块名为 ysyx_23060000 (请将 23060000 替换为你的实际学号)
  override val desiredName = "myCore"

  val io = IO(new Bundle {
    val interrupt = Input(Bool())

    // ---------------------------------------------------------
    // AXI4 Master 接口 (命名严格对应表格)
    // ---------------------------------------------------------
    // AW (Write Address)
    val master_awready = Input(Bool())
    val master_awvalid = Output(Bool())
    val master_awaddr  = Output(UInt(32.W))
    val master_awid    = Output(UInt(4.W))
    val master_awlen   = Output(UInt(8.W))
    val master_awsize  = Output(UInt(3.W))
    val master_awburst = Output(UInt(2.W))
    
    // W (Write Data)
    val master_wready  = Input(Bool())
    val master_wvalid  = Output(Bool())
    val master_wdata   = Output(UInt(32.W))
    val master_wstrb   = Output(UInt(4.W))
    val master_wlast   = Output(Bool())
    
    // B (Write Response)
    val master_bready  = Output(Bool())
    val master_bvalid  = Input(Bool())
    val master_bresp   = Input(UInt(2.W))
    val master_bid     = Input(UInt(4.W))
    
    // AR (Read Address)
    val master_arready = Input(Bool())
    val master_arvalid = Output(Bool())
    val master_araddr  = Output(UInt(32.W))
    val master_arid    = Output(UInt(4.W))
    val master_arlen   = Output(UInt(8.W))
    val master_arsize  = Output(UInt(3.W))
    val master_arburst = Output(UInt(2.W))
    
    // R (Read Data)
    val master_rready  = Output(Bool())
    val master_rvalid  = Input(Bool())
    val master_rresp   = Input(UInt(2.W))
    val master_rdata   = Input(UInt(32.W))
    val master_rlast   = Input(Bool())
    val master_rid     = Input(UInt(4.W))

    // ---------------------------------------------------------
    // AXI4 Slave 接口 (占位，如果 Core 暂不支持，这里做 Termination)
    // ---------------------------------------------------------
    val slave_awready = Output(Bool())
    val slave_awvalid = Input(Bool())
    val slave_awaddr  = Input(UInt(32.W))
    val slave_awid    = Input(UInt(4.W))
    val slave_awlen   = Input(UInt(8.W))
    val slave_awsize  = Input(UInt(3.W))
    val slave_awburst = Input(UInt(2.W))
    
    val slave_wready  = Output(Bool())
    val slave_wvalid  = Input(Bool())
    val slave_wdata   = Input(UInt(32.W))
    val slave_wstrb   = Input(UInt(4.W))
    val slave_wlast   = Input(Bool())
    
    val slave_bready  = Input(Bool())
    val slave_bvalid  = Output(Bool())
    val slave_bresp   = Output(UInt(2.W))
    val slave_bid     = Output(UInt(4.W))
    
    val slave_arready = Output(Bool())
    val slave_arvalid = Input(Bool())
    val slave_araddr  = Input(UInt(32.W))
    val slave_arid    = Input(UInt(4.W))
    val slave_arlen   = Input(UInt(8.W))
    val slave_arsize  = Input(UInt(3.W))
    val slave_arburst = Input(UInt(2.W))
    
    val slave_rready  = Input(Bool())
    val slave_rvalid  = Output(Bool())
    val slave_rresp   = Output(UInt(2.W))
    val slave_rdata   = Output(UInt(32.W))
    val slave_rlast   = Output(Bool())
    val slave_rid     = Output(UInt(4.W))
  })

  // 实例化你的 Core
  val core = Module(new Core)

  core.clock := clock
  core.reset := reset
  // core.io.interrupt := io.interrupt // 如果未来支持中断请解开

  // ==============================================================================
  // 1. Master 接口连接
  // ==============================================================================
  
  // --- AW Channel (Output) ---
  io.master_awvalid := core.io.master.aw.valid
  core.io.master.aw.ready := io.master_awready

  io.master_awaddr  := core.io.master.aw.bits.addr
  io.master_awid    := core.io.master.aw.bits.id           // 固定 ID = 0
  io.master_awlen   := core.io.master.aw.bits.len           // 固定 1 Beat
  io.master_awsize  := core.io.master.aw.bits.size      // 固定 4 Bytes
  io.master_awburst := core.io.master.aw.bits.burst          // INCR


  // --- W Channel (Output) ---
  io.master_wvalid  := core.io.master.w.valid
  core.io.master.w.ready := io.master_wready

  io.master_wdata   := core.io.master.w.bits.data
  io.master_wstrb   := core.io.master.w.bits.strb
  io.master_wlast   := core.io.master.w.bits.last
  


  // --- B Channel (Input) ---
  // [修复] 必须驱动 core.io.master.b 的所有输入信号
  core.io.master.b.valid     := io.master_bvalid
  io.master_bready := core.io.master.b.ready
  core.io.master.b.bits.resp := io.master_bresp
  core.io.master.b.bits.id   := io.master_bid



  // --- AR Channel (Output) ---
  io.master_arvalid := core.io.master.ar.valid
  core.io.master.ar.ready := io.master_arready
  io.master_araddr  := core.io.master.ar.bits.addr
  io.master_arid    := core.io.master.ar.bits.id
  io.master_arlen   := core.io.master.ar.bits.len
  io.master_arsize  := core.io.master.ar.bits.size
  io.master_arburst := core.io.master.ar.bits.burst

  // --- R Channel (Input) ---
  core.io.master.r.valid     := io.master_rvalid
  io.master_rready := core.io.master.r.ready
  core.io.master.r.bits.data := io.master_rdata
  core.io.master.r.bits.resp := io.master_rresp
  core.io.master.r.bits.last := io.master_rlast
  core.io.master.r.bits.id   := io.master_rid



  // ==============================================================================
  // 2. Slave 接口 Termination (未使用但需驱动)
  // ==============================================================================
  io.slave_awready := false.B
  io.slave_wready  := false.B
  io.slave_bvalid  := false.B
  io.slave_bresp   := 0.U
  io.slave_bid     := 0.U
  
  io.slave_arready := false.B
  io.slave_rvalid  := false.B
  io.slave_rresp   := 0.U
  io.slave_rdata   := 0.U
  io.slave_rlast   := false.B
  io.slave_rid     := 0.U
  when(io.master_awready && io.master_awvalid) {
    Debug.log("[DEBUG] [CoreWrapper] [[[[[[[[[[[[[[[[[[[[[[CURRENT]]]]]]]]]]]]]]]]]]]]]]: wid: %x, waddr: %x, wsize: %x, wlen: %x\n", io.master_awid, io.master_awaddr, io.master_awsize, io.master_awlen)
  }
  when(io.master_wvalid && io.master_wready) {
    Debug.log("[DEBUG] [CoreWrapper] [[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[[CURRENT]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]]: wdata: %x, wstrb: %x, wlast: %x\n", io.master_wdata, io.master_wstrb, io.master_wlast)
  }
}

// ==============================================================================
// 3. 生成 Verilog 的 Object
// ==============================================================================
object GenCore extends App {
  ChiselStage.emitSystemVerilogFile(
    new myCore,
    args = Array("--target-dir", "src/main/verilog/Core"),
    firtoolOpts = Array(
      "--disable-all-randomization"
    )
  )
}