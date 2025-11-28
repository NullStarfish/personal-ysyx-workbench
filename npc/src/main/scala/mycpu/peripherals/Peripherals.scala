package mycpu.peripherals

import chisel3._
import chisel3.util._
import mycpu.utils._

abstract class Peripheral(val deviceConfig: mycpu.DeviceConfig) extends Module {
  
  // 1. 预先计算好该外设需要的地址位宽 (例如 size=0x100 -> width=8)
  val localAddrWidth = log2Ceil(deviceConfig.size)

  val io = IO(new Bundle {
    val bus = Flipped(new AXI4LiteBundle(addrWidth = localAddrWidth))
  })

  io.bus.setAsSlave()

  // 辅助函数：获取当前请求相对于外设基地址的偏移量
  def getOffset(addr: UInt): UInt = {
    // 直接截取低位，只取 localAddrWidth 这么多位
    addr(localAddrWidth - 1, 0)
  }

  // 提取写地址和读地址的 Offset
  // 注意：io.bus.aw.bits.addr 的位宽已经是 localAddrWidth 了，
  // 但为了安全起见或逻辑复用，截取也是可以的，或者直接使用。
  val writeOffset = io.bus.aw.bits.addr
  val readOffset  = io.bus.ar.bits.addr
}