package mycpu.core.components

import chisel3._
import chisel3.util._
import mycpu.common._

class CSR extends Module {
  val io = IO(new Bundle {
    // 指令输入
    val cmd      = Input(CSROp())
    val addr     = Input(UInt(12.W))
    val wdata    = Input(UInt(XLEN.W)) // 来自 rs1 或 zimm
    val rdata    = Output(UInt(XLEN.W))

    // 异常控制
    val pc       = Input(UInt(XLEN.W)) // 当前指令 PC
    val isEcall  = Input(Bool())
    val isMret   = Input(Bool())
    
    // 跳转目标
    val evec     = Output(UInt(XLEN.W)) // mtvec
    val epc      = Output(UInt(XLEN.W)) // mepc


    val debug_mtvec   = Output(UInt(XLEN.W))
    val debug_mepc    = Output(UInt(XLEN.W))
    val debug_mstatus = Output(UInt(XLEN.W))
    val debug_mcause  = Output(UInt(XLEN.W))

  })

  val MSTATUS = 0x300.U
  val MTVEC   = 0x305.U
  val MEPC    = 0x341.U
  val MCAUSE  = 0x342.U

  val mstatus = RegInit(0x1800.U(XLEN.W)) // Reset to M-mode
  val mtvec   = RegInit(0.U(XLEN.W))
  val mepc    = RegInit(0.U(XLEN.W))
  val mcause  = RegInit(0.U(XLEN.W))

  io.debug_mtvec   := mtvec
  io.debug_mepc    := mepc
  io.debug_mstatus := mstatus
  io.debug_mcause  := mcause




  // 读逻辑
  io.rdata := MuxLookup(io.addr, 0.U)(Seq(
    MSTATUS -> mstatus,
    MTVEC   -> mtvec,
    MEPC    -> mepc,
    MCAUSE  -> mcause
  ))
  
  io.evec := mtvec
  io.epc  := mepc

  // 写逻辑
  val oldVal = io.rdata
  val newVal = MuxLookup(io.cmd, oldVal)(Seq(
    CSROp.W -> io.wdata,
    CSROp.S -> (oldVal | io.wdata),
    CSROp.C -> (oldVal & ~io.wdata)
  ))
  
  val wen = io.cmd =/= CSROp.N
  
  when (io.isEcall) {
    mepc := io.pc
    mcause := 11.U // Machine ECALL
    // mstatus push logic (MIE -> MPIE) 略简化
  } .elsewhen (io.isMret) {
    // mstatus pop logic (MPIE -> MIE) 略简化
  } .elsewhen (wen) {
    switch (io.addr) {
      is (MSTATUS) { mstatus := newVal }
      is (MTVEC)   { mtvec   := newVal }
      is (MEPC)    { mepc    := newVal }
      is (MCAUSE)  { mcause  := newVal }
    }
  }
}