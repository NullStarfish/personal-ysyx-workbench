package mycpu

import chisel3._


package object common {
  // === 全局配置 ===
  val XLEN = 32
  val START_ADDR = 0x20000000L

  def XLenU = UInt(XLEN.W)

  // === 枚举定义 ===
  
  object ALUOp extends ChiselEnum {
    val ADD, SUB, AND, OR, XOR = Value
    val SLT, SLTU, SLL, SRL, SRA = Value
    val COPY_A, COPY_B = Value 
    val NOP = Value
    // [修复] 添加缺失的乘除法枚举
    val MUL, DIV, REM = Value 
  }

  object CSROp extends ChiselEnum {
    val N = Value(0.U) // None
    val W = Value(1.U) // Write
    val S = Value(2.U) // Set
    val C = Value(3.U) // Clear
  }

  object ImmType extends ChiselEnum {
    val I, S, B, U, J, Z = Value
  }



  
}