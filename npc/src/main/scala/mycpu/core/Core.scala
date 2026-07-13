package mycpu.core

import chisel3._
import chisel3.util._
import mycpu.cache._
import mycpu.common._
import mycpu.core.backend._
import mycpu.core.bundles._
import mycpu.core.components._
import mycpu.core.frontend.{Decode, Fetch, I$0Packet, I$0Stage, I$1Stage}
import mycpu.dpi.SimStateBundle
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
  val iCache1 = Module(new I$1Stage(icacheParams))
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

  if (enableDpi && enableTraceFields) {
    val flushTrace = Module(new FlushTrace)
    flushTrace.io.clk := clock
    flushTrace.io.reset := reset.asBool
    flushTrace.io.flush := redirectFlush
    flushTrace.io.pc := Mux(writeBackRedirect, writeBack.io.retireTrace.get.bits.dnpc, execute.io.out.bits.retireTrace.get.dnpc)
    flushTrace.io.inst := Mux(writeBackRedirect, writeBack.io.retireTrace.get.bits.inst, execute.io.out.bits.retireTrace.get.inst)

    val pipelineTrace = Module(new PipelineTrace)
    pipelineTrace.io.clk := clock
    pipelineTrace.io.reset := reset.asBool
    pipelineTrace.io.fetchOut.valid := iCache1.io.out.fire
    pipelineTrace.io.fetchOut.bits.pc := iCache1.io.out.bits.pc
    pipelineTrace.io.fetchOut.bits.inst := iCache1.io.out.bits.inst
    pipelineTrace.io.decodeOut.valid := decode.io.out.fire
    pipelineTrace.io.decodeOut.bits.pc := decode.io.out.bits.retireTrace.get.pc
    pipelineTrace.io.decodeOut.bits.inst := decode.io.out.bits.retireTrace.get.inst
    pipelineTrace.io.executeOut.valid := execute.io.out.fire
    pipelineTrace.io.executeOut.bits.pc := execute.io.out.bits.retireTrace.get.pc
    pipelineTrace.io.executeOut.bits.inst := execute.io.out.bits.retireTrace.get.inst
    pipelineTrace.io.lsuOut.valid := lsu.io.out.fire
    pipelineTrace.io.lsuOut.bits.pc := lsu.io.out.bits.retireTrace.get.pc
    pipelineTrace.io.lsuOut.bits.inst := lsu.io.out.bits.retireTrace.get.inst
    pipelineTrace.io.retire.valid := writeBack.io.retireTrace.get.valid
    pipelineTrace.io.retire.bits.pc := writeBack.io.retireTrace.get.bits.pc
    pipelineTrace.io.retire.bits.inst := writeBack.io.retireTrace.get.bits.inst

    val hazardTrace = Module(new HazardTrace)
    hazardTrace.io.clk := clock
    hazardTrace.io.reset := reset.asBool
    hazardTrace.io.loadUseStall := loadUseStall
    hazardTrace.io.redirectFlush := redirectFlush
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
