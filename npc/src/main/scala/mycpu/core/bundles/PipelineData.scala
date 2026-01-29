package mycpu.core.bundles

import chisel3._
import mycpu.common._

// 进程间通信包
class FetchPacket extends Bundle {
  val inst = UInt(32.W)
  val pc   = UInt(32.W) // [新增] 携带 PC，用于 Main 阶段校验
}

// 调度意图：描述 Main 线程该“做什么”
class CtrlSignals extends Bundle {
  val service   = ServiceType() 
  val aluOp     = ALUOp()       
  val arg1      = Arg1Type()    
  val arg2      = Arg2Type()    
  val immType   = ImmType()     
  val regWen    = Bool()   
  
  // 访存控制
  val memSize   = UInt(2.W)    // 00=Byte, 01=Half, 10=Word
  val memSigned = Bool()       // Load时是否符号扩展
  
  // CSR 控制
  val csrOp     = CSROp()
}

// 枚举定义
object ServiceType extends ChiselEnum {
  val ALU, MEM_RD, MEM_WR, BRANCH, JUMP, CSR, TRAP, NONE = Value
}

object Arg1Type extends ChiselEnum {
  val REG, PC, ZERO = Value
}

object Arg2Type extends ChiselEnum {
  val REG, IMM, CONST_4 = Value
}