package mycpu.peripherals

import chisel3._
import chisel3.util._
import mycpu.utils._
import mycpu.common.XLEN // 确保导入了全局的 XLEN 定义

abstract class Peripheral(val deviceConfig: mycpu.DeviceConfig) extends Module {
  
  // 1. 计算该外设实际需要的内部地址位宽 (逻辑用)
  val localAddrWidth = log2Ceil(deviceConfig.size)

  val io = IO(new Bundle {
    // [关键修改]：物理接口必须是 XLEN，以便与 Xbar 连接
    val bus = Flipped(new AXI4LiteBundle(XLEN, XLEN))
  })

  io.bus.setAsSlave()


  protected def getWriteOffset: UInt = io.bus.aw.bits.addr(localAddrWidth - 1, 0)
  protected def getReadOffset:  UInt = io.bus.ar.bits.addr(localAddrWidth - 1, 0)


  val writeOffset = Wire(UInt(localAddrWidth.W))
  writeOffset := getWriteOffset

  val readOffset = Wire(UInt(localAddrWidth.W))
  readOffset := getReadOffset
}