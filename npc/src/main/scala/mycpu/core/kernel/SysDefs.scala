package mycpu.core.kernel

import chisel3._
import chisel3.util._
import mycpu.common._

// ==============================================================================
// 1. 驱动元数据与时序定义
// ==============================================================================
object DriverTiming {
  sealed trait Type
  case object Combinational extends Type 
  case object Sequential    extends Type 
}

case class DriverMeta(
  name: String,
  readTiming: DriverTiming.Type,
  writeTiming: DriverTiming.Type = DriverTiming.Sequential, 
  allowMultiReader: Boolean = true 
)

object Errno {
  val ESUCCESS = 0.U(8.W)
  val EBUSY    = 16.U(8.W)
  val EINVAL   = 22.U(8.W)
}

// ==============================================================================
// 2. 隐式通信通道 (Hidden Channel)
// ==============================================================================
class ClientChannelReq(val addrWidth: Int, val dataWidth: Int) extends Bundle {
  val valid = Bool()
  val addr  = UInt(addrWidth.W)
  val data  = UInt(dataWidth.W)
  val size  = UInt(2.W)
  val wen   = Bool() 
}

class ClientChannel(val addrWidth: Int, val dataWidth: Int) extends Bundle {
  val req = new ClientChannelReq(addrWidth, dataWidth)
  val respData = Output(UInt(dataWidth.W)) 
  val ready    = Output(Bool())            
  val error    = Output(UInt(8.W))
}

// ==============================================================================
// 3. 返回结果包
// ==============================================================================
class SysResult extends Bundle {
  // [修复] 使用全局定义的宽位宽
  val value = UInt(KERNEL_DATA_WIDTH.W) 
  val errno = UInt(8.W)
  
  def isError: Bool = errno =/= Errno.ESUCCESS
}