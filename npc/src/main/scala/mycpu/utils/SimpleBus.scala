package mycpu.utils
import chisel3._
import chisel3.util._
import mycpu.common._

// ==============================================================================
// 简单的用户侧请求/响应包定义
// ==============================================================================
class SimpleReadReq(val addrWidth: Int = XLEN) extends Bundle {
  val addr = UInt(addrWidth.W)
}

class SimpleWriteReq(val addrWidth: Int = XLEN, val dataWidth: Int = XLEN) extends Bundle {
  val addr  = UInt(addrWidth.W)
  val wdata = UInt(dataWidth.W)
  val wstrb = UInt((dataWidth/8).W)
}

class SimpleBusResp(val dataWidth: Int = XLEN) extends Bundle {
  val rdata   = UInt(dataWidth.W)
  val isError = Bool() 
}
