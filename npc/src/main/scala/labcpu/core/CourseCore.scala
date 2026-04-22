package labcpu.core

import chisel3._
import chisel3.util._
import labcpu.core.backend.WriteBack
import labcpu.core.bundles._
import labcpu.core.components.HazardUnit
import labcpu.core.frontend.Fetch
import mycpu.common._
import mycpu.core.backend.{Decode, Execute, ExecuteOperandSelect}
import mycpu.core.bundles._
import mycpu.core.components.{FlushableStage, Tracer}

class CourseCore(
    startAddr: BigInt = START_ADDR,
    enableDpi: Boolean = false,
    enableTracer: Boolean = ENABLE_TRACER,
    enableTraceFields: Boolean = ENABLE_TRACE_FIELDS,
) extends Module {
  val io = IO(new Bundle {
    val imem = new InstMemIO
    val dmem = new DataMemIO
    val debug_regs = Output(Vec(32, UInt(XLEN.W)))
    val debug_pc = Output(UInt(XLEN.W))
    val retire_valid = Output(Bool())
    val retire_pc = Output(UInt(XLEN.W))
    val retire_inst = Output(UInt(32.W))
    val trace = Output(new CoreTraceBundle)
  })

  val fetch = Module(new Fetch(startAddr, enableTraceFields = enableTraceFields))
  val decode = Module(new Decode(enableTraceFields = enableTraceFields))
  val operandSelect = Module(new ExecuteOperandSelect(enableTraceFields = enableTraceFields))
  val execute = Module(new Execute(enableTraceFields = enableTraceFields))
  val writeBack = Module(new WriteBack(enableTraceFields = enableTraceFields))
  val hazard = Module(new HazardUnit)
  val tracer = if (enableTracer && enableTraceFields) Some(Module(new Tracer(enableDpi = enableDpi))) else None

  val ifId = Module(new FlushableStage(new FetchPacket))
  val idEx = Module(new FlushableStage(new DecodePacket(enableTraceFields)))
  val exWb = Module(new FlushableStage(new ExecutePacket(enableTraceFields)))

  fetch.io.imem.rdata := io.imem.rdata
  io.imem.addr := fetch.io.imem.addr

  writeBack.io.in.valid := exWb.io.deq.valid
  writeBack.io.in.bits := exWb.io.deq.bits
  writeBack.io.dmemRdata := io.dmem.rdata

  decode.io.regWrite := writeBack.io.regWrite

  decode.io.bpUpdate := execute.io.bpUpdate

  decode.io.in.valid := ifId.io.deq.valid
  decode.io.in.bits := ifId.io.deq.bits
  decode.io.out.ready := idEx.io.enq.ready && !hazard.io.loadUseStall && !hazard.io.redirectFlush

  idEx.io.enq.valid := decode.io.out.valid && !hazard.io.loadUseStall && !hazard.io.redirectFlush
  idEx.io.enq.bits := decode.io.out.bits
  idEx.io.deq.ready := operandSelect.io.in.ready
  idEx.io.flush := hazard.io.redirectFlush

  operandSelect.io.exForward.valid := exWb.io.deq.valid
  operandSelect.io.exForward.bits := exWb.io.deq.bits
  operandSelect.io.memForward.valid := false.B
  operandSelect.io.memForward.bits := 0.U.asTypeOf(new MemoryPacket(enableTraceFields))
  operandSelect.io.in.valid := idEx.io.deq.valid
  operandSelect.io.in.bits := idEx.io.deq.bits

  execute.io.in.valid := operandSelect.io.out.valid
  execute.io.in.bits := operandSelect.io.out.bits
  operandSelect.io.out.ready := execute.io.in.ready
  execute.io.out.ready := exWb.io.enq.ready

  exWb.io.enq.valid := execute.io.out.valid
  exWb.io.enq.bits := execute.io.out.bits
  exWb.io.deq.ready := true.B
  exWb.io.flush := false.B

  val decodeFire = idEx.io.enq.fire
  val decodePredictedRedirect = decodeFire &&
    decode.io.out.bits.exec.family === ExecFamily.Branch &&
    decode.io.out.bits.pred.predictedTaken
  val decodePredictedTarget = decode.io.out.bits.data.pc + decode.io.out.bits.data.offset
  val fetchRedirectValid = hazard.io.redirectFlush || decodePredictedRedirect
  val fetchRedirectTarget = Mux(hazard.io.redirectFlush, execute.io.out.bits.redirect.bits, decodePredictedTarget)

  ifId.io.enq.valid := fetch.io.out.valid && !hazard.io.redirectFlush && !decodePredictedRedirect
  ifId.io.enq.bits := fetch.io.out.bits
  ifId.io.deq.ready := decode.io.in.ready
  ifId.io.flush := hazard.io.redirectFlush || decodePredictedRedirect
  fetch.io.out.ready := ifId.io.enq.ready && !hazard.io.redirectFlush && !decodePredictedRedirect

  hazard.io.decodeRs1Used := decode.io.hazard.rs1Used && ifId.io.deq.valid
  hazard.io.decodeRs2Used := decode.io.hazard.rs2Used && ifId.io.deq.valid
  hazard.io.decodeRs1Addr := decode.io.hazard.rs1Addr
  hazard.io.decodeRs2Addr := decode.io.hazard.rs2Addr
  hazard.io.idLoadValid := idEx.io.deq.valid && idEx.io.deq.bits.mem.valid && !idEx.io.deq.bits.mem.write
  hazard.io.idLoadRd := idEx.io.deq.bits.wb.rd
  hazard.io.exFire := exWb.io.enq.fire
  hazard.io.exRedirectValid := execute.io.out.bits.redirect.valid

  fetch.io.stall := !ifId.io.enq.ready || hazard.io.loadUseStall
  fetch.io.redirect.valid := fetchRedirectValid
  fetch.io.redirect.bits := fetchRedirectTarget

  io.dmem.addr := exWb.io.deq.bits.result
  io.dmem.ren := exWb.io.deq.valid && exWb.io.deq.bits.mem.valid && !exWb.io.deq.bits.mem.write
  io.dmem.wen := exWb.io.deq.valid && exWb.io.deq.bits.mem.valid && exWb.io.deq.bits.mem.write
  io.dmem.subop := exWb.io.deq.bits.mem.subop
  io.dmem.unsigned := exWb.io.deq.bits.mem.unsigned
  io.dmem.wdata := exWb.io.deq.bits.rhs

  io.debug_regs := decode.io.debug_regs
  io.debug_pc := fetch.io.debug_pc
  if (enableTraceFields) {
    io.retire_valid := writeBack.io.traceCommit.get.valid
    io.retire_pc := writeBack.io.traceCommit.get.bits.pc
    io.retire_inst := writeBack.io.traceCommit.get.bits.inst
  } else {
    io.retire_valid := false.B
    io.retire_pc := 0.U
    io.retire_inst := 0.U
  }

  val regsFlat = Cat(io.debug_regs.reverse)
  if (enableTracer && enableTraceFields) {
    val tracerMod = tracer.get
    tracerMod.io.commitTrace <> writeBack.io.traceCommit.get
    tracerMod.io.regsFlat := regsFlat
    tracerMod.io.mtvec := execute.io.debug_csrs.mtvec
    tracerMod.io.mepc := execute.io.debug_csrs.mepc
    tracerMod.io.mstatus := execute.io.debug_csrs.mstatus
    tracerMod.io.mcause := execute.io.debug_csrs.mcause
    io.trace := tracerMod.io.trace
  } else {
    io.trace := 0.U.asTypeOf(new CoreTraceBundle)
  }
}
