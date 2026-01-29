package mycpu.core

import chisel3._
import chisel3.util._
import mycpu.common._
import mycpu.utils._
import mycpu.core.os._
import mycpu.core.kernel._
import mycpu.core.drivers._
import mycpu.core.processes._
import chisel3.util.experimental.BoringUtils

class Core extends Module {
  val io = IO(new Bundle {
    val master = new AXI4Bundle(AXI_ID_WIDTH, 32, 32)
  })

  implicit val kernel: Kernel = new Kernel()

  // 1. 挂载物理驱动
  val axiDrv = new SmartAXIDriver(io.master)
  kernel.mount(axiDrv)
  
  val pcReg  = RegInit(START_ADDR.U(32.W))
  val rfVec  = RegInit(VecInit(Seq.fill(32)(0.U(32.W))))
  val csrVec = RegInit(VecInit(Seq.fill(4096)(0.U(32.W))))
  
  kernel.mount(new PCDriver(pcReg))
  kernel.mount(new mycpu.core.drivers.RegFileDriver(rfVec))
  kernel.mount(new CSRDriver(csrVec))
  kernel.mount(new PipeDriver("Fetch2Main", depth = 2)) // 严格串行化，深度2足够
  kernel.mount(new PipeDriver("TokenPass", depth = 2))
  kernel.mount(new TerminalDriver("term"))

  // 3. 别名挂载 (方便 Process 查找)

  // 4. 初始化进程
  object Init extends HwProcess("Init")(None, kernel) {
    override def entry(): Unit = {
      // 启动 Fetch 和 Main
      // 不需要显式传递 handle，它们会在内部通过 sys_open 打开
      spawn((p, k) => new MainProcess(p, k))().build()
      spawn((p, k) => new FetchProcess(p, k))().build()
    }
  }
  
  Init.build()
  kernel.boot()

  // 5. DPI 验证探针
  if (true) {
    val commit_valid = WireInit(false.B); BoringUtils.addSink(commit_valid, "DPI_Commit_Valid")
    val commit_pc    = WireInit(0.U(32.W)); BoringUtils.addSink(commit_pc,    "DPI_Commit_PC")
    val commit_inst  = WireInit(0.U(32.W)); BoringUtils.addSink(commit_inst,  "DPI_Commit_Inst")
    
    val rf_flat = Cat(rfVec.reverse)
    val simState = Module(new InlineSimState)
    simState.io.clk := clock; simState.io.reset := reset; simState.io.valid := commit_valid
    simState.io.pc := commit_pc; simState.io.inst := commit_inst; simState.io.regs := rf_flat
    simState.io.dnpc := 0.U; simState.io.mstatus := 0.U; simState.io.mtvec := 0.U; simState.io.mepc := 0.U; simState.io.mcause := 0.U

    val is_ebreak_inst = (commit_inst === "h00100073".U)
    val simEbreak = Module(new InlineSimEbreak)
    simEbreak.io.valid := commit_valid && is_ebreak_inst
    simEbreak.io.is_ebreak := commit_inst
  }
}