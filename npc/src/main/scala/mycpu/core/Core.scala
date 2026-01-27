package mycpu.core

import chisel3._
import mycpu.common._
import mycpu.utils._
import mycpu.core.os._
import mycpu.core.kernel._
import mycpu.core.processes._
import mycpu.core.bundles.FetchPacket
import firtoolresolver.shaded.coursier.Fetch


class Core extends Module {
  val io = IO(new Bundle {
    val master = new AXI4LiteBundle()
  })
  io.master.setAsMasterInit()

  // === 1. 物理资源实例化 (Bare Metal) ===
  val pcReg  = RegInit(START_ADDR.U(32.W))
  val rfVec  = RegInit(VecInit(Seq.fill(32)(0.U(32.W))))
  val csrVec = RegInit(VecInit(Seq.fill(4096)(0.U(32.W))))

  val imemAxi = Wire(new AXI4Bundle(AXI_ID_WIDTH, 32, 32))
  val dmemAxi = Wire(new AXI4Bundle(AXI_ID_WIDTH, 32, 32))



  // 物理 AXI 仲裁器
  val arbiter = Module(new SimpleAXIArbiter)
  arbiter.io.left  <> imemAxi
  arbiter.io.right <> dmemAxi
  io.master <> arbiter.io.out

  // === 2. 挂载驱动服务 (Mounting Services) ===
  // 这里的驱动实现了带阻塞控制的统一 API
  Kernel.mount("PC",   () => new PCDriver(pcReg))
  Kernel.mount("RF",   () => new RegFileDriver(rfVec))
  Kernel.mount("EX",   () => new ExecuteService())
  Kernel.mount("CSR",  () => new CSRDriver(csrVec))
  Kernel.mount("IMEM", () => new SmartAXIDriver(imemAxi))
  Kernel.mount("DMEM", () => new SmartAXIDriver(dmemAxi))

  // === 3. 启动进程流水线 (Booting Pipeline) ===
  // Fetch (取指进程) -> Main (执行进程)
  // 它们之间通过 HwQueue 自动同步

  implicit val fetchPktGen: FetchPacket = new FetchPacket
  implicit val uintGen: UInt = UInt(32.W)

  // 为管道整体加括号，然后使用点号调用 exit()
  (Kernel.boot(new FetchProcess) | new MainProcess).exit()
}