package mycpu

import chisel3._
import mycpu.core.Core
import mycpu.peripherals._ // 引入新增的模块
import mycpu.utils._
import circt.stage.ChiselStage 

class Top extends Module {
    val io = IO(new Bundle {})

    val core       = Module(new Core)
    val memArbiter = Module(new SimpleAXIArbiter)
    val xbar       = Module(new Xbar(MemMap.devices))
    
    // 1. 实例化外设 (使用封装后的 Wrapper)
    val sram   = Module(new SRAMAXIWrapper)
    val serial = Module(new Serial)
    val clint  = Module(new CLINT)

    // 2. Core -> Arbiter -> Xbar
    memArbiter.io.left <> core.io.imem
    memArbiter.io.right <> core.io.dmem
    xbar.io.in <> memArbiter.io.out

    // 3. 极速连接外设 (核心优化点)
    // 定义一个辅助函数，或者直接写
    def connectDevice(name: String, port: AXI4LiteBundle): Unit = {
        val idx = MemMap.getIndex(name)
        xbar.io.slaves(idx) <> port
    }

    connectDevice("SRAM",   sram.io.bus)
    connectDevice("SERIAL", serial.io.bus)
    connectDevice("CLINT",  clint.io.bus)
}



object Main extends App {
  ChiselStage.emitSystemVerilogFile(
    new Top,
    args = Array("--target-dir", "src/main/verilog/gen"),
    firtoolOpts = Array(
      "--disable-all-randomization"
    )
  )
}