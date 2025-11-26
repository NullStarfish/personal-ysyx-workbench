package mycpu.core.bundles

import chisel3._
import chisel3.util._
import mycpu.common._

// [新增] 定义调试信息特质 (Trait)
// 所有混入此特质的 Bundle 都会自动拥有 pc 和 inst 字段
trait HasDebugInfo extends Bundle {
  val pc   = XLenU
  val inst = UInt(32.W)
  val dnpc = XLenU // [新增] 下一条指令的 PC

  // 辅助方法：用于快速连接调试信号
  def connectDebug(src: HasDebugInfo): Unit = {
    this.pc   := src.pc
    this.inst := src.inst
    this.dnpc := src.dnpc // [新增]
  }
}

// IF -> ID
// 使用 'with' 关键字混入特质
class FetchPacket extends Bundle with HasDebugInfo {
  val isException = Bool() 
}

// ID -> EX
class DecodePacket extends Bundle with HasDebugInfo {
  val rs1Data  = XLenU
  val rs2Data  = XLenU
  val imm      = XLenU
  val rdAddr   = UInt(5.W)
  val rs1Addr  = UInt(5.W)
  val ctrl     = new ControlSignals()
  val csrAddr  = UInt(12.W)
}

// EX -> MEM
class ExecutePacket extends Bundle with HasDebugInfo {
  val aluResult = XLenU     
  val memWData  = XLenU     
  val pcTarget  = XLenU     
  val rdAddr    = UInt(5.W)
  val ctrl      = new ControlSignals()
  val redirect  = Valid(UInt(XLEN.W))    


}

// MEM -> WB
class MemoryPacket extends Bundle with HasDebugInfo {
  val wbData   = XLenU
  val rdAddr   = UInt(5.W)
  val regWen   = Bool()
  val pcTarget = XLenU 
}




// WB 阶段反馈接口 (保持不变)
class WriteBackIO extends Bundle {
  val wen  = Bool()
  val addr = UInt(5.W)
  val data = XLenU
}

class RedirectIO extends Bundle {
  val valid  = Bool()
  val target = XLenU
}