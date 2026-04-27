package mycpu.core.components
import chisel3._

class SimState extends BlackBox {
  val io = IO(new Bundle {
    val clk   = Input(Clock())
    val reset = Input(Bool())
    val valid = Input(Bool()) // [新增]
    val pc    = Input(UInt(32.W))
    val dnpc  = Input(UInt(32.W)) // [新增]
    val regs_flat = Input(UInt(1024.W))
    val mtvec   = Input(UInt(32.W))
    val mepc    = Input(UInt(32.W))
    val mstatus = Input(UInt(32.W))
    val mcause  = Input(UInt(32.W))
    val inst    = Input(UInt(32.W))
  })
}