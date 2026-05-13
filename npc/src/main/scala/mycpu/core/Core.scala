package mycpu.core

import chisel3._
import chisel3.util._
import mycpu.common._
import mycpu.core.backend._
import mycpu.core.bundles._
import mycpu.core.components._
import mycpu.core.frontend.{Decode, Fetch}
import mycpu.dpi.SimStateBundle
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

  val fetch = Module(new Fetch(enableTraceFields = enableTraceFields, enableDpi = enableDpi))
  val decode = Module(new Decode(enableTraceFields = enableTraceFields))
  val operandSelect = Module(new ExecuteOperandSelect(enableTraceFields = enableTraceFields))
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

  memory.io.fetchReq <> fetch.io.fetch
  fetch.io.reply <> memory.io.fetchReply
  memory.io.lsuReq <> lsu.io.req
  lsu.io.reply <> memory.io.lsuReply
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

  operandSelect.io.in <> idEx.io.deq
  execute.io.in <> operandSelect.io.out
  execute.io.out <> exMem.io.enq
  lsu.io.in <> exMem.io.deq
  lsu.io.out <> memWb.io.enq

  operandSelect.io.exForward.valid := exMem.io.deq.valid
  operandSelect.io.exForward.bits := exMem.io.deq.bits

  val memForward = Wire(Valid(new MemoryPacket))
  memForward.valid := lsu.io.out.valid || memWb.io.deq.valid
  memForward.bits := Mux(lsu.io.out.valid, lsu.io.out.bits, memWb.io.deq.bits)
  operandSelect.io.memForward := memForward

  hazard.io.raw.decode.rs1.valid := decode.io.out.valid && decode.io.out.bits.rawRs1.valid
  hazard.io.raw.decode.rs1.addr := decode.io.out.bits.rawRs1.addr
  hazard.io.raw.decode.rs2.valid := decode.io.out.valid && decode.io.out.bits.rawRs2.valid
  hazard.io.raw.decode.rs2.addr := decode.io.out.bits.rawRs2.addr

  hazard.io.raw.idExLoad.valid := idEx.io.deq.valid && idEx.io.deq.bits.wbCtrl.wen
  hazard.io.raw.idExLoad.addr := idEx.io.deq.bits.wbCtrl.rd
  val exMemLoad = Wire(new RAWRdPacket)
  exMemLoad.valid := exMem.io.deq.valid && exMem.io.deq.bits.memCtrl.en && !exMem.io.deq.bits.memCtrl.write
  exMemLoad.addr := exMem.io.deq.bits.wbCtrl.rd
  hazard.io.raw.lsuLoad.valid := lsu.io.pendingLoad.valid || exMemLoad.valid
  hazard.io.raw.lsuLoad.addr := Mux(lsu.io.pendingLoad.valid, lsu.io.pendingLoad.addr, exMemLoad.addr)
  hazard.io.raw.lsuToMemWbFire := lsu.io.pendingLoad.valid && lsu.io.out.fire

  val executeRedirect = execute.io.out.fire && execute.io.out.bits.ifRedct.redirect.valid
  hazard.io.ctrl.redirect := executeRedirect
  val redirectFlush = hazard.io.flush
  val loadUseStall = hazard.io.stall

  fetch.io.redirect.valid := redirectFlush
  fetch.io.redirect.bits := execute.io.out.bits.ifRedct.redirect.bits

  ifId.io.flush := redirectFlush
  idEx.io.flush := redirectFlush
  exMem.io.flush := false.B
  memWb.io.flush := false.B

  ifId.io.stall := false.B
  idEx.io.stall := loadUseStall
  exMem.io.stall := false.B
  memWb.io.stall := false.B

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
