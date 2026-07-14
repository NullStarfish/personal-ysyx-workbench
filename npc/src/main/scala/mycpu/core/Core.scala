package mycpu.core

import chisel3._
import chisel3.util._
import mycpu.cache._
import mycpu.common._
import mycpu.core.backend._
import mycpu.core.bundles._
import mycpu.core.components._
import mycpu.core.frontend.{Decode, Fetch, I$0Packet, I$0Stage, I$1Stage}
import mycpu.dpi.{DpiApi, SimStateBundle}
import mycpu.memory._
import mycpu.utils._
import mycpu.core.frontend.I$1Packet

class Core(
    enableDpi: Boolean = false,
    enableTracer: Boolean = ENABLE_TRACER,
    enableTraceFields: Boolean = ENABLE_TRACE_FIELDS,
) extends Module {
  val io = IO(new Bundle {
    val master = new AXI4Bundle(idWidth = AXI_ID_WIDTH, addrWidth = XLEN, dataWidth = XLEN)
    val debug_regs = Output(Vec(32, UInt(XLEN.W)))
    val debug_csrs = Output(new CsrDebugBundle)
    val simState = Output(new SimStateBundle)
  })



  val enableTrace = false
  if (enableTrace) {
    when (io.master.r.fire) {
      printf("[TOP] read fire. data: %x\n", io.master.r.bits.data)
    }
  }

  val icacheParams = CacheConfigs.SimpICache
  val fetch = Module(new Fetch(enableTraceFields = enableTraceFields, enableDpi = enableDpi))
  val cacheSet = Module(new CacheSet(icacheParams))
  val iCache0 = Module(new I$0Stage(icacheParams))
  val iCache1 = Module(new I$1Stage(icacheParams, enableDpi = enableDpi))
  val decode = Module(new Decode(enableTraceFields = enableTraceFields))
  val execute = Module(new Execute(enableTraceFields = enableTraceFields, enableDpi = enableDpi))
  val lsu = Module(new LSU(enableTraceFields = enableTraceFields, enableDpi = enableDpi))
  val writeBack = Module(new WriteBack(enableTraceFields = enableTraceFields, enableDpi = enableDpi))
  val csr = Module(new CSR)
  val memory = Module(new MemoryController)
  val hazard = Module(new HazardUnit)
  val ifI0 = Module(new FlushableStage(new IFPacket))
  val i0I1 = Module(new FlushableStage(new I$0Packet(icacheParams)))
  val i1Id = Module(new FlushableStage(new I$1Packet))
  val idEx = Module(new FlushableStage(new DecodePacket))
  val exMem = Module(new FlushableStage(new ExecutePacket(enableTraceFields)))
  val memWb = Module(new FlushableStage(new MemoryPacket))
  val tracer = if (enableTracer && enableTraceFields) Some(Module(new Tracer(enableDpi = enableDpi))) else None
  require(ENABLE_ICACHE, "the pipelined frontend currently requires ICache")

  // 两级前端流水寄存器由Core统一持有，stage本身只保留局部状态。
  fetch.io.out <> ifI0.io.enq
  cacheSet.io.lookup.valid := fetch.io.out.fire
  cacheSet.io.lookup.bits.index := icacheParams.index(fetch.io.out.bits.pc)
  cacheSet.io.lookup.bits.tag := icacheParams.tag(fetch.io.out.bits.pc)
  cacheSet.io.lookup.bits.wordOffset := icacheParams.wordOffset(fetch.io.out.bits.pc)

  iCache0.io.in <> ifI0.io.deq
  iCache0.io.cacheSetResp := cacheSet.io.lookupResp
  iCache0.io.out <> i0I1.io.enq

  iCache1.io.in <> i0I1.io.deq
  cacheSet.io.write <> iCache1.io.cacheSetWrite
  memory.io.icache <> iCache1.io.mem
  iCache1.io.out <> i1Id.io.enq

  assert(iCache1.io.out.fire === i1Id.io.enq.fire, "I$1 and I$1/ID must agree on output transfer")

  // Decode保持看到当前指令的valid；load-use只阻止真正deq，避免hazard组合环。
  decode.io.in <> i1Id.io.deq
  decode.io.out <> idEx.io.enq
  decode.io.regWrite <> writeBack.io.regWrite

  execute.io.in <> idEx.io.deq
  execute.io.out <> exMem.io.enq

  lsu.io.in <> exMem.io.deq
  lsu.io.out <> memWb.io.enq


  memory.io.lsu <> lsu.io.mem
  io.master <> memory.io.axi

  writeBack.io.in <> memWb.io.deq

  csr.io.cmd := writeBack.io.csr.cmd
  csr.io.addr := writeBack.io.csr.addr
  csr.io.wdata := writeBack.io.csr.wdata
  csr.io.except := writeBack.io.csr.except
  csr.io.isMret := writeBack.io.csr.isMret
  writeBack.io.csr.rdata := csr.io.rdata
  writeBack.io.csr.evec := csr.io.evec
  writeBack.io.csr.epc := csr.io.epc
  writeBack.io.csr.retireCsrs := csr.io.retireCsrs

  io.debug_regs := decode.io.debug_regs
  io.debug_csrs.mtvec := csr.io.debug_mtvec
  io.debug_csrs.mepc := csr.io.debug_mepc
  io.debug_csrs.mstatus := csr.io.debug_mstatus
  io.debug_csrs.mcause := csr.io.debug_mcause

    private def connectForward(dst: ForwardPacket, srcValid: Bool, src: ForwardSource): Unit = {
    dst.valid := srcValid && src.valid
    dst.addr := src.addr
    dst.data := src.data
  }

  connectForward(decode.io.forwards(0), execute.io.out.valid, execute.io.out.bits.forward)
  connectForward(decode.io.forwards(1), exMem.io.deq.valid, exMem.io.deq.bits.forward)

  hazard.io.raw.decode.rs1.valid := decode.io.out.valid && decode.io.out.bits.rawRs1.valid
  hazard.io.raw.decode.rs1.addr := decode.io.out.bits.rawRs1.addr
  hazard.io.raw.decode.rs2.valid := decode.io.out.valid && decode.io.out.bits.rawRs2.valid
  hazard.io.raw.decode.rs2.addr := decode.io.out.bits.rawRs2.addr

  val idExLateWrite =
    (idEx.io.deq.bits.memCtrl.en && !idEx.io.deq.bits.memCtrl.write) ||
      idEx.io.deq.bits.sys.csr.csrOp =/= CSROp.N
  val exMemLateWrite =
    (exMem.io.deq.bits.memCtrl.en && !exMem.io.deq.bits.memCtrl.write) ||
      exMem.io.deq.bits.sys.csr.csrOp =/= CSROp.N

  hazard.io.raw.idExLoad.valid := idEx.io.deq.valid && idExLateWrite
  hazard.io.raw.idExLoad.addr := idEx.io.deq.bits.wbCtrl.rd
  hazard.io.raw.exMemLoad.valid := exMem.io.deq.valid && exMemLateWrite
  hazard.io.raw.exMemLoad.addr := exMem.io.deq.bits.wbCtrl.rd
  hazard.io.raw.lsuLoad.valid := lsu.io.pendingLoad.valid
  hazard.io.raw.lsuLoad.addr := lsu.io.pendingLoad.addr

  val executeRedirect = execute.io.out.valid && execute.io.out.bits.ifRedct.redirect.valid
  val writeBackRedirect = writeBack.io.redirect.valid
  hazard.io.ctrl.redirect := executeRedirect || writeBackRedirect
  val redirectFlush = hazard.io.flush
  val loadUseStall = hazard.io.stall

  fetch.io.block := iCache0.io.blockFetch
  ifI0.io.blockEnq := iCache0.io.blockFetch
  ifI0.io.blockDeq := false.B
  i0I1.io.blockEnq := false.B
  i0I1.io.blockDeq := false.B
  i1Id.io.blockEnq := false.B
  i1Id.io.blockDeq := false.B

  fetch.io.redirect.valid := redirectFlush
  fetch.io.redirect.bits := Mux(
    writeBackRedirect,
    writeBack.io.redirect.bits,
    execute.io.out.bits.ifRedct.redirect.bits,
  )
  iCache0.io.flush := redirectFlush
  iCache1.io.flush := redirectFlush
  iCache1.io.fencei := writeBack.io.fencei
  cacheSet.io.flush := writeBack.io.fencei
  ifI0.io.flush := redirectFlush
  i0I1.io.flush := redirectFlush
  i1Id.io.flush := redirectFlush

  if (enableDpi) {
    val counters = DpiApi.counters(clock, reset.asBool, enabled = true)
    counters.pushToSim("core.cycles", true.B)
    counters.pushToSim("frontend.fetch_accepted", fetch.io.out.fire)
    counters.pushToSim("frontend.icache_output", iCache1.io.out.fire)
    counters.pushToSim("pipeline.decode_output", decode.io.out.fire)
    counters.pushToSim("pipeline.execute_output", execute.io.out.fire)
    counters.pushToSim("pipeline.lsu_output", lsu.io.out.fire)
    counters.pushToSim("pipeline.if_i0_valid_cycles", ifI0.io.deq.valid)
    counters.pushToSim("pipeline.i0_i1_valid_cycles", i0I1.io.deq.valid)
    counters.pushToSim("pipeline.i1_id_valid_cycles", i1Id.io.deq.valid)
    counters.pushToSim("pipeline.id_ex_valid_cycles", idEx.io.deq.valid)
    counters.pushToSim("pipeline.ex_mem_valid_cycles", exMem.io.deq.valid)
    counters.pushToSim("pipeline.mem_wb_valid_cycles", memWb.io.deq.valid)
    counters.pushToSim("hazard.load_use_cycles", loadUseStall)
    counters.pushToSim("frontend.flushes", redirectFlush)
    counters.pushToSim("frontend.execute_redirects", executeRedirect)
    counters.pushToSim("frontend.writeback_redirects", writeBackRedirect)
    counters.pushToSim("lsu.backpressure_cycles", execute.io.out.valid && !lsu.io.in.ready)

    // Keep one live read port as the common entry point for simulation-only RTL reports.
    dontTouch(counters.readFromSim("retire.total"))
  }

  idEx.io.flush := writeBackRedirect
  exMem.io.flush := writeBackRedirect
  memWb.io.flush := writeBackRedirect

  idEx.io.blockEnq := loadUseStall
  idEx.io.blockDeq := false.B
  exMem.io.blockEnq := false.B
  exMem.io.blockDeq := false.B
  memWb.io.blockEnq := false.B
  memWb.io.blockDeq := false.B

  when(redirectFlush) {
    idEx.io.enq.valid := false.B
  }

  when(writeBackRedirect) {
    lsu.io.in.valid := false.B
  }

  if (enableTracer && enableTraceFields) {
    val tracerMod = tracer.get
    tracerMod.io.retireTrace := writeBack.io.retireTrace.get
    tracerMod.io.gprs := decode.io.debug_regs
    tracerMod.io.csrs := io.debug_csrs
    io.simState := tracerMod.io.simState
  } else {
    io.simState := 0.U.asTypeOf(new SimStateBundle)
  }
}
