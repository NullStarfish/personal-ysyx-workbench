package mycpu

import chisel3._
import chisel3.util._

// 定义外设配置结构
case class DeviceConfig(
    name: String, 
    startAddr: BigInt, 
    size: BigInt
) {
  // 辅助方法：计算结束地址
  def endAddr: BigInt = startAddr + size
}

object MemMap {
  // 定义基地址 (BigInt类型，方便计算)
  val DEVICE_BASE_ADDR = BigInt("a0000000", 16)

  // 定义所有外设的映射列表
  val devices = List(
    DeviceConfig("SERIAL", DEVICE_BASE_ADDR + 0x3f8, 0x100), // 串口
    DeviceConfig("RTC",    DEVICE_BASE_ADDR + 0x048, 0x008), // RTC
    DeviceConfig("GPIO",   DEVICE_BASE_ADDR + 0x1000, 0x100) // 示例：GPIO
  )
  
  // 可以在这里加一个校验函数，确保地址没有重叠
}