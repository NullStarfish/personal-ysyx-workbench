package mycpu.core.components

import chisel3._
import chisel3.util._
import mycpu.common._
import mycpu.core.bundles._
import mycpu.utils._

class MemoryReadReq extends Bundle {
  val addr = XLenU
  val size = UInt(3.W)
}

class MemoryReadArbiter extends Module {
  val io = IO(new Bundle {
    val fetchReq = Flipped(Decoupled(UInt(XLEN.W)))
    val fetchReply = Decoupled(UInt(XLEN.W))

    val lsuReq = Flipped(Decoupled(new LsuReq))
    val lsuReply = Decoupled(UInt(XLEN.W))

    val outReq = Decoupled(new MemoryReadReq)
    val inReply = Flipped(Decoupled(UInt(XLEN.W)))
  })

  object Owner extends ChiselEnum {
    val None, Fetch, LSU = Value
  }

  val owner = RegInit(Owner.None)
  val fetchRespValid = RegInit(false.B)
  val fetchRespData = Reg(UInt(XLEN.W))
  val lsuRespValid = RegInit(false.B)
  val lsuRespData = Reg(UInt(XLEN.W))


  val enableTrace = true
  if (enableTrace) {
    when (io.fetchReq.valid) {
      printf("[Read ARB] fetch req\n")
    }
    when (io.lsuReq.valid) {
      printf("[Read ARB] LSU req\n")
    }
    when (io.fetchReq.valid || io.lsuReq.valid) {
      printf("Current Owner: %d\n", owner.asUInt)
    }

    when (io.fetchReply.fire) {
      printf("Fetch reply fire, data: %x\n", io.fetchReply.bits)
    }
    when (io.lsuReply.fire) {
      printf("LSU reply fire, data: %x\n", io.lsuReply.bits)
    }
    
  }

  val canGrantLsu = io.lsuReq.valid && !lsuRespValid
  val canGrantFetch = io.fetchReq.valid && !fetchRespValid
  val grantLsu = canGrantLsu
  val hasReq = canGrantLsu || canGrantFetch

  io.outReq.valid := owner === Owner.None && hasReq
  io.outReq.bits.addr := Mux(grantLsu, io.lsuReq.bits.addr, io.fetchReq.bits)
  io.outReq.bits.size := Mux(grantLsu, io.lsuReq.bits.size, 2.U)

  io.lsuReq.ready := owner === Owner.None && grantLsu && io.outReq.ready
  io.fetchReq.ready := owner === Owner.None && !grantLsu && canGrantFetch && io.outReq.ready

  io.fetchReply.valid := fetchRespValid
  io.fetchReply.bits := fetchRespData
  io.lsuReply.valid := lsuRespValid
  io.lsuReply.bits := lsuRespData
  io.inReply.ready := Mux(owner === Owner.Fetch, !fetchRespValid, Mux(owner === Owner.LSU, !lsuRespValid, false.B))

  when(io.outReq.fire) {
    owner := Mux(grantLsu, Owner.LSU, Owner.Fetch)
  }

  when(io.inReply.fire) {
    when(owner === Owner.Fetch) {
      fetchRespValid := true.B
      fetchRespData := io.inReply.bits
    }.elsewhen(owner === Owner.LSU) {
      lsuRespValid := true.B
      lsuRespData := io.inReply.bits
    }
    owner := Owner.None
  }

  when(io.fetchReply.fire) {
    fetchRespValid := false.B
  }

  when(io.lsuReply.fire) {
    lsuRespValid := false.B
  }
}

class MemoryController extends Module {
  val io = IO(new Bundle {
    val fetchReq = Flipped(Decoupled(UInt(XLEN.W)))
    val fetchReply = Decoupled(UInt(XLEN.W))

    val lsuReq = Flipped(Decoupled(new LsuReq))
    val lsuReply = Decoupled(UInt(XLEN.W))

    val axi = new AXI4Bundle(AXI_ID_WIDTH, XLEN, XLEN)
  })

  object WriteState extends ChiselEnum {
    val Idle, WaitResp = Value
  }

  private def setAddr(a: AXI4BundleA, addr: UInt, size: UInt): Unit = {
    a.id := 0.U
    a.addr := addr
    a.len := 0.U
    a.size := size
    a.burst := 0.U
    a.lock := false.B
    a.cache := AXI4Parameters.CACHE_DEVICE_NOBUF
    a.prot := 0.U
    a.qos := 0.U
  }

  val readArb = Module(new MemoryReadArbiter)
  val lsuReadReq = Wire(Decoupled(new LsuReq))
  val lsuReadReply = Wire(Decoupled(UInt(XLEN.W)))

  val writeState = RegInit(WriteState.Idle)
  val awDone = RegInit(false.B)
  val wDone = RegInit(false.B)

  io.axi.setAsMasterInit()

  readArb.io.fetchReq <> io.fetchReq
  io.fetchReply <> readArb.io.fetchReply
  readArb.io.lsuReq <> lsuReadReq

  lsuReadReq.valid := io.lsuReq.valid && !io.lsuReq.bits.write && writeState === WriteState.Idle
  lsuReadReq.bits := io.lsuReq.bits

  val writeActive = writeState === WriteState.Idle && io.lsuReq.valid && io.lsuReq.bits.write

  io.axi.ar.valid := readArb.io.outReq.valid
  setAddr(io.axi.ar.bits, readArb.io.outReq.bits.addr, readArb.io.outReq.bits.size)
  readArb.io.outReq.ready := io.axi.ar.ready

  readArb.io.inReply.valid := io.axi.r.valid
  readArb.io.inReply.bits := io.axi.r.bits.data
  io.axi.r.ready := readArb.io.inReply.ready

  val writeDone = Wire(Bool())
  writeDone := false.B

  when(writeActive) {
    io.axi.aw.valid := !awDone
    setAddr(io.axi.aw.bits, io.lsuReq.bits.addr, io.lsuReq.bits.size)
    io.axi.w.valid := !wDone
    io.axi.w.bits.data := io.lsuReq.bits.data
    io.axi.w.bits.strb := io.lsuReq.bits.strb
    io.axi.w.bits.last := true.B

    writeDone := (awDone || io.axi.aw.fire) && (wDone || io.axi.w.fire)

    when(io.axi.aw.fire) {
      awDone := true.B
    }
    when(io.axi.w.fire) {
      wDone := true.B
    }
    when(writeDone) {
      awDone := false.B
      wDone := false.B
      writeState := WriteState.WaitResp
    }
  }

  io.lsuReq.ready := Mux(io.lsuReq.bits.write, writeActive && writeDone, lsuReadReq.ready)

  val writeReplyValid = writeState === WriteState.WaitResp && io.axi.b.valid
  lsuReadReply <> readArb.io.lsuReply
  io.lsuReply.valid := lsuReadReply.valid || writeReplyValid
  io.lsuReply.bits := Mux(lsuReadReply.valid, lsuReadReply.bits, 0.U)
  lsuReadReply.ready := io.lsuReply.ready && lsuReadReply.valid
  io.axi.b.ready := io.lsuReply.ready && writeReplyValid && !lsuReadReply.valid

  when(writeReplyValid && io.lsuReply.ready && !lsuReadReply.valid) {
    writeState := WriteState.Idle
  }
  


  
}
