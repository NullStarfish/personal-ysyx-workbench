package mycpu.core

import chisel3._
import chisel3.util._
import mycpu.core.frontend.Fetch
import mycpu.core.backend._
import mycpu.core.bundles._
import mycpu.core.components.SimState // [新增]
import mycpu.memory.AXI4LiteMasterIO

class Core extends Module {
  val io = IO(new Bundle {
    val imem = new AXI4LiteMasterIO()
    val dmem = new AXI4LiteMasterIO()
  })

  val fetch   = Module(new Fetch)
  val decode  = Module(new Decode)
  val execute = Module(new Execute)
  val lsu     = Module(new LSU)
  val wb      = Module(new WriteBack)
  
  // [新增] 实例化状态探针
  val simState = Module(new SimState)

  // ... (原有的流水线连接代码保持不变) ...
  decode.io.in <> Queue(fetch.io.out, 2)
  execute.io.in <> Queue(decode.io.out, 2)
  lsu.io.in <> Queue(execute.io.out, 2)
  wb.io.in <> Queue(lsu.io.out, 2)
  decode.io.regWrite <> wb.io.regWrite
  fetch.io.redirect.valid := execute.io.redirect.valid
  fetch.io.redirect.bits  := execute.io.redirect.bits
  io.imem <> fetch.io.axi
  io.dmem <> lsu.io.axi
  
  // 连接 SimState
  simState.io.clk := clock
  simState.io.reset := reset.asBool
  simState.io.pc := fetch.io.debug_pc
  simState.io.regs_flat := decode.io.debug_regs.asUInt
  
  // [新增] 连接 CSR
  simState.io.mtvec   := execute.io.debug_csrs.mtvec
  simState.io.mepc    := execute.io.debug_csrs.mepc
  simState.io.mstatus := execute.io.debug_csrs.mstatus
  simState.io.mcause  := execute.io.debug_csrs.mcause
}