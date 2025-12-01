package mycpu.peripherals

import chisel3._

class DifftestSkip extends BlackBox {
  val io = IO(new Bundle {
    val clock   = Input(Clock())
    val skip  = Input(Bool()) // 控制是否跳过当前指令的对比测试
  })
}