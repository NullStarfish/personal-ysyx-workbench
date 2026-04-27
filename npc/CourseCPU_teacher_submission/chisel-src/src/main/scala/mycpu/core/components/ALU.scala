package mycpu.core.components

import chisel3._
import chisel3.util._
import mycpu.common._

class ALU extends Module {
  val io = IO(new Bundle {
    val a   = Input(UInt(XLEN.W))
    val b   = Input(UInt(XLEN.W))
    val op  = Input(ALUOp()) // 使用之前定义的 Enum
    val out = Output(UInt(XLEN.W))
  })

  val out = WireDefault(0.U(XLEN.W))
  val shamt = io.b(4, 0) // 移位量仅取低5位

  switch(io.op) {
    // 算术运算
    is(ALUOp.ADD) { out := io.a + io.b }
    is(ALUOp.SUB) { out := io.a - io.b }
    
    // 逻辑运算
    is(ALUOp.AND) { out := io.a & io.b }
    is(ALUOp.OR)  { out := io.a | io.b }
    is(ALUOp.XOR) { out := io.a ^ io.b }
    
    // 比较运算 (结果置 1 或 0)
    is(ALUOp.SLT) { out := (io.a.asSInt < io.b.asSInt).asUInt } // 有符号比较
    is(ALUOp.SLTU){ out := (io.a < io.b).asUInt }               // 无符号比较
    
    // 移位运算
    is(ALUOp.SLL) { out := io.a << shamt }
    is(ALUOp.SRL) { out := io.a >> shamt }
    is(ALUOp.SRA) { out := (io.a.asSInt >> shamt).asUInt }      // 算术右移 (保留符号位)
    
    // 透传 (用于 LUI 等指令)
    is(ALUOp.COPY_A) { out := io.a }
    is(ALUOp.COPY_B) { out := io.b }
    
    // 乘除法占位 (返回 0 或保持原值，防止综合报错)
    is(ALUOp.MUL, ALUOp.DIV, ALUOp.REM) { out := 0.U } 
  }

  io.out := out
}