package mycpu.core.kernel


import chisel3._
import chisel3.util._
import chisel3.util.experimental.BoringUtils

// 访问位宽
object AccessSize {
  val Byte = 0.U(2.W)
  val Half = 1.U(2.W)
  val Word = 2.U(2.W)
}

// IOCTL 命令
object IoctlCmd {
  val AXI_SET_LEN   = 0x10.U
  val AXI_SET_BURST = 0x11.U
  val CSR_RW        = 0x20.U // 交换
  val CSR_RS        = 0x21.U // 置位
  val CSR_RC        = 0x22.U // 清除
}

// 驱动内部使用的对齐计算器
object DriverUtils {
  def alignWrite(addr: UInt, data: UInt, size: UInt): (UInt, UInt) = {
    val offset = addr(1, 0)
    val shiftedData = data << (offset << 3)
    val mask = MuxLookup(size, "b1111".U)(Seq(
      AccessSize.Byte -> "b0001".U,
      AccessSize.Half -> "b0011".U,
      AccessSize.Word -> "b1111".U
    ))
    (shiftedData, mask << offset)
  }

  def alignRead(addr: UInt, rawData: UInt, size: UInt, signed: Bool): UInt = {
    val offset = addr(1, 0)
    val shifted = rawData >> (offset << 3)
    MuxLookup(size, shifted)(Seq(
      AccessSize.Byte -> Mux(signed, shifted(7, 0).asSInt.asUInt, shifted(7, 0)),
      AccessSize.Half -> Mux(signed, shifted(15, 0).asSInt.asUInt, shifted(15, 0)),
      AccessSize.Word -> shifted
    ))
  }

  def driveRemote[T <: Data](localSource: T, remoteSink: T): Unit = {
    BoringUtils.bore(localSource, Seq(remoteSink))
  }

  // 读取远程信号：Remote(Parent) -> Local(Child)
  def readRemote[T <: Data](remoteSource: T, localSink: T): Unit = {
    BoringUtils.bore(remoteSource, Seq(localSink))
  }
}