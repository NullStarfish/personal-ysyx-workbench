package mycpu.utils

import chisel3._
import chisel3.util._
import mycpu.common._
import mycpu.utils._


// ==============================================================================
// Read Bridge (使用 AXI4LiteReadBundle)
// ==============================================================================
class AXI4LiteReadBridge(addrWidth: Int = XLEN, dataWidth: Int = XLEN) extends Module {
  val io = IO(new Bundle {
    val req  = Flipped(Decoupled(new SimpleReadReq(addrWidth)))
    val resp = Decoupled(new SimpleBusResp(dataWidth))
    // [关键] 只有读通道，没有多余引脚
    val axi  = new AXI4LiteReadBundle(addrWidth, dataWidth)
  })

  object State extends ChiselEnum { val sIdle, sReadAddr, sReadData = Value }
  val state = RegInit(State.sIdle)
  val addrReg = Reg(UInt(addrWidth.W))

  val respQueue = Module(new Queue(new SimpleBusResp(dataWidth), 1))
  io.resp <> respQueue.io.deq

  // Default Outputs
  io.req.ready           := false.B
  io.axi.ar.valid        := false.B
  io.axi.ar.bits.addr    := addrReg
  io.axi.ar.bits.prot    := 0.U
  io.axi.r.ready         := false.B
  respQueue.io.enq.valid := false.B
  respQueue.io.enq.bits  := DontCare

  switch(state) {
    is(State.sIdle) {
      io.req.ready := true.B
      when(io.req.fire) {
        addrReg := io.req.bits.addr
        state   := State.sReadAddr
      }
    }
    is(State.sReadAddr) {
      io.axi.ar.valid := true.B
      when(io.axi.ar.fire) { state := State.sReadData }
    }
    is(State.sReadData) {
      io.axi.r.ready := true.B
      when(io.axi.r.valid) {
        respQueue.io.enq.valid      := true.B
        respQueue.io.enq.bits.rdata := io.axi.r.bits.data
        respQueue.io.enq.bits.isError := (io.axi.r.bits.resp =/= 0.U)
        when(respQueue.io.enq.ready) { state := State.sIdle }
      }
    }
  }
}

// ==============================================================================
// Write Bridge (使用 AXI4LiteWriteBundle)
// ==============================================================================
class AXI4LiteWriteBridge(addrWidth: Int = XLEN, dataWidth: Int = XLEN) extends Module {
  val io = IO(new Bundle {
    val req  = Flipped(Decoupled(new SimpleWriteReq(addrWidth, dataWidth)))
    val resp = Decoupled(Bool()) // True = Error
    // [关键] 只有写通道
    val axi  = new AXI4LiteWriteBundle(addrWidth, dataWidth)
  })

  object State extends ChiselEnum { val sIdle, sIssue, sWaitResp = Value }
  val state = RegInit(State.sIdle)
  val reqReg = Reg(new SimpleWriteReq(addrWidth, dataWidth))
  val awSent = RegInit(false.B)
  val wSent  = RegInit(false.B)

  // Default Outputs
  io.req.ready        := false.B
  io.resp.valid       := false.B
  io.resp.bits        := false.B
  io.axi.aw.valid     := false.B
  io.axi.aw.bits.addr := reqReg.addr
  io.axi.aw.bits.prot := 0.U
  io.axi.w.valid      := false.B
  io.axi.w.bits.data  := reqReg.wdata
  io.axi.w.bits.strb  := reqReg.wstrb
  io.axi.b.ready      := false.B

  switch(state) {
    is(State.sIdle) {
      io.req.ready := true.B
      when(io.req.fire) {
        reqReg := io.req.bits
        awSent := false.B; wSent := false.B
        state  := State.sIssue
      }
    }
    is(State.sIssue) {
      when(!awSent) { io.axi.aw.valid := true.B }
      when(io.axi.aw.fire) { awSent := true.B }
      
      when(!wSent) { io.axi.w.valid := true.B }
      when(io.axi.w.fire) { wSent := true.B }

      when((awSent || io.axi.aw.fire) && (wSent || io.axi.w.fire)) {
        state := State.sWaitResp
      }
    }
    is(State.sWaitResp) {
      io.axi.b.ready := true.B
      when(io.axi.b.fire) {
        io.resp.valid := true.B
        io.resp.bits  := (io.axi.b.bits.resp =/= 0.U)
        when(io.resp.ready) { state := State.sIdle }
      }
    }
  }
}