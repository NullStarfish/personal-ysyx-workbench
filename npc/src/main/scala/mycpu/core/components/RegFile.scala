package mycpu.core.components

import chisel3._
import mycpu.common._

class RegFile extends Module {
  val io = IO(new Bundle {
    val raddr1 = Input(UInt(5.W))
    val raddr2 = Input(UInt(5.W))
    val rdata1 = Output(UInt(XLEN.W))
    val rdata2 = Output(UInt(XLEN.W))

    val wen    = Input(Bool())
    val waddr  = Input(UInt(5.W))
    val wdata  = Input(UInt(XLEN.W))
    
    // [新增] 调试接口：导出所有寄存器用于 DPI
    val debug_regs = Output(Vec(32, UInt(XLEN.W)))
  })

  // 使用 Reg(Vec) 替代 Mem，方便导出
  // 初始化为 0
  val regs = RegInit(VecInit(Seq.fill(32)(0.U(XLEN.W))))

  when(io.wen && io.waddr =/= 0.U) {
    regs(io.waddr) := io.wdata
  }

  io.rdata1 := Mux(io.raddr1 === 0.U, 0.U, regs(io.raddr1))
  io.rdata2 := Mux(io.raddr2 === 0.U, 0.U, regs(io.raddr2))
  
  // 连接调试输出
  io.debug_regs := regs
}