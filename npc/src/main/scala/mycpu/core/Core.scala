package mycpu.core

import chisel3._
import chisel3.util._
import mycpu.core.frontend.Fetch
import mycpu.core.backend._
import mycpu.core.bundles._
import mycpu.core.components.SimState
import mycpu.utils._
import mycpu.MemMap
import mycpu.peripherals.Xbar

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

  // === 2. 关键反馈环路 (Feedback Loop) ===
  // 这是实现 "单指令串行执行" 的核心。
  // Fetch 在 WB 完成并给出有效信号之前，会一直等待。
  // WB 的 debug_valid 实际上就是指令提交信号。
  
  fetch.io.next_pc      := wb.io.debug_out.dnpc
  fetch.io.pc_update_en := wb.io.debug_valid 


  val imem = Wire(new AXI4LiteBundle())
  val dmem = Wire(new AXI4LiteBundle())
  imem <> fetch.io.axi
  dmem <> lsu.io.axi
  arbiter.io.left <> imem
  arbiter.io.right <> dmem
  io.master <> arbiter.io.out


  //temp : 区分4bytes aligned的perip和其他直接寻址的外设：默认都是4bytes aligned
  



  // === 3. 其他连接 ===
  
  // 寄存器回写 (Forwarding 不需要在单周期模式下考虑，只要 WB 写完再 Fetch 下一条)
  // 注意：RegFile 在 Decode 阶段读取，在 WB 阶段写入。
  // 因为是串行，Fetch 下一条指令时，前一条的 WB 肯定已经发生（或者正在发生）。
  // 这里的时序：
  // Cycle N: WB fires (reg write happens) -> Fetch Update PC
  // Cycle N+1: Fetch starts reading RegFile (for next inst)
  // 所以不存在数据冒险。
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