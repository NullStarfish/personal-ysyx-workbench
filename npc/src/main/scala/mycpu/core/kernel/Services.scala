package mycpu.core.kernel

import chisel3._
import mycpu.common._
import mycpu.utils._
import mycpu.core.os._
import chisel3.util.experimental.BoringUtils

// --- 1. 计算服务 (ALU 资源) ---
class ExecuteService extends ResourceHandle {
  override val name = "EX"
  private val alu = Module(new mycpu.core.components.ALU)

  // 统一写接口：对于 ALU，write = 触发计算
  alu.io.a := 0.U
  alu.io.b := 0.U
  alu.io.op := ALUOp.NOP
  override def write(addr: UInt, data: UInt, mode: UInt): UInt = {
    alu.io.a  := addr // rs1
    alu.io.b  := data // rs2 or imm
    alu.io.op := mode.asTypeOf(ALUOp())
    alu.io.out        // 立即返回组合逻辑结果
  }

  override def read(addr: UInt, mode: UInt, signed: Bool): UInt = unsupported("read")
  override def ioctl(cmd: UInt, arg: UInt): UInt = 0.U
}

// --- 2. 访存服务 (Memory 资源) ---
class MemoryService(axi_global: AXI4Bundle) extends ResourceHandle {
  val axi = BoringUtils.bore(axi_global)
  override val name = "MEM"
  private val driver = new SmartAXIDriver(axi)

  override def setup(agent: HardwareAgent): Unit = driver.setup(agent)

  // 统一读：Load 指令
  override def read(addr: UInt, mode: UInt, signed: Bool): UInt = {
    // 内部驱动处理 AXI 握手并自动挂起 Thread
    driver.read(addr, mode, signed) 
  }

  // 统一写：Store 指令
  override def write(addr: UInt, data: UInt, mode: UInt): UInt = {
    driver.write(addr, data, mode)
    0.U
  }
}