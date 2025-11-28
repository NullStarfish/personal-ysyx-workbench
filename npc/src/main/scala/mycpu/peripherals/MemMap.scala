package mycpu

import chisel3._
import chisel3.util._

case class DeviceConfig(name: String, startAddr: BigInt, size: BigInt) {
  def endAddr: BigInt = startAddr + size
}

object MemMap {
  // 定义区域
  val SRAM_BASE   = BigInt("80000000", 16) // 常见的 MIPS/RISC-V 物理起始地址
  val SRAM_SIZE   = BigInt("04000000", 16) // 64MB

  val MMIO_BASE   = BigInt("a0000000", 16)

  // 设备列表
  val devices = List(
    DeviceConfig("SRAM",   SRAM_BASE,             SRAM_SIZE), // 0
    DeviceConfig("SERIAL", MMIO_BASE + 0x3f8,     0x8),       // 1
    // 你可以继续添加 RTC, GPIO 等
  )
  
  // 辅助函数：通过名字获取索引，方便连线
  def getIndex(name: String): Int = devices.indexWhere(_.name == name)
}