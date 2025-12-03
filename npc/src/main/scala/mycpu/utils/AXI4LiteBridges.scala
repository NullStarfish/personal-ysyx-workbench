package mycpu.utils

import chisel3._
import chisel3.util._
import mycpu.common._

// ==============================================================================
// Read Bridge
// ==============================================================================
class AXI4LiteReadBridge(addrWidth: Int = XLEN, dataWidth: Int = XLEN) extends Module {
  // 计算 Size (比如 32bit -> 2^2=4 bytes -> size=2)
  private val sizeConst = log2Ceil(dataWidth / 8).U(3.W)

  val io = IO(new Bundle {
    val req  = Flipped(Decoupled(new SimpleReadReq(addrWidth)))
    val resp = Decoupled(new SimpleReadBusResp(dataWidth))
    // 使用拆分后的 ReadOnly Bundle
    val axi  = new AXI4ReadOnly(idWidth = 1, addrWidth, dataWidth)
  })

  object State extends ChiselEnum { val sIdle, sReadAddr, sReadData = Value }
  val state = RegInit(State.sIdle)
  val addrReg = Reg(UInt(addrWidth.W))

  val respQueue = Module(new Queue(new SimpleReadBusResp(dataWidth), 1))
  io.resp <> respQueue.io.deq

  // ---------------------------------------------
  // Default Outputs (AXI Full Logic for Lite)
  // ---------------------------------------------
  io.req.ready           := false.B
  
  // AR Channel Defaults
  io.axi.ar.valid        := false.B
  io.axi.ar.bits.id      := 0.U
  io.axi.ar.bits.addr    := addrReg
  io.axi.ar.bits.len     := 0.U                      // Burst Length = 1
  io.axi.ar.bits.size    := sizeConst                // Full Width
  io.axi.ar.bits.burst   := AXI4Parameters.BURST_INCR
  io.axi.ar.bits.lock    := false.B
  io.axi.ar.bits.cache   := AXI4Parameters.CACHE_DEVICE_NOBUF
  io.axi.ar.bits.prot    := AXI4Parameters.PROT_PRIVILEGED
  io.axi.ar.bits.qos     := 0.U

  // R Channel Defaults
  io.axi.r.ready         := false.B
  
  respQueue.io.enq.valid := false.B
  respQueue.io.enq.bits  := DontCare

  // Debug Prints
  when(io.req.fire) { Debug.log("[AXIRead] Req: addr=%x\n", io.req.bits.addr) }
  when(io.axi.ar.fire) { Debug.log("[AXIRead] AR Sent\n") }
  when(io.resp.fire) { Debug.log("[AXIRead] Resp Done: data=%x\n", io.resp.bits.rdata) }

  // ---------------------------------------------
  // FSM
  // ---------------------------------------------
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
      // 优化：如果地址握手且数据没来，进 sReadData；如果同时来，直接完成
      // 这里支持 outstanding 但逻辑简单化处理
      io.axi.r.ready := true.B // 允许 AR 和 R 在同一拍 (极少数 Slave 支持，但协议允许)

      when(io.axi.ar.fire) { 
        when(io.axi.r.fire) {
          // Address accepted AND Data arrived same cycle
          respQueue.io.enq.valid        := true.B
          respQueue.io.enq.bits.rdata   := io.axi.r.bits.data
          respQueue.io.enq.bits.isError := (io.axi.r.bits.resp =/= AXI4Parameters.RESP_OKAY)
          when(respQueue.io.enq.ready) { state := State.sIdle }
        }.otherwise {
          state := State.sReadData
        }
      }
    }
    is(State.sReadData) {
      io.axi.r.ready := true.B
      when(io.axi.r.fire) {
        respQueue.io.enq.valid        := true.B
        respQueue.io.enq.bits.rdata   := io.axi.r.bits.data
        respQueue.io.enq.bits.isError := (io.axi.r.bits.resp =/= AXI4Parameters.RESP_OKAY)
        when(respQueue.io.enq.ready) { state := State.sIdle }
      }
    }
  }
}

// ==============================================================================
// Write Bridge
// ==============================================================================
class AXI4LiteWriteBridge(addrWidth: Int = XLEN, dataWidth: Int = XLEN) extends Module {
  private val sizeConst = log2Ceil(dataWidth / 8).U(3.W)

  val io = IO(new Bundle {
    val req  = Flipped(Decoupled(new SimpleWriteReq(addrWidth, dataWidth)))
    val resp = Decoupled(new SimpleWriteBusResp(dataWidth))
    val axi  = new AXI4WriteOnly(idWidth = 1, addrWidth, dataWidth)
  })

  object State extends ChiselEnum { val sIdle, sIssue, sWaitAXIResp, sReplyLSU = Value }
  val state = RegInit(State.sIdle)
  val reqReg = Reg(new SimpleWriteReq(addrWidth, dataWidth))
  val awSent = RegInit(false.B)
  val wSent  = RegInit(false.B)
  val bError = Reg(Bool())

  // ---------------------------------------------
  // Default Outputs
  // ---------------------------------------------
  io.req.ready        := false.B
  io.resp.valid       := false.B
  io.resp.bits.isError := bError
  
  // AW Channel
  io.axi.aw.valid     := false.B
  io.axi.aw.bits.id   := 0.U
  io.axi.aw.bits.addr := reqReg.addr
  io.axi.aw.bits.len  := 0.U
  io.axi.aw.bits.size := sizeConst
  io.axi.aw.bits.burst:= AXI4Parameters.BURST_INCR
  io.axi.aw.bits.lock := false.B
  io.axi.aw.bits.cache:= AXI4Parameters.CACHE_DEVICE_NOBUF
  io.axi.aw.bits.prot := AXI4Parameters.PROT_PRIVILEGED
  io.axi.aw.bits.qos  := 0.U
  
  // W Channel
  io.axi.w.valid      := false.B
  io.axi.w.bits.data  := reqReg.wdata
  io.axi.w.bits.strb  := reqReg.wstrb
  io.axi.w.bits.last  := true.B // Lite 总是 Last
  
  // B Channel
  io.axi.b.ready      := false.B

  when(io.req.fire) { Debug.log("[AXIWrite] Req: addr=%x data=%x\n", io.req.bits.addr, io.req.bits.wdata) }
  when(io.axi.b.fire) { Debug.log("[AXIWrite] B Resp received\n") }

  // ---------------------------------------------
  // FSM
  // ---------------------------------------------
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
      // 并行发送 AW 和 W
      when(!awSent) { io.axi.aw.valid := true.B }
      when(io.axi.aw.fire) { awSent := true.B }
      
      when(!wSent) { io.axi.w.valid := true.B }
      when(io.axi.w.fire) { wSent := true.B }

      when((awSent || io.axi.aw.fire) && (wSent || io.axi.w.fire)) {
        state := State.sWaitAXIResp
      }
    }
    is(State.sWaitAXIResp) {
      io.axi.b.ready := true.B
      when(io.axi.b.fire) {
        bError := (io.axi.b.bits.resp =/= AXI4Parameters.RESP_OKAY)
        state  := State.sReplyLSU
      }
    }
    is(State.sReplyLSU) {
      io.resp.valid := true.B
      when(io.resp.ready) {
        state := State.sIdle
      }
    }
  }
}