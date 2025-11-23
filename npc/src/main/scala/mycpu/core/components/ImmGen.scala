package mycpu.core.components

import chisel3._
import chisel3.util._
import mycpu.common._

class ImmGen extends Module {
  val io = IO(new Bundle {
    val inst = Input(UInt(32.W))
    val sel  = Input(ImmType()) 
    val out  = Output(UInt(XLEN.W))
  })

  val inst = io.inst

  // RISC-V 立即数生成逻辑 (使用 Fill 进行显式符号扩展，确保全为 UInt 类型操作)

  // I-Type: inst[31:20] (12 bits) -> Sign Ext to 32 bits
  // Fill(20, inst(31)) 重复符号位 20 次，加上 inst[31:20] 共 32 位
  val immI = Cat(Fill(20, inst(31)), inst(31, 20))

  // S-Type: inst[31:25], inst[11:7] (12 bits) -> Sign Ext to 32 bits
  val immS = Cat(Fill(20, inst(31)), inst(31, 25), inst(11, 7))

  // B-Type: inst[31], inst[7], inst[30:25], inst[11:8], 0 (13 bits) -> Sign Ext
  // 原始位宽 13 位，需要补充 19 位符号位 + 1 位原有符号位 = Fill(20, ...)
  val immB = Cat(Fill(20, inst(31)), inst(7), inst(30, 25), inst(11, 8), 0.U(1.W))

  // U-Type: inst[31:12], 12'b0 (32 bits)
  val immU = Cat(inst(31, 12), 0.U(12.W))

  // J-Type: inst[31], inst[19:12], inst[20], inst[30:21], 0 (21 bits) -> Sign Ext
  // 原始位宽 21 位，需要补充 11 位符号位 + 1 位原有符号位 = Fill(12, ...)
  val immJ = Cat(Fill(12, inst(31)), inst(19, 12), inst(20), inst(30, 21), 0.U(1.W))

  // Z-Type (Zero): 0
  val immZ = 0.U(XLEN.W)

  io.out := MuxLookup(io.sel, immZ)(Seq(
    ImmType.I -> immI,
    ImmType.S -> immS,
    ImmType.B -> immB,
    ImmType.U -> immU,
    ImmType.J -> immJ,
    ImmType.Z -> immZ
  ))
}