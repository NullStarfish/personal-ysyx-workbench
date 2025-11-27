package mycpu.peripherals

import chisel3._
import chisel3.util._
import mycpu.utils._ // 假设这里定义了 AXI4LiteBundle



// Peripheral 不再直接接收 addrWidth 决定 IO，而是接收 size 决定内部逻辑
// deviceConfig 用于内部逻辑（如地址掩码）
abstract class Peripheral(val deviceConfig: mycpu.DeviceConfig) extends Module {
  val io = IO(new Bundle {
    // 所有的外设都使用统一的 32位 宽度的总线接口，方便 Xbar 连接
    val bus = Flipped(new AXI4LiteBundle())
  })

  io.bus.setAsSlave()

  // 辅助函数：获取当前请求相对于外设基地址的偏移量 (Offset)
  // 比如基地址 0xA0000000，请求 0xA0000004，Offset 就是 0x04
  def getOffset(addr: UInt): UInt = {
    // 方法1：直接截取低位 (最常用，硬件开销最小)
    // 需要计算 size 对应多少个 bit，例如 size=0x100 -> maskBits=8
    val maskBits = log2Ceil(deviceConfig.size) 
    addr(maskBits - 1, 0)
    
    // 方法2：减法 (硬件开销大，不推荐)
    // addr - deviceConfig.startAddr.U
  }

  // 计算内部所需的地址位数
  val localAddrWidth = log2Ceil(deviceConfig.size)
  
  // 提取写地址和读地址的 Offset
  // 很多简单的外设不区分读写地址逻辑，或者 latch 住地址
  val writeOffset = io.bus.aw.bits.addr(localAddrWidth - 1, 0)
  val readOffset  = io.bus.ar.bits.addr(localAddrWidth - 1, 0)
}