package mycpu.core.components

import chisel3._
import chisel3.util._
import mycpu.common._

class ALU extends Module {
  val io = IO(new Bundle {
    val a   = Input(UInt(XLEN.W))
    val b   = Input(UInt(XLEN.W))
    val op  = Input(ALUOp()) 
    val out = Output(UInt(XLEN.W))
  })

  // 默认输出
  io.out := 0.U

  // 移位量只取低5位 (RV32I)
  val shamt = io.b(4, 0)

  switch(io.op) {
    is(ALUOp.ADD) { io.out := io.a + io.b }
    is(ALUOp.SUB) { io.out := io.a - io.b }
    
    is(ALUOp.AND) { io.out := io.a & io.b }
    is(ALUOp.OR)  { io.out := io.a | io.b }
    is(ALUOp.XOR) { io.out := io.a ^ io.b }
    
    is(ALUOp.SLT) { io.out := (io.a.asSInt < io.b.asSInt).asUInt }
    is(ALUOp.SLTU){ io.out := (io.a < io.b).asUInt }
    
    is(ALUOp.SLL) { io.out := io.a << shamt }
    is(ALUOp.SRL) { io.out := io.a >> shamt }
    is(ALUOp.SRA) { io.out := (io.a.asSInt >> shamt).asUInt }
    
    is(ALUOp.COPY_A) { io.out := io.a }
    is(ALUOp.COPY_B) { io.out := io.b }
    
    // 乘除法单元暂留空 (返回0或a，防止Latches)
    is(ALUOp.MUL, ALUOp.DIV, ALUOp.REM) { io.out := 0.U }
    is(ALUOp.NOP) { io.out := 0.U }
  }
}