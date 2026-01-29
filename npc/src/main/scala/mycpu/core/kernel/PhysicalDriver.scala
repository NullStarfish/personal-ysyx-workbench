package mycpu.core.kernel

import chisel3._
import chisel3.util._
import mycpu.core.os._
import mycpu.utils._

/**
 * 物理驱动 (Physical Driver)
 * 定义了硬件操作的原子接口。
 */
abstract class PhysicalDriver(val meta: DriverMeta) {
  
  // 1. 组合逻辑接口
  def combRead(addr: UInt, size: UInt): UInt = 0.U
  
  // 2. 顺序逻辑接口 (Kernel 管理线程调用)
  // 返回 (Data, Error, Done)
  def seqRead(addr: UInt, size: UInt): (UInt, UInt, Bool) = (0.U, 0.U, true.B)
  
  // 返回 (Error, Done)
  def seqWrite(addr: UInt, data: UInt, size: UInt): (UInt, Bool) = (0.U, true.B)
  
  // 硬件 Setup
  def setup(agent: HardwareAgent): Unit = {}
}

/**
 * [API 重构] 驱动工具集
 * 提供通用的数据对齐和选通信号生成逻辑，供各类总线驱动使用。
 */
object DriverUtils {
  /**
   * 根据地址低位和访问大小生成写选通信号 (WSTRB / Mask)
   * @param addr 访问地址 (32-bit)
   * @param size 访问大小 (0=Byte, 1=Half, 2=Word)
   * @return 4-bit Strobe
   */
  def genWStrobe(addr: UInt, size: UInt): UInt = {
    // 1. 根据 size 生成基础 Mask (低位对齐)
    // size=0(Byte)->0001, size=1(Half)->0011, size=2(Word)->1111
    val baseMask = MuxLookup(size, 0.U)(Seq(
      0.U -> "b0001".U(4.W),
      1.U -> "b0011".U(4.W),
      2.U -> "b1111".U(4.W)
    ))
    
    // 2. 根据地址低位 (offset) 左移 Mask
    // 比如地址 0x1, Byte => mask 0001 左移 1 位 => 0010
    baseMask << addr(1, 0)
  }

  /**
   * 根据地址低位对写数据进行通道对齐 (Data Alignment)
   * RISC-V 寄存器数据通常是右对齐的 (LSB)，但总线要求数据出现在对应的字节通道上。
   * @param data 原始写数据 (32-bit, 右对齐)
   * @param addr 访问地址
   * @return 移位后的数据
   */
  def alignWData(data: UInt, addr: UInt): UInt = {
    val offsetBits = addr(1, 0) ## 0.U(3.W) // addr[1:0] * 8
    data << offsetBits
  }
}