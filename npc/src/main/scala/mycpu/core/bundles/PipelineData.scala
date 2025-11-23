package mycpu.core.bundles

import chisel3._
import mycpu.common._

// IF -> ID
class FetchPacket extends Bundle {
  val pc   = XLenU
  val inst = UInt(32.W)
  val isException = Bool() 
}

// ID -> EX
class DecodePacket extends Bundle {
  val pc       = XLenU
  val rs1Data  = XLenU
  val rs2Data  = XLenU
  val imm      = XLenU
  val rdAddr   = UInt(5.W)
  // [新增] 传递 rs1Addr，用于 CSR 立即数指令 (zimm)
  val rs1Addr  = UInt(5.W) 
  val ctrl     = new ControlSignals()
  val csrAddr  = UInt(12.W)
}

// EX -> MEM
class ExecutePacket extends Bundle {
  val aluResult = XLenU     
  val memWData  = XLenU     
  val pcTarget  = XLenU     
  val rdAddr    = UInt(5.W)
  val ctrl      = new ControlSignals()
  val redirect  = Bool()    
}

// MEM -> WB
class MemoryPacket extends Bundle {
  val wbData   = XLenU
  val rdAddr   = UInt(5.W)
  val regWen   = Bool()
  val pcTarget = XLenU // 传递给 WB 用于重定向
}

// 反馈接口
class WriteBackIO extends Bundle {
  val wen  = Bool()
  val addr = UInt(5.W)
  val data = XLenU
}

class RedirectIO extends Bundle {
  val valid  = Bool()
  val target = XLenU
}