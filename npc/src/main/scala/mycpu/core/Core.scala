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

  kernel.mount(new SmartAXIDriver(io.master))
  
  val pcReg  = RegInit(START_ADDR.U(32.W))
  val rfVec  = RegInit(VecInit(Seq.fill(32)(0.U(32.W))))
  val csrVec = RegInit(VecInit(Seq.fill(4096)(0.U(32.W))))
  
  kernel.mount(new PCDriver(pcReg))
  kernel.mount(new mycpu.core.drivers.RegFileDriver(rfVec))
  kernel.mount(new CSRDriver(csrVec))
  
  kernel.mount(new PipeDriver("Fetch2Main", depth = 4))
  kernel.mount(new TerminalDriver("term"))

  object Init extends HwProcess("Init")(None, kernel) {
    val pipeOut = sys_open("Fetch2Main")
    val pipeIn  = sys_open("Fetch2Main")
    
    override def entry(): Unit = {
      spawn((p, k) => new MainProcess(p, k))(in = pipeIn).build()
      spawn((p, k) => new FetchProcess(p, k))(out = pipeOut).build()
    }
  }
  
  class AliasDriver(val aliasName: String, target: PhysicalDriver) extends PhysicalDriver(target.meta) {
    override val meta = target.meta.copy(name = aliasName)
    override def combRead(a: UInt, s: UInt) = target.combRead(a, s)
    override def seqRead(a: UInt, s: UInt) = target.seqRead(a, s)
    override def seqWrite(a: UInt, d: UInt, s: UInt) = target.seqWrite(a, d, s)
    override def setup(a: HardwareAgent) = target.setup(a)
  }
  
  val axiDrv = new SmartAXIDriver(io.master)
  kernel.mount(new AliasDriver("IMEM", axiDrv))
  kernel.mount(new AliasDriver("DMEM", axiDrv))

  Init.build()
  kernel.boot()

  if (true) {
    val commit_valid = WireInit(false.B); BoringUtils.addSink(commit_valid, "DPI_Commit_Valid")
    val commit_pc    = WireInit(0.U(32.W)); BoringUtils.addSink(commit_pc,    "DPI_Commit_PC")
    val commit_inst  = WireInit(0.U(32.W)); BoringUtils.addSink(commit_inst,  "DPI_Commit_Inst")
    
    val rf_flat = Cat(rfVec.reverse)
    val simState = Module(new InlineSimState)
    simState.io.clk := clock; simState.io.reset := reset; simState.io.valid := commit_valid
    simState.io.pc := commit_pc; simState.io.inst := commit_inst; simState.io.regs := rf_flat
    simState.io.dnpc := 0.U; simState.io.mstatus := 0.U; simState.io.mtvec := 0.U; simState.io.mepc := 0.U; simState.io.mcause := 0.U

    // [修复] 实例化 Ebreak DPI
    // 当指令为 EBREAK (0x00100073) 且提交有效时触发
    val is_ebreak_inst = (commit_inst === "h00100073".U)
    val simEbreak = Module(new InlineSimEbreak)
    simEbreak.io.valid := commit_valid && is_ebreak_inst
    simEbreak.io.is_ebreak := commit_inst
  }
}