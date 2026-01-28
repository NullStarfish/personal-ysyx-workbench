package mycpu.core.kernel

import chisel3._
import chisel3.util._
import mycpu.core.os._
import mycpu.utils._
import chisel3.util.experimental.BoringUtils

class RegFileDriver(regs: Vec[UInt]) extends ResourceHandle {
  override val name = "RF"

  // 1. 读取：Bore 整个 Vec 是合法的，因为这只是读取副本
  val readOnlyRegs = BoringUtils.bore(regs)
  
  // 2. 写入：定义本地 Source 信号
  private val remoteWen   = WireDefault(false.B)
  private val remoteAddr  = WireDefault(0.U(5.W))
  private val remoteData  = WireDefault(0.U(32.W))

  // 3. 建立“插头” (Source) - 连接到 Core 中的 Sink
  BoringUtils.addSource(remoteWen,   "RF_Update_En")
  BoringUtils.addSource(remoteAddr,  "RF_Update_Addr")
  BoringUtils.addSource(remoteData,  "RF_Update_Data")

  // --- 阻塞控制 (Busy Table) ---
  // 单发射 CPU 其实不需要这个，但为了完整性保留
  // private val busyTable = RegInit(0.U(32.W)) 

  override def read(addr: UInt, size: UInt, signed: Bool): UInt = {
    // 简单的读逻辑
    Mux(addr === 0.U, 0.U, readOnlyRegs(addr))
  }

  override def write(addr: UInt, data: UInt, size: UInt): UInt = {
    // 定义写动作
    def doWrite(): Unit = {
      when(addr =/= 0.U) {
        remoteWen  := true.B
        remoteAddr := addr
        remoteData := data
      }
    }

    // 根据上下文执行
    ContextScope.current match {
      case AtomicCtx(_) => doWrite()
      case ThreadCtx(t) => t.Step("RF_Write_Step") { doWrite() }
      case LogicCtx(_)  => doWrite()
    }
    
    0.U
  }

  override def ioctl(cmd: UInt, arg: UInt): UInt = 0.U
}