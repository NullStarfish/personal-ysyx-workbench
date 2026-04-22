package mycpu.core

import chisel3._
import chisel3.util.Cat
import mycpu.common._
import mycpu.core.backend._
import mycpu.core.bundles._
import mycpu.core.components.{FlushableStage, HazardUnit, Tracer}
import mycpu.core.frontend.Fetch
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
    val trace = Output(new CoreTraceBundle)
  })

  val fetch = Module(new Fetch(enableTraceFields = enableTraceFields))
  val decode = Module(new Decode(enableTraceFields = enableTraceFields))
  val operandSelect = Module(new ExecuteOperandSelect(enableTraceFields = enableTraceFields))
  val execute = Module(new Execute(enableTraceFields = enableTraceFields))
  val lsu = Module(new LSU(enableTraceFields = enableTraceFields))
  val writeBack = Module(new WriteBack(enableTraceFields = enableTraceFields))
  val hazard = Module(new HazardUnit)
  val ifId = Module(new FlushableStage(new FetchPacket))
  val idEx = Module(new FlushableStage(new DecodePacket(enableTraceFields)))
  val exMem = Module(new FlushableStage(new ExecutePacket(enableTraceFields)))
  val memWb = Module(new FlushableStage(new MemoryPacket(enableTraceFields)))
  val arbiter = Module(new SimpleAXIArbiter)
  val tracer = if (enableTracer && enableTraceFields) Some(Module(new Tracer(enableDpi = enableDpi))) else None

  arbiter.io.left <> fetch.io.axi
  arbiter.io.right <> lsu.io.axi
  io.master <> arbiter.io.out

  ifId.io.flush := false.B
  idEx.io.flush := false.B
  exMem.io.flush := false.B
  memWb.io.flush := false.B

  writeBack.io.in.valid := memWb.io.deq.valid
  writeBack.io.in.bits := memWb.io.deq.bits
  memWb.io.deq.ready := writeBack.io.in.ready

  decode.io.regWrite <> writeBack.io.regWrite
  io.debug_regs := decode.io.debug_regs
  io.debug_csrs.mtvec := execute.io.debug_csrs.mtvec
  io.debug_csrs.mepc := execute.io.debug_csrs.mepc
  io.debug_csrs.mstatus := execute.io.debug_csrs.mstatus
  io.debug_csrs.mcause := execute.io.debug_csrs.mcause
  val regsFlat = Cat(io.debug_regs.reverse)

  decode.io.bpUpdate := execute.io.bpUpdate

  decode.io.in.valid := ifId.io.deq.valid
  decode.io.in.bits := ifId.io.deq.bits

  operandSelect.io.exForward.valid := exMem.io.deq.valid
  operandSelect.io.exForward.bits := exMem.io.deq.bits
  operandSelect.io.memForward.valid := memWb.io.deq.valid
  operandSelect.io.memForward.bits := memWb.io.deq.bits

  operandSelect.io.in.valid := idEx.io.deq.valid
  operandSelect.io.in.bits := idEx.io.deq.bits

  execute.io.in.valid := operandSelect.io.out.valid
  execute.io.in.bits := operandSelect.io.out.bits
  operandSelect.io.out.ready := execute.io.in.ready

  lsu.io.in.valid := exMem.io.deq.valid
  lsu.io.in.bits := exMem.io.deq.bits
  lsu.io.out.ready := memWb.io.enq.ready

  execute.io.out.ready := exMem.io.enq.ready
  exMem.io.enq.valid := execute.io.out.valid
  exMem.io.enq.bits := execute.io.out.bits
  val exFire = exMem.io.enq.fire

  hazard.io.decodeInst := Mux(ifId.io.deq.valid, ifId.io.deq.bits.inst, 0.U)
  hazard.io.idWriteValid := false.B
  hazard.io.idWriteRd := 0.U
  hazard.io.idLoadValid := idEx.io.deq.valid && idEx.io.deq.bits.mem.valid && !idEx.io.deq.bits.mem.write
  hazard.io.idLoadRd := idEx.io.deq.bits.wb.rd
  hazard.io.exLoadValid := exMem.io.deq.valid && exMem.io.deq.bits.mem.valid && !exMem.io.deq.bits.mem.write
  hazard.io.exLoadRd := exMem.io.deq.bits.wb.rd
  hazard.io.memPendingLoad := lsu.io.status.pendingLoad
  hazard.io.memPendingRd := lsu.io.status.pendingRd
  hazard.io.exFire := exFire
  hazard.io.exRedirectValid := execute.io.out.bits.redirect.valid

  val loadUseStall = hazard.io.loadUseStall
  val redirectFlush = hazard.io.redirectFlush

  decode.io.out.ready := idEx.io.enq.ready && !loadUseStall && !redirectFlush
  idEx.io.enq.valid := decode.io.out.valid && !loadUseStall && !redirectFlush
  idEx.io.enq.bits := decode.io.out.bits
  val decodeFire = idEx.io.enq.fire
  val decodePredictedRedirect = decodeFire &&
    decode.io.out.bits.exec.family === ExecFamily.Branch &&
    decode.io.out.bits.pred.predictedTaken
  val decodePredictedTarget = decode.io.out.bits.data.pc + decode.io.out.bits.data.offset
  val fetchRedirectValid = redirectFlush || decodePredictedRedirect
  val fetchRedirectTarget = Mux(redirectFlush, execute.io.out.bits.redirect.bits, decodePredictedTarget)
  idEx.io.deq.ready := operandSelect.io.in.ready

  fetch.io.out.ready := ifId.io.enq.ready && !redirectFlush
  ifId.io.enq.valid := fetch.io.out.valid && !redirectFlush
  ifId.io.enq.bits := fetch.io.out.bits
  ifId.io.deq.ready := decode.io.in.ready
  fetch.io.ctrl.stall := !ifId.io.enq.ready
  fetch.io.ctrl.redirect.valid := fetchRedirectValid
  fetch.io.ctrl.redirect.bits := fetchRedirectTarget

  memWb.io.enq.valid := lsu.io.out.valid
  memWb.io.enq.bits := lsu.io.out.bits

  if (enableTracer && enableTraceFields) {
    val tracerMod = tracer.get
    tracerMod.io.commitTrace <> writeBack.io.traceCommit.get
    tracerMod.io.regsFlat := regsFlat
    tracerMod.io.mtvec := io.debug_csrs.mtvec
    tracerMod.io.mepc := io.debug_csrs.mepc
    tracerMod.io.mstatus := io.debug_csrs.mstatus
    tracerMod.io.mcause := io.debug_csrs.mcause
    io.trace := tracerMod.io.trace
  } else {
    io.trace := 0.U.asTypeOf(new CoreTraceBundle)
  }

  ifId.io.flush := redirectFlush
  idEx.io.flush := redirectFlush
  exMem.io.deq.ready := lsu.io.in.ready
}
