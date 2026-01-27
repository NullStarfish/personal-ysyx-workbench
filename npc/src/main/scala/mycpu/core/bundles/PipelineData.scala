package mycpu.core.bundles

import chisel3._
import mycpu.common._

// 进程间通信包
class FetchPacket extends Bundle {
  val pc   = UInt(32.W)
  val inst = UInt(32.W)
}

// 调度意图：描述 Main 线程该“做什么”
class CtrlSignals extends Bundle {
  val service  = ServiceType() // 呼叫哪个服务
  val arg1     = Arg1Type()    // 参数1来源
  val arg2     = Arg2Type()    // 参数2来源
  val immType  = ImmType()     // 立即数解码方式
  val regWen   = Bool()        // 退休时是否写回寄存器
}

// 枚举定义
object ServiceType extends ChiselEnum {
  val ALU, MEM_RD, MEM_WR, BRANCH, CSR, NONE = Value
}

object Arg1Type extends ChiselEnum {
  val REG, PC, ZERO, ZIMM = Value
}

object Arg2Type extends ChiselEnum {
  val REG, IMM, CONST_4 = Value
}