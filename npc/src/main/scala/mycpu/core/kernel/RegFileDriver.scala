package mycpu.core.kernel

import chisel3._
import chisel3.util._
import mycpu.core.os._
import mycpu.utils._
import chisel3.util.experimental.BoringUtils

class RegFileDriver(regs_global: Vec[UInt]) extends ResourceHandle {
  val regs = BoringUtils.bore(regs_global)
  override val name = "RF"

  override def read(addr: UInt, _1: UInt, _2: Bool): UInt = ContextScope.current match {
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

  override def write(addr: UInt, data: UInt, mode: UInt): UInt = {
    // 核心修复 1: 显式添加 = 号和返回值类型 : UInt
    ContextScope.current match {
      case LogicCtx(_) => 
        when(addr =/= 0.U) { regs(addr) := data }
        0.U // 核心修复 2: 0.U 必须写在 when 块外面，作为 match 分支的返回值

      case ThreadCtx(t) => 
        t.Step("RF_Write_Step") { 
          when(addr =/= 0.U) { regs(addr) := data } 
        }
        0.U 

      case AtomicCtx(t) => 
        when(addr =/= 0.U) { regs(addr) := data }
        0.U 
    }
  }


}