package mycpu.core.components

import chisel3._
import mycpu.common._

// === 资源接口包 (Resource Bundle) ===
// 进程拿着这个去申请资源
class RfReadPort extends Bundle {
  val addr = Output(UInt(5.W))
  val data = Input(UInt(XLEN.W))
}

class RfWritePort extends Bundle {
  val addr = Output(UInt(5.W))
  val data = Output(UInt(XLEN.W))
  val wen  = Output(Bool())
}

// === 内核态资源管理器 (Kernel Space Manager) ===
// 这是一个纯 Scala 类，维护物理存储，并提供连接服务
class RegFileManager {
  // 物理存储 (Core 内的全局变量)
  private val regs = RegInit(VecInit(Seq.fill(32)(0.U(XLEN.W))))

  // 提供 "读服务"
  // port: 申请者的 IO 端口
  def connectRead(port: Record): Unit = {
    val p = port.asInstanceOf[RfReadPort]
    p.data := Mux(p.addr === 0.U, 0.U, regs(p.addr))
  }

  // 提供 "写服务"
  def connectWrite(port: Record): Unit = {
    val p = port.asInstanceOf[RfWritePort]
    when(p.wen && p.addr =/= 0.U) {
      regs(p.addr) := p.data
    }
  }
}