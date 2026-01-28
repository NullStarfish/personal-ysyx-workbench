package mycpu.core.kernel

import chisel3._
import chisel3.util._
import mycpu.core.os._
import mycpu.utils._
import chisel3.util.experimental.BoringUtils

class RegFileDriver(regs: Vec[UInt]) extends ResourceHandle {
  override val name = "RF"




  override def read(addr: UInt, size: UInt, signed: Bool): UInt = {
    // 简单的读逻辑
    Mux(addr === 0.U, 0.U, regs(addr))
  }

  override def write(addr: UInt, data: UInt, size: UInt): UInt = {
    // 定义写动作
    def doWrite(): Unit = {
      when(addr =/= 0.U) {
        regs(addr) := data
      }
    }

    // 根据上下文执行
    ContextScope.current match {
      case AtomicCtx(_) => doWrite()
      case ThreadCtx(t) => t.Step("RF_Write_Step") { doWrite() }
      case LogicCtx(_)  => doWrite()
    }
    
    0.U
  }

  override def ioctl(cmd: UInt, arg: UInt): UInt = 0.U
}