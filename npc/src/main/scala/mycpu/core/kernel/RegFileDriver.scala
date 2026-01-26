package mycpu.core.kernel

import chisel3._
import chisel3.util._
import mycpu.core.os._
import mycpu.utils._

class RegFileDriver(regs: Vec[UInt]) extends ResourceHandle {
  override val name = "RF"

  override def read(addr: UInt): UInt = ContextScope.current match {
    case LogicCtx(_) => 
      // 情况 1: Logic 中，直接连线
      regs(addr)

    case ThreadCtx(t) => 
      // 情况 2: Thread 顶级，生成一个新步骤读到 Latch
      val latch = Reg(UInt(32.W))
      t.Step("RF_Read_Step") { latch := regs(addr) }
      latch

    case AtomicCtx(t) => 
      // 情况 3: Step 内部 (Atomic)，直接返回 Wire 供当前 Step 使用
      regs(addr)
  }

  override def write(addr: UInt, data: UInt): UInt = {
    ContextScope.current match {
        case LogicCtx(_)  => when(addr =/= 0.U) { regs(addr) := data }
        case ThreadCtx(t) => t.Step("RF_Write_Step") { when(addr =/= 0.U) { regs(addr) := data } }
        case AtomicCtx(t) => when(addr =/= 0.U) { regs(addr) := data }
    }
    0.U
  }
}