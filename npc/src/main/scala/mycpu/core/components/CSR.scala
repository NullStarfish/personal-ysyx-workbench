package mycpu.core.components

import chisel3._
import chisel3.util._
import mycpu.common._
import mycpu.core.bundles.{CsrDebugBundle, ExceptionBundle}

class CSR extends Module {
  val io = IO(new Bundle {
    // 指令输入
    val cmd      = Input(CSROp())
    val addr     = Input(UInt(12.W))
    val wdata    = Input(UInt(XLEN.W)) // 来自 rs1 或 zimm
    val rdata    = Output(UInt(XLEN.W))

    // 异常控制
    val except   = Input(new ExceptionBundle)
    val isMret   = Input(Bool())
    
    // 跳转目标
    val evec     = Output(UInt(XLEN.W)) // mtvec
    val epc      = Output(UInt(XLEN.W)) // mepc


    val debug_mtvec   = Output(UInt(XLEN.W))
    val debug_mepc    = Output(UInt(XLEN.W))
    val debug_mstatus = Output(UInt(XLEN.W))
    val debug_mcause  = Output(UInt(XLEN.W))
    val retireCsrs = Output(new CsrDebugBundle)

  })

  val MSTATUS = 0x300.U
  val MTVEC   = 0x305.U
  val MEPC    = 0x341.U
  val MCAUSE  = 0x342.U
  val MVENDORID = 0xBC0.U
  val MARCHID = 0xBC1.U

  val MSTATUS_MIE = 3
  val MSTATUS_MPIE = 7
  val MSTATUS_MPP_HI = 12
  val MSTATUS_MPP_LO = 11

  private def mstatusBit(index: Int): UInt = index.U(5.W)

  val mstatus = RegInit(0x1800.U(XLEN.W)) // Reset to M-mode
  val mtvec   = RegInit(0.U(XLEN.W))
  val mepc    = RegInit(0.U(XLEN.W))
  val mcause  = RegInit(0.U(XLEN.W))


  val mvendorid = RegInit(0x79737978.U(XLEN.W))
  val marchid = RegInit(25050151.U(XLEN.W))
  io.debug_mtvec   := mtvec
  io.debug_mepc    := mepc
  io.debug_mstatus := mstatus
  io.debug_mcause  := mcause




  // 读逻辑
  io.rdata := MuxLookup(io.addr, 0.U)(Seq(
    MSTATUS -> mstatus,
    MTVEC   -> mtvec,
    MEPC    -> mepc,
    MCAUSE  -> mcause,
    MVENDORID -> mvendorid,
    MARCHID -> marchid
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

  val nextMstatus = WireDefault(mstatus)
  val nextMtvec = WireDefault(mtvec)
  val nextMepc = WireDefault(mepc)
  val nextMcause = WireDefault(mcause)
  
  when (io.except.valid) {
    nextMepc := io.except.pc
    nextMcause := io.except.no.asUInt
    nextMstatus := mstatus.bitSet(mstatusBit(MSTATUS_MPIE), mstatus(MSTATUS_MIE))
      .bitSet(mstatusBit(MSTATUS_MIE), false.B)
      .bitSet(mstatusBit(MSTATUS_MPP_LO), true.B)
      .bitSet(mstatusBit(MSTATUS_MPP_HI), true.B)
  } .elsewhen (io.isMret) {
    nextMstatus := mstatus.bitSet(mstatusBit(MSTATUS_MIE), mstatus(MSTATUS_MPIE))
      .bitSet(mstatusBit(MSTATUS_MPIE), true.B)
      .bitSet(mstatusBit(MSTATUS_MPP_LO), false.B)
      .bitSet(mstatusBit(MSTATUS_MPP_HI), false.B)
  } .elsewhen (wen) {
    switch (io.addr) {
      is (MSTATUS) { nextMstatus := newVal }
      is (MTVEC)   { nextMtvec   := newVal }
      is (MEPC)    { nextMepc    := newVal }
      is (MCAUSE)  { nextMcause  := newVal }
    }
  }

  mstatus := nextMstatus
  mtvec := nextMtvec
  mepc := nextMepc
  mcause := nextMcause

  io.retireCsrs.mtvec := nextMtvec
  io.retireCsrs.mepc := nextMepc
  io.retireCsrs.mstatus := nextMstatus
  io.retireCsrs.mcause := nextMcause
}
