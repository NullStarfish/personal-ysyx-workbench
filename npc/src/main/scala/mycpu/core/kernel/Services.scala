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

  alu.io.a := 0.U
  alu.io.b := 0.U
  alu.io.op := ALUOp.NOP

  // 现在 mode 直接接收 ALUOp 枚举类型
  override def write(addr: UInt, data: UInt, mode: UInt): UInt = {
    alu.io.a  := addr // rs1Val
    alu.io.b  := data // rs2Val or imm
    // [修复] 不再使用 asTypeOf，因为在 Main 中传入的已经是匹配宽度的枚举
    // 实际上我们可以改变 write 的签名，但为了保持资源接口统一，我们在内部转换
    alu.io.op := mode.asTypeOf(ALUOp()) 
    alu.io.out
  }

  override def read(addr: UInt, mode: UInt, signed: Bool): UInt = unsupported("read")
  override def ioctl(cmd: UInt, arg: UInt): UInt = 0.U
}