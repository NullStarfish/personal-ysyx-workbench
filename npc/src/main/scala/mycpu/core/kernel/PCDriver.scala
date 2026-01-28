package mycpu.core.kernel

import chisel3._
import chisel3.util.experimental.BoringUtils
import mycpu.core.os._
import mycpu.utils._

class PCDriver(pcReg: UInt) extends ResourceHandle {
  override val name = "PC"


  override def read(addr: UInt, size: UInt, signed: Bool): UInt = {
    ContextScope.current match {
      case LogicCtx(_)  => pcReg
      case AtomicCtx(_) => pcReg
      case ThreadCtx(t) => 
        val latch = Reg(UInt(32.W))
        t.Step("PC_Read") { latch := pcReg }
        latch
    }
  }

  override def write(addr: UInt, data: UInt, size: UInt): UInt = {
    ContextScope.current match {
      case AtomicCtx(_) => 
        pcReg := data
        
      case ThreadCtx(t) => 
        t.Step("PC_Write") { 
           pcReg := data
        }
        
      case LogicCtx(_) =>
        pcReg := data
    }
    0.U
  }
  
}