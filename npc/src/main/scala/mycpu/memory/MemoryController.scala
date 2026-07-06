package mycpu.memory

import chisel3._
import chisel3.util._
import mycpu.common._
import mycpu.utils._

class MemoryReadArbiter extends Module {
  val io = IO(new Bundle {
    val fetch = Flipped(new MemReadIO)
    val lsuA = Flipped(Decoupled(new MemA))
    val lsuR = Decoupled(new MemR)

    val outA = Decoupled(new MemA)
    val inR = Flipped(Decoupled(new MemR))
  })

  object Owner extends ChiselEnum {
    val None, Fetch, LSU = Value
  }

  val owner = RegInit(Owner.None)
  val fetchRespValid = RegInit(false.B)
  val fetchResp = Reg(new MemR)
  val lsuRespValid = RegInit(false.B)
  val lsuResp = Reg(new MemR)

  val enableTrace = false
  if (enableTrace) {
    when(io.fetch.a.valid) {
      printf("[Read ARB] fetch req\n")
    }
    when(io.lsuA.valid) {
      printf("[Read ARB] LSU req\n")
    }
    when(io.fetch.r.fire) {
      printf("Fetch reply fire, data: %x\n", io.fetch.r.bits.data)
    }
    when(io.lsuR.fire) {
      printf("LSU reply fire, data: %x\n", io.lsuR.bits.data)
    }
  }

  val canGrantLsu = io.lsuA.valid && !lsuRespValid
  val canGrantFetch = io.fetch.a.valid && !fetchRespValid
  val grantLsu = canGrantLsu
  val hasReq = canGrantLsu || canGrantFetch

  io.outA.valid := owner === Owner.None && hasReq
  io.outA.bits := Mux(grantLsu, io.lsuA.bits, io.fetch.a.bits)

  io.lsuA.ready := owner === Owner.None && grantLsu && io.outA.ready
  io.fetch.a.ready := owner === Owner.None && !grantLsu && canGrantFetch && io.outA.ready

  io.fetch.r.valid := fetchRespValid
  io.fetch.r.bits := fetchResp
  io.lsuR.valid := lsuRespValid
  io.lsuR.bits := lsuResp
  io.inR.ready := Mux(owner === Owner.Fetch, !fetchRespValid, Mux(owner === Owner.LSU, !lsuRespValid, false.B))

  when(io.outA.fire) {
    owner := Mux(grantLsu, Owner.LSU, Owner.Fetch)
  }

  when(io.inR.fire) {
    when(owner === Owner.Fetch) {
      fetchRespValid := true.B
      fetchResp := io.inR.bits
    }.elsewhen(owner === Owner.LSU) {
      lsuRespValid := true.B
      lsuResp := io.inR.bits
    }
    when(io.inR.bits.last) {
      owner := Owner.None
    }
  }

  when(io.fetch.r.fire) {
    fetchRespValid := false.B
  }

  when(io.lsuR.fire) {
    lsuRespValid := false.B
  }
}

class MemoryController extends Module {
  val io = IO(new Bundle {
    val icache = Flipped(new MemReadIO)
    val lsu = Flipped(new MemIO)
    val axi = new AXI4Bundle(AXI_ID_WIDTH, XLEN, XLEN)
  })

  object WriteState extends ChiselEnum {
    val Idle, WaitResp = Value
  }

  private def setAddr(a: AXI4BundleA, req: MemA): Unit = {
    a.id := req.id
    a.addr := req.addr
    a.len := req.len
    a.size := req.size
    a.burst := Mux(req.len === 0.U, AXI4Parameters.BURST_FIXED, AXI4Parameters.BURST_INCR)
    a.lock := false.B
    a.cache := AXI4Parameters.CACHE_DEVICE_NOBUF
    a.prot := 0.U
    a.qos := 0.U
  }

  val readArb = Module(new MemoryReadArbiter)
  val writeState = RegInit(WriteState.Idle)
  val awDone = RegInit(false.B)
  val wDone = RegInit(false.B)

  io.axi.setAsMasterInit()

  readArb.io.fetch <> io.icache

  readArb.io.lsuA.valid := io.lsu.a.valid && !io.lsu.a.bits.write && writeState === WriteState.Idle
  readArb.io.lsuA.bits := io.lsu.a.bits

  val writeAActive = writeState === WriteState.Idle && io.lsu.a.valid && io.lsu.a.bits.write && !awDone
  val writeWActive = writeState === WriteState.Idle && io.lsu.w.valid && !wDone

  io.lsu.a.ready := Mux(io.lsu.a.bits.write, writeAActive && io.axi.aw.ready, readArb.io.lsuA.ready)
  io.lsu.w.ready := writeWActive && io.axi.w.ready

  io.axi.ar.valid := readArb.io.outA.valid
  setAddr(io.axi.ar.bits, readArb.io.outA.bits)
  readArb.io.outA.ready := io.axi.ar.ready

  readArb.io.inR.valid := io.axi.r.valid
  readArb.io.inR.bits.data := io.axi.r.bits.data
  readArb.io.inR.bits.resp := io.axi.r.bits.resp
  readArb.io.inR.bits.last := io.axi.r.bits.last
  readArb.io.inR.bits.id := io.axi.r.bits.id
  io.axi.r.ready := readArb.io.inR.ready

  io.axi.aw.valid := writeAActive
  setAddr(io.axi.aw.bits, io.lsu.a.bits)
  io.axi.w.valid := writeWActive
  io.axi.w.bits.data := io.lsu.w.bits.data
  io.axi.w.bits.strb := io.lsu.w.bits.strb
  io.axi.w.bits.last := io.lsu.w.bits.last

  when(io.axi.aw.fire) {
    awDone := true.B
  }
  when(io.axi.w.fire) {
    wDone := true.B
  }
  when((awDone || io.axi.aw.fire) && (wDone || io.axi.w.fire)) {
    awDone := false.B
    wDone := false.B
    writeState := WriteState.WaitResp
  }

  io.lsu.r <> readArb.io.lsuR

  io.lsu.b.valid := writeState === WriteState.WaitResp && io.axi.b.valid
  io.lsu.b.bits.resp := io.axi.b.bits.resp
  io.lsu.b.bits.id := io.axi.b.bits.id
  io.axi.b.ready := io.lsu.b.ready && io.lsu.b.valid

  when(io.lsu.b.fire) {
    writeState := WriteState.Idle
  }
}
