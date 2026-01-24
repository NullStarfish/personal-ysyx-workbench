package mycpu.core

import chisel3._
import chisel3.util._
import mycpu._
import mycpu.core.frontend.Fetch
import mycpu.core.backend._
import mycpu.core.bundles._
import mycpu.core.components.SimState
import mycpu.utils._
import mycpu.common._
import mycpu.common.AXI_ID_WIDTH
import mycpu.peripherals.DifftestSkip


class Core extends Module {
  val io = IO(new Bundle {
  val master = new AXI4LiteBundle()
  })

  val fetch   = Module(new Fetch)
  val decode  = Module(new Decode)
  val execute = Module(new Execute)
  val lsu     = Module(new LSU)
  val wb      = Module(new WriteBack)
  val simState = Module(new SimState)
  val arbiter  = Module(new SimpleAXIArbiter)





  // === 1. 构建流水线数据通路 (Pipeline/Chain) ===
  // 使用 Queue(1) 实现你期望的 "Ready 依赖" 和全解耦握手。
  // 只有当下一级 Queue ready 时，上一级才能 fire。
  
  // IF -> ID
  val q1 = Module(new Queue(new FetchPacket, 1))
  q1.io.enq <> fetch.io.out
  decode.io.in <> q1.io.deq

  // ID -> EX
  val q2 = Module(new Queue(new DecodePacket, 1))
  q2.io.enq <> decode.io.out
  execute.io.in <> q2.io.deq

  // EX -> MEM
  val q3 = Module(new Queue(new ExecutePacket, 1))
  q3.io.enq <> execute.io.out
  lsu.io.in <> q3.io.deq

  // MEM -> WB
  val q4 = Module(new Queue(new MemoryPacket, 1))
  q4.io.enq <> lsu.io.out
  wb.io.in  <> q4.io.deq


  
  fetch.io.next_pc      := wb.io.debug_out.dnpc
  fetch.io.pc_update_en := wb.io.debug_valid 


  val imem = Wire(new AXI4Bundle(AXI_ID_WIDTH, XLEN, XLEN))
  val dmem = Wire(new AXI4Bundle(AXI_ID_WIDTH, XLEN, XLEN))
  imem <> fetch.io.axi
  dmem <> lsu.io.axi
  arbiter.io.left <> imem
  arbiter.io.right <> dmem
  io.master <> arbiter.io.out

  val skip = Module(new DifftestSkip())
  skip.io.clock := clock
  val validAddr : UInt= Mux(arbiter.io.out.ar.fire, arbiter.io.out.ar.bits.addr, Mux(arbiter.io.out.aw.fire, arbiter.io.out.aw.bits.addr, 0.U))
  skip.io.skip := MemMap.isDifftestSkip(validAddr)



  decode.io.regWrite <> wb.io.regWrite

  // SimState 连接 (用于 C++ 差分测试)
  simState.io.clk   := clock
  simState.io.reset := reset.asBool
  simState.io.valid := wb.io.debug_valid
  simState.io.pc    := wb.io.debug_out.pc
  simState.io.inst  := wb.io.debug_out.inst
  simState.io.dnpc  := wb.io.debug_out.dnpc
  
  simState.io.regs_flat := decode.io.debug_regs.asUInt
  simState.io.mtvec     := execute.io.debug_csrs.mtvec
  simState.io.mepc      := execute.io.debug_csrs.mepc
  simState.io.mstatus   := execute.io.debug_csrs.mstatus
  simState.io.mcause    := execute.io.debug_csrs.mcause

  // 内存接口连接

}