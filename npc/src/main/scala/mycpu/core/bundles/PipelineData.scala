package mycpu.core.bundles

import chisel3._
import mycpu.common._

// 进程间通信包
class FetchPacket extends Bundle {
  val inst = UInt(32.W)
}

// 调度意图：描述 Main 线程该“做什么”
class CtrlSignals extends Bundle {
  val service  = ServiceType() 
  val aluOp    = ALUOp()       // [新增] 明确的 ALU 操作类型
  val arg1     = Arg1Type()    
  val arg2     = Arg2Type()    
  val immType  = ImmType()     
  val regWen   = Bool()   

  // [新增] 访存控制信号，用于传递给 MainProcess
  val memSize   = UInt(2.W)    // 00=Byte, 01=Half, 10=Word
  val memSigned = Bool()       // 读操作是否符号扩展     
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