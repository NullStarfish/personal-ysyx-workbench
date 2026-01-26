package mycpu.core
import chisel3._
import mycpu.common._
import mycpu.utils._
import mycpu.core.os._
import mycpu.core.kernel._
import mycpu.core.processes._

class Core extends Module {
  val io = IO(new Bundle {
    val master = new AXI4LiteBundle()
  })

  // === 1. 物理硬件实例化 (The Real Hardware) ===
  val pcReg    = RegInit(START_ADDR.U(XLEN.W))
  val rfRegs   = RegInit(VecInit(Seq.fill(32)(0.U(XLEN.W))))
  val csrRegs  = RegInit(VecInit(Seq.fill(4096)(0.U(XLEN.W)))) // 简化版
  
  // AXI 内部连线
  val imemBus = Wire(new AXI4Bundle(AXI_ID_WIDTH, XLEN, XLEN))
  val dmemBus = Wire(new AXI4Bundle(AXI_ID_WIDTH, XLEN, XLEN))
  val arbiter = Module(new SimpleAXIArbiter)
  arbiter.io.left <> imemBus
  arbiter.io.right <> dmemBus
  io.master <> arbiter.io.out

  // === 2. 驱动挂载 (Driver Mounting) ===
  Kernel.mount("PC",   () => new PCDriver(pcReg))
  Kernel.mount("RF",   () => new RegFileDriver(rfRegs))
  Kernel.mount("CSR",  () => new CSRDriver(csrRegs))
  Kernel.mount("IMEM", () => new SmartAXIDriver(imemBus))
  Kernel.mount("DMEM", () => new SmartAXIDriver(dmemBus))

  // === 3. 启动进程链 (Process Pipeline) ===
  // 我们将流水线每一段定义为一个 Process
  // Kernel.boot 会创建一个起始输入信号
  Kernel.boot(new FetchProcess) | 
         new DecodeProcess      | 
         new ExecuteProcess     | 
         new LSUProcess         | 
         new WBProcess          exit()
}