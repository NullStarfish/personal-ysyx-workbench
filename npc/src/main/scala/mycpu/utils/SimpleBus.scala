package mycpu.utils
import chisel3._
import chisel3.util._
import mycpu.common._

// ==============================================================================
// 简单的用户侧请求/响应包定义
// ==============================================================================
class AXI4TransReq (val addrWidth: Int = XLEN) extends Bundle {
  val id    = UInt(AXI_ID_WIDTH.W)
  val addr  = UInt(addrWidth.W)
  val len   = UInt(8.W)
  val size  = UInt(3.W)
  val burst = UInt(2.W)
  val lock  = Bool()
  val cache = UInt(4.W)
  val prot  = UInt(3.W)
  val qos   = UInt(4.W)
}


class AXI4WriteStream (val dataWidth: Int = XLEN) extends Bundle {
  val wdata = UInt(dataWidth.W)
  val wstrb = UInt((dataWidth/8).W)
  val last  = Bool()
}

class AXI4ReadStream (val dataWidth: Int = XLEN) extends Bundle {
  val rid     = UInt(AXI_ID_WIDTH.W)
  val rdata   = UInt(dataWidth.W)
  val rresp   = UInt(2.W)
  val rlast   = Bool()
}

class AXI4WriteBackResp (val dataWidth: Int = XLEN) extends  Bundle {
  val bid     = UInt(AXI_ID_WIDTH.W)
  val bresp   = UInt(2.W)
}