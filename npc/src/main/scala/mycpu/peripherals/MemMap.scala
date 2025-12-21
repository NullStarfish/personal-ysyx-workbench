package mycpu

import chisel3._
import chisel3.util._

case class DeviceConfig(name: String, startAddr: BigInt, endAddr: BigInt, isDifftestSkip: Boolean = false) {
  def size: BigInt = endAddr - startAddr
}

object MemMap {
  // 定义区域


  // 设备列表
    val devices = List(
        DeviceConfig("CLINT", 0x02000000L, 0x0200ffffL, true),
        DeviceConfig("SRAM", 0x0f000000L, 0x0fffffffL, false),
        DeviceConfig("UART16550", 0x10000000L, 0x10000fffL,  true),
        DeviceConfig("SPI master", 0x10001000L, 0x10001fffL, true),
        DeviceConfig("GPIO", 0x10002000L, 0x1000200fL, true),
        DeviceConfig("PS2", 0x10011000L, 0x10011007L, true),
        DeviceConfig("MROM", 0x20000000L, 0x20000fffL, false),
        DeviceConfig("VGA", 0x21000000L, 0x211fffffL, true),
        DeviceConfig("Flash", 0x30000000L, 0x3fffffffL, false),
        DeviceConfig("ChipLink MMIO", 0x40000000L, 0x7fffffffL, true),
        // 以下这些地址在 32位 Int 中是负数，必须加 L
        DeviceConfig("PSRAM", 0x80000000L, 0x9fffffffL, true),
        DeviceConfig("SDRAM", 0xa0000000L, 0xbfffffffL, true),
        DeviceConfig("ChipLink MEM", 0xc0000000L, 0xffffffffL, true)
    )

    def isDifftestSkip(addr: UInt): Bool = {
        // 使用 MuxCase 或逻辑或运算
        val hitSignals = devices.map { d =>
            (addr >= d.startAddr.U && addr <= d.endAddr.U) && d.isDifftestSkip.B
        }
        // 只要有一个命中了且需要 skip，就返回 true
        VecInit(hitSignals).asUInt.orR
    }

  
  // 辅助函数：通过名字获取索引，方便连线
  def getIndex(name: String): Int = devices.indexWhere(_.name == name)
}