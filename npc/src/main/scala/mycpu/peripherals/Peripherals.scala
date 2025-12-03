package mycpu.peripherals

import chisel3._
import chisel3.util._
import mycpu.utils._
import mycpu.common.XLEN

abstract class Peripheral(val deviceConfig: mycpu.DeviceConfig) extends Module {
  
  val localAddrWidth = log2Ceil(deviceConfig.size)

  val io = IO(new Bundle {
    // 物理接口是统一的 AXI4LiteBundle
    val bus = Flipped(new AXI4LiteBundle(XLEN, XLEN))
  })
  
  // 建议：不在基类中做 setAsSlave，也不做默认连接。
  // 因为子类会使用 AXI4Split 接管 io.bus。
}