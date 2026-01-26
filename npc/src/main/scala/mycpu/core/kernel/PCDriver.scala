package mycpu.core.kernel
import chisel3._
import mycpu.core.os._
import mycpu.utils._
class PCDriver(pcReg: UInt) extends ResourceHandle {
  override val name = "PC"

  override def read(addr: UInt, size: UInt, signed: Bool): UInt = {
    ContextScope.current match {
      case LogicCtx(_)  => pcReg // 组合逻辑：直连
      case AtomicCtx(_) => pcReg // 原子步骤：直连
      case ThreadCtx(t) =>
        val latch = Reg(UInt(32.W))
        t.Step("PC_Read") { latch := pcReg }
        latch
    }
  }

  override def write(addr: UInt, data: UInt, size: UInt): UInt = {
    // 这里 pcReg := data 实际上是把 data 连到了传入的那个物理寄存器上
    ContextScope.current match {
      case ThreadCtx(t) => t.Step("PC_Write") { pcReg := data }
      case _ => pcReg := data
    }
    0.U
  }
}