package mycpu.core

import chisel3._
import chisel3.util._
import mycpu.cache._
import mycpu.common._
import mycpu.core.backend._
import mycpu.core.bundles._
import mycpu.core.components._
import mycpu.core.frontend.{Decode, Fetch}
import mycpu.dpi.SimStateBundle
import mycpu.memory._
import mycpu.utils._

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

  val fetch = Module(new Fetch(enableTraceFields = enableTraceFields, enableDpi = enableDpi))
  val decode = Module(new Decode(enableTraceFields = enableTraceFields))
  val execute = Module(new Execute(enableTraceFields = enableTraceFields, enableDpi = enableDpi))
  val lsu = Module(new LSU(enableTraceFields = enableTraceFields, enableDpi = enableDpi))
  val writeBack = Module(new WriteBack(enableTraceFields = enableTraceFields))
  val memory = Module(new MemoryController)
  val hazard = Module(new HazardUnit)
  val ifId = Module(new FlushableStage(new FetchPacket))
  val idEx = Module(new FlushableStage(new DecodePacket))
  val exMem = Module(new FlushableStage(new ExecutePacket(enableTraceFields)))
  val memWb = Module(new FlushableStage(new MemoryPacket))
  val tracer = if (enableTracer && enableTraceFields) Some(Module(new Tracer(enableDpi = enableDpi))) else None
  val icacheOpt = if (ENABLE_ICACHE) Some(Module(new ICache(params = CacheConfigs.SimpICache, enableDpi = enableDpi))) else None

  icacheOpt match {
    case Some(icache) =>
    icache.io.cpuReq.valid := fetch.io.instReq.valid
    icache.io.cpuReq.bits := fetch.io.instReq.bits
    fetch.io.instReq.ready := icache.io.cpuReq.ready

    fetch.io.instResp <> icache.io.cpuReply

    memory.io.icache <> icache.io.mem

    icache.io.prefetch.valid := false.B
    icache.io.prefetch.bits := 0.U

    case None =>
    memory.io.icache.a.valid := fetch.io.instReq.valid
    memory.io.icache.a.bits.addr := fetch.io.instReq.bits
    memory.io.icache.a.bits.size := 2.U
    memory.io.icache.a.bits.len := 0.U
    memory.io.icache.a.bits.write := false.B
    memory.io.icache.a.bits.id := 0.U
    fetch.io.instReq.ready := memory.io.icache.a.ready
    fetch.io.instResp.valid := memory.io.icache.r.valid
    fetch.io.instResp.bits.inst := memory.io.icache.r.bits.data
    fetch.io.instResp.bits.hit := false.B
    memory.io.icache.r.ready := fetch.io.instResp.ready
  }

  memory.io.lsu <> lsu.io.mem
  io.master <> memory.io.axi

  writeBack.io.in <> memWb.io.deq
  decode.io.regWrite <> writeBack.io.regWrite

  io.debug_regs := decode.io.debug_regs
  io.debug_csrs.mtvec := execute.io.debug_csrs.mtvec
  io.debug_csrs.mepc := execute.io.debug_csrs.mepc
  io.debug_csrs.mstatus := execute.io.debug_csrs.mstatus
  io.debug_csrs.mcause := execute.io.debug_csrs.mcause

  fetch.io.out <> ifId.io.enq
  decode.io.in <> ifId.io.deq
  decode.io.out <> idEx.io.enq

  execute.io.in <> idEx.io.deq
  execute.io.out <> exMem.io.enq
  lsu.io.in <> exMem.io.deq
  lsu.io.out <> memWb.io.enq

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

  hazard.io.raw.idExLoad.valid := idEx.io.deq.valid && idEx.io.deq.bits.memCtrl.en && !idEx.io.deq.bits.memCtrl.write
  hazard.io.raw.idExLoad.addr := idEx.io.deq.bits.wbCtrl.rd
  hazard.io.raw.exMemLoad.valid := exMem.io.deq.valid && exMem.io.deq.bits.memCtrl.en && !exMem.io.deq.bits.memCtrl.write
  hazard.io.raw.exMemLoad.addr := exMem.io.deq.bits.wbCtrl.rd
  hazard.io.raw.lsuLoad.valid := lsu.io.pendingLoad.valid
  hazard.io.raw.lsuLoad.addr := lsu.io.pendingLoad.addr

  val executeRedirect = execute.io.out.valid && execute.io.out.bits.ifRedct.redirect.valid
  val executeFenceI = execute.io.out.valid && execute.io.out.bits.fencei
  hazard.io.ctrl.redirect := executeRedirect
  val redirectFlush = hazard.io.flush
  val loadUseStall = hazard.io.stall

  fetch.io.redirect.valid := redirectFlush
  fetch.io.redirect.bits := execute.io.out.bits.ifRedct.redirect.bits
  icacheOpt.foreach { icache =>
    icache.io.fencei := executeFenceI
  }

  if (enableDpi && enableTraceFields) {
    val flushTrace = Module(new FlushTrace)
    flushTrace.io.clk := clock
    flushTrace.io.reset := reset.asBool
    flushTrace.io.flush := redirectFlush
    flushTrace.io.pc := execute.io.out.bits.retireTrace.get.dnpc
    flushTrace.io.inst := execute.io.out.bits.retireTrace.get.inst

    val pipelineTrace = Module(new PipelineTrace)
    pipelineTrace.io.clk := clock
    pipelineTrace.io.reset := reset.asBool
    pipelineTrace.io.fetchOut.valid := fetch.io.out.fire
    pipelineTrace.io.fetchOut.bits.pc := fetch.io.out.bits.pc
    pipelineTrace.io.fetchOut.bits.inst := fetch.io.out.bits.inst
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

  ifId.io.flush := redirectFlush
  idEx.io.flush := false.B
  exMem.io.flush := false.B
  memWb.io.flush := false.B

  ifId.io.blockEnq := false.B
  ifId.io.blockDeq := loadUseStall
  idEx.io.blockEnq := loadUseStall
  idEx.io.blockDeq := false.B
  exMem.io.blockEnq := false.B
  exMem.io.blockDeq := false.B
  memWb.io.blockEnq := false.B
  memWb.io.blockDeq := false.B

  when(redirectFlush) {
    ifId.io.enq.valid := false.B
    idEx.io.enq.valid := false.B
  }

  if (enableTracer && enableTraceFields) {
    val tracerMod = tracer.get
    tracerMod.io.retireTrace := writeBack.io.retireTrace.get
    tracerMod.io.gprs := decode.io.debug_regs
    tracerMod.io.csrs := execute.io.debug_csrs
    io.simState := tracerMod.io.simState
  } else {
    io.simState := 0.U.asTypeOf(new SimStateBundle)
  }
}
