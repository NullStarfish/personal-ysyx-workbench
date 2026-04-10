package mycpu.core

import chisel3._
import mycpu.common._
import mycpu.core.backend._
import mycpu.core.bundles._
import mycpu.core.components.{FlushableStage, HazardUnit}
import mycpu.core.frontend.Fetch
import mycpu.utils._

class Core extends Module {
  val io = IO(new Bundle {
    val master = new AXI4Bundle(idWidth = AXI_ID_WIDTH, addrWidth = XLEN, dataWidth = XLEN)
    val debug_regs = Output(Vec(32, UInt(XLEN.W)))
  })

  val fetch = Module(new Fetch)
  val decode = Module(new Decode)
  val execute = Module(new Execute)
  val lsu = Module(new LSU)
  val writeBack = Module(new WriteBack)
  val hazard = Module(new HazardUnit)
  val ifId = Module(new FlushableStage(new FetchPacket))
  val idEx = Module(new FlushableStage(new DecodePacket))
  val exMem = Module(new FlushableStage(new ExecutePacket))
  val memWb = Module(new FlushableStage(new MemoryPacket))
  val arbiter = Module(new SimpleAXIArbiter)

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

  val exForward = Wire(new ForwardInfo)
  exForward.valid := exMem.io.deq.valid && exMem.io.deq.bits.wb.regWen && !exMem.io.deq.bits.mem.valid && (exMem.io.deq.bits.wb.rd =/= 0.U)
  exForward.addr := exMem.io.deq.bits.wb.rd
  exForward.data := exMem.io.deq.bits.result

  val memForward = Wire(new ForwardInfo)
  memForward.valid := memWb.io.deq.valid && memWb.io.deq.bits.wb.regWen && (memWb.io.deq.bits.wb.rd =/= 0.U)
  memForward.addr := memWb.io.deq.bits.wb.rd
  memForward.data := memWb.io.deq.bits.wbData

  decode.io.exForward := exForward
  decode.io.memForward := memForward

  decode.io.in.valid := ifId.io.deq.valid
  decode.io.in.bits := ifId.io.deq.bits

  execute.io.in.valid := idEx.io.deq.valid
  execute.io.in.bits := idEx.io.deq.bits

  lsu.io.in.valid := exMem.io.deq.valid
  lsu.io.in.bits := exMem.io.deq.bits
  lsu.io.out.ready := memWb.io.enq.ready

  execute.io.out.ready := exMem.io.enq.ready
  exMem.io.enq.valid := execute.io.out.valid
  exMem.io.enq.bits := execute.io.out.bits
  val exFire = exMem.io.enq.fire

  hazard.io.decodeInst := Mux(ifId.io.deq.valid, ifId.io.deq.bits.inst, 0.U)
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
  idEx.io.enq.valid := decode.io.out.valid
  idEx.io.enq.bits := decode.io.out.bits
  val decodeFire = idEx.io.enq.fire
  idEx.io.deq.ready := execute.io.in.ready

  fetch.io.out.ready := ifId.io.enq.ready && !redirectFlush
  ifId.io.enq.valid := fetch.io.out.valid
  ifId.io.enq.bits := fetch.io.out.bits
  ifId.io.deq.ready := decode.io.in.ready
  fetch.io.ctrl.stall := !ifId.io.enq.ready
  fetch.io.ctrl.redirect.valid := redirectFlush
  fetch.io.ctrl.redirect.bits := execute.io.out.bits.redirect.bits

  memWb.io.enq.valid := lsu.io.out.valid
  memWb.io.enq.bits := lsu.io.out.bits

  ifId.io.flush := redirectFlush
  idEx.io.flush := redirectFlush
  exMem.io.deq.ready := lsu.io.in.ready
}
