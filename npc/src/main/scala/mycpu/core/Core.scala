package mycpu.core

import chisel3._
import chisel3.util._
import mycpu.common._
import mycpu.utils._
import mycpu.core.os._
import mycpu.core.kernel._
import mycpu.core.processes._
import mycpu.core.bundles.FetchPacket
import chisel3.util.experimental.BoringUtils

class Core extends Module {
  val io = IO(new Bundle {
    val master = new AXI4LiteBundle()
  })
  io.master.setAsMasterInit()

  // === 1. 物理资源实例化 ===
  val pcReg  = RegInit(START_ADDR.U(32.W))
  val pcNext = WireInit(pcReg) // 默认保持
  val pcWen  = WireInit(false.B)

  // 使用 BoringUtils 建立“插座”（Sink），等待 Main 进程插入
  BoringUtils.addSink(pcNext, "PC_Update_Data")
  BoringUtils.addSink(pcWen,  "PC_Update_En")



  val rfVec  = RegInit(VecInit(Seq.fill(32)(0.U(32.W))))
  // --- 新增：RegFile 写端口 ---
  val rfWen   = WireInit(false.B)
  val rfWaddr = WireInit(0.U(5.W))
  val rfWdata = WireInit(0.U(32.W))

  // 建立“插座” (Sink)
  BoringUtils.addSink(rfWen,   "RF_Update_En")
  BoringUtils.addSink(rfWaddr, "RF_Update_Addr")
  BoringUtils.addSink(rfWdata, "RF_Update_Data")



  val csrVec = RegInit(VecInit(Seq.fill(4096)(0.U(32.W))))

  val csrWen   = WireInit(false.B)
  val csrWaddr = WireInit(0.U(12.W)) // 假设 CSR 地址是 12位
  val csrWdata = WireInit(0.U(32.W))
  BoringUtils.addSink(csrWen,   "CSR_Update_En")
  BoringUtils.addSink(csrWaddr, "CSR_Update_Addr")
  BoringUtils.addSink(csrWdata, "CSR_Update_Data")



  val imemAxi = Wire(new AXI4Bundle(AXI_ID_WIDTH, 32, 32))
  val dmemAxi = Wire(new AXI4Bundle(AXI_ID_WIDTH, 32, 32))

  val arbiter = Module(new SimpleAXIArbiter)
  arbiter.io.left  <> imemAxi
  arbiter.io.right <> dmemAxi
  io.master <> arbiter.io.out

  // === 2. 挂载驱动服务 ===
  Kernel.mount("PC",   () => new PCDriver(pcReg))
  Kernel.mount("RF",   () => new RegFileDriver(rfVec))
  Kernel.mount("EX",   () => new ExecuteService())
  Kernel.mount("CSR",  () => new CSRDriver(csrVec))
  Kernel.mount("IMEM", () => new SmartAXIDriver(imemAxi))
  Kernel.mount("DMEM", () => new SmartAXIDriver(dmemAxi))

  // === 3. 启动进程流水线 ===
  implicit val fetchPktGen: FetchPacket = new FetchPacket
  implicit val uintGen: UInt = UInt(32.W)

  (Kernel.boot(new FetchProcess) | new MainProcess).exit()
  
  // ==================================================================
  // [新增] 仿真监控线程 (Sim Monitor Thread)
  // 直接在 Core 中监听，避免 SimHost 模块被优化的问题
  // ==================================================================
  if (true) { // 仅仅为了代码块折叠，也可以是 if (debugEnable)
    // 1. 定义接收端 (Sinks) - 来自 MainProcess
    val commit_valid = dontTouch(Wire(Bool()))
    val commit_pc    = dontTouch(Wire(UInt(32.W)))
    val commit_inst  = dontTouch(Wire(UInt(32.W)))
    val next_pc      = dontTouch(Wire(UInt(32.W)))
    val is_skip      = dontTouch(Wire(Bool()))
    val is_ebreak    = dontTouch(Wire(Bool()))

    // 设置默认值
    commit_valid := false.B
    commit_pc    := 0.U
    commit_inst  := 0.U
    next_pc      := 0.U
    is_skip      := false.B
    is_ebreak    := false.B

    // 绑定 BoringUtils Sink
    BoringUtils.addSink(commit_valid, "DPI_Commit_Valid")
    BoringUtils.addSink(commit_pc,    "DPI_Commit_PC")
    BoringUtils.addSink(commit_inst,  "DPI_Commit_Inst")
    BoringUtils.addSink(next_pc,      "DPI_Next_PC")
    BoringUtils.addSink(is_skip,      "DPI_Difftest_Skip")
    BoringUtils.addSink(is_ebreak,    "DPI_Sim_Ebreak")

    // 2. 准备数据 - 直接读取 Core 中的物理寄存器，无需 BoringUtils
    val rf_flat = Cat(rfVec.reverse) // 32个寄存器展平
    val mstatus = csrVec(0x300)
    val mtvec   = csrVec(0x305)
    val mepc    = csrVec(0x341)
    val mcause  = csrVec(0x342)

    // 3. 实例化 DPI BlackBoxes
    val simState = Module(new InlineSimState)
    simState.io.clk     := clock
    simState.io.reset   := reset
    simState.io.valid   := commit_valid
    simState.io.pc      := commit_pc
    simState.io.dnpc    := next_pc
    simState.io.inst    := commit_inst
    simState.io.regs    := rf_flat
    simState.io.mstatus := mstatus
    simState.io.mtvec   := mtvec
    simState.io.mepc    := mepc
    simState.io.mcause  := mcause

    val simEbreak = Module(new InlineSimEbreak)
    simEbreak.io.valid     := commit_valid && is_ebreak
    simEbreak.io.is_ebreak := commit_inst

    val simSkip = Module(new InlineDifftestSkip)
    simSkip.io.clock := clock
    simSkip.io.skip  := commit_valid && is_skip
  }
}