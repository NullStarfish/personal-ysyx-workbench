package mycpu.utils
import chisel3._
import chisel3.util._

object QueueProbe {
  // 监控 Decoupled 接口 (Queue 的 enq 或 deq)
  def apply[T <: Data](port: DecoupledIO[T], name: String): Unit = {
    // 只有当握手成功 (Fire) 时才打印
    when (port.valid && port.ready) {
      // %x 打印十六进制，%d 打印十进制
      printf(s"[QueueProbe] $name FIRE: Data=0x%x\n", port.bits.asUInt)
    }
    // 可以在这里加更多的调试，比如检测堵塞时间等
  }
}