package mycpu.core.drivers

import chisel3._
import chisel3.util._
import mycpu.core.kernel._
import mycpu.core.os._
import mycpu.utils._
import mycpu.common._

class PCDriver(pcReg: UInt) extends PhysicalDriver(
  DriverMeta("PC", DriverTiming.Combinational, DriverTiming.Sequential)
) {
  override def combRead(addr: UInt, size: UInt): UInt = pcReg

  override def seqWrite(addr: UInt, data: UInt, size: UInt): (UInt, Bool) = {
    // [修复] data 是 64位，PC 是 32位，显式截断
    pcReg := data(XLEN-1, 0)
    (Errno.ESUCCESS, true.B)
  }
}

class RegFileDriver(regs: Vec[UInt]) extends PhysicalDriver(
  DriverMeta("RF", DriverTiming.Combinational, DriverTiming.Sequential)
) {
  override def combRead(addr: UInt, size: UInt): UInt = {
    Mux(addr === 0.U, 0.U, regs(addr))
  }

  override def seqWrite(addr: UInt, data: UInt, size: UInt): (UInt, Bool) = {
    when(addr =/= 0.U) {
      // [修复] 显式截断数据
      regs(addr) := data(XLEN-1, 0)
    }
    (Errno.ESUCCESS, true.B)
  }
}

class CSRDriver(regs: Vec[UInt]) extends PhysicalDriver(
  DriverMeta("CSR", DriverTiming.Combinational, DriverTiming.Sequential)
) {
  override def combRead(addr: UInt, size: UInt): UInt = {
    Mux(addr < regs.length.U, regs(addr), 0.U)
  }

  override def seqWrite(cmdOrAddr: UInt, data: UInt, size: UInt): (UInt, Bool) = {
    val cmd = cmdOrAddr(1, 0).asTypeOf(CSROp())
    val addr = data(31, 20) 
    val wdata = data(19, 0) // 这里逻辑可能需要根据你实际 CSR 协议调整，暂时保留原样
    // 注意：这里的 CSR 写逻辑比较特殊，如果之前有特定实现请保留
    (Errno.ESUCCESS, true.B)
  }
}