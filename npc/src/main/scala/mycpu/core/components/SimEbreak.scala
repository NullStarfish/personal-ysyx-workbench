package mycpu.core.components

import chisel3._

class SimEbreak extends BlackBox {
  val io = IO(new Bundle {
    val valid = Input(Bool())
    val is_ebreak = Input(UInt(32.W))
  })
}