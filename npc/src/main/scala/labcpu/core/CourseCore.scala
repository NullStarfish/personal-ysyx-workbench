package labcpu.core

import chisel3._
import chisel3.util._
import labcpu.core.backend.WriteBack
import labcpu.core.bundles._
import labcpu.core.frontend.Fetch
import mycpu.common._
import mycpu.core.backend.{Decode, Execute}
import mycpu.core.bundles._
import mycpu.core.components.{FlushableStage, HazardUnit, Tracer}

class CourseCore(startAddr: BigInt = START_ADDR, enableDpi: Boolean = false) extends Module {
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

  val fetch = Module(new Fetch(startAddr))
  val decode = Module(new Decode)
  val execute = Module(new Execute)
  val writeBack = Module(new WriteBack)
  val hazard = Module(new HazardUnit)
  val tracer = Module(new Tracer(enableDpi = enableDpi))

  val ifId = Module(new FlushableStage(new FetchPacket))
  val idEx = Module(new FlushableStage(new DecodePacket))
  val exMem = Module(new FlushableStage(new ExecutePacket))

  fetch.io.imem.rdata := io.imem.rdata
  io.imem.addr := fetch.io.imem.addr

  writeBack.io.in.valid := exMem.io.deq.valid
  writeBack.io.in.bits := exMem.io.deq.bits
  writeBack.io.dmemRdata := io.dmem.rdata

  decode.io.regWrite := writeBack.io.regWrite

  val exForward = Wire(new ForwardInfo)
  exForward.valid := idEx.io.deq.valid && idEx.io.deq.bits.wb.regWen && (idEx.io.deq.bits.wb.rd =/= 0.U) &&
    !(idEx.io.deq.bits.mem.valid && !idEx.io.deq.bits.mem.write)
  exForward.addr := idEx.io.deq.bits.wb.rd
  exForward.data := execute.io.out.bits.result

  val memForward = Wire(new ForwardInfo)
  memForward.valid := exMem.io.deq.valid && exMem.io.deq.bits.wb.regWen && (exMem.io.deq.bits.wb.rd =/= 0.U)
  memForward.addr := exMem.io.deq.bits.wb.rd
  memForward.data := writeBack.io.out.wbData

  decode.io.exForward := exForward
  decode.io.memForward := memForward
  decode.io.bpUpdate := execute.io.bpUpdate

  decode.io.in.valid := ifId.io.deq.valid
  decode.io.in.bits := ifId.io.deq.bits
  decode.io.out.ready := idEx.io.enq.ready && !hazard.io.loadUseStall && !hazard.io.redirectFlush

  idEx.io.enq.valid := decode.io.out.valid && !hazard.io.loadUseStall && !hazard.io.redirectFlush
  idEx.io.enq.bits := decode.io.out.bits
  idEx.io.deq.ready := execute.io.in.ready
  idEx.io.flush := hazard.io.redirectFlush

  execute.io.in.valid := idEx.io.deq.valid
  execute.io.in.bits := idEx.io.deq.bits
  execute.io.out.ready := exMem.io.enq.ready

  exMem.io.enq.valid := execute.io.out.valid
  exMem.io.enq.bits := execute.io.out.bits
  exMem.io.deq.ready := true.B
  exMem.io.flush := false.B

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

  hazard.io.decodeInst := Mux(ifId.io.deq.valid, ifId.io.deq.bits.inst, 0.U)
  hazard.io.idLoadValid := idEx.io.deq.valid && idEx.io.deq.bits.mem.valid && !idEx.io.deq.bits.mem.write
  hazard.io.idLoadRd := idEx.io.deq.bits.wb.rd
  hazard.io.exLoadValid := exMem.io.deq.valid && exMem.io.deq.bits.mem.valid && !exMem.io.deq.bits.mem.write
  hazard.io.exLoadRd := exMem.io.deq.bits.wb.rd
  hazard.io.memPendingLoad := false.B
  hazard.io.memPendingRd := 0.U
  hazard.io.exFire := exMem.io.enq.fire
  hazard.io.exRedirectValid := execute.io.out.bits.redirect.valid

  fetch.io.stall := !ifId.io.enq.ready || hazard.io.loadUseStall
  fetch.io.redirect.valid := fetchRedirectValid
  fetch.io.redirect.bits := fetchRedirectTarget

  io.dmem.addr := exMem.io.deq.bits.result
  io.dmem.ren := exMem.io.deq.valid && exMem.io.deq.bits.mem.valid && !exMem.io.deq.bits.mem.write
  io.dmem.wen := exMem.io.deq.valid && exMem.io.deq.bits.mem.valid && exMem.io.deq.bits.mem.write
  io.dmem.subop := exMem.io.deq.bits.mem.subop
  io.dmem.unsigned := exMem.io.deq.bits.mem.unsigned
  io.dmem.wdata := exMem.io.deq.bits.rhs

  io.debug_regs := decode.io.debug_regs
  io.debug_pc := fetch.io.debug_pc
  io.retire_valid := writeBack.io.retire.valid
  io.retire_pc := writeBack.io.retire.pc
  io.retire_inst := writeBack.io.retire.inst

  val regsFlat = Cat(io.debug_regs.reverse)
  tracer.io.ifValid := ifId.io.deq.valid
  tracer.io.idValid := idEx.io.deq.valid
  tracer.io.exValid := exMem.io.deq.valid
  tracer.io.memValid := exMem.io.deq.valid
  tracer.io.retire := writeBack.io.retire
  tracer.io.branchResolved := execute.io.bpUpdate.valid
  tracer.io.branchCorrect := execute.io.bpUpdate.actualTaken === execute.io.bpUpdate.predictedTaken
  tracer.io.regsFlat := regsFlat
  tracer.io.mtvec := execute.io.debug_csrs.mtvec
  tracer.io.mepc := execute.io.debug_csrs.mepc
  tracer.io.mstatus := execute.io.debug_csrs.mstatus
  tracer.io.mcause := execute.io.debug_csrs.mcause
  io.trace := tracer.io.trace
}
