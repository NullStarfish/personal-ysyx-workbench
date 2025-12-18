package mycpu.utils

import chisel3._
import chisel3.util._
import mycpu._
import mycpu.common._
import dataclass.data

// ==============================================================================
// Read Bridge
// ==============================================================================
class AXI4ReadBridge(addrWidth: Int = XLEN, dataWidth: Int = XLEN) extends Module {
  val io = IO(new Bundle {
    val rReq = Flipped(Decoupled(new AXI4BundleA(addrWidth, dataWidth)))
    val rStream = Decoupled(new AXI4BundleR(AXI_ID_WIDTH, dataWidth))

    val axi = new AXI4ReadOnly(idWidth = AXI_ID_WIDTH, addrWidth, dataWidth)
  })
  //两个Queue，作为req的fifo和dataStream的fifo
  val arQueue = Module(new Queue(new AXI4BundleA(AXI_ID_WIDTH, addrWidth), entries = 4))
  val rQueue  = Module(new Queue(new AXI4BundleR(AXI_ID_WIDTH, dataWidth), entries = 4))

  arQueue.io.enq <> io.rReq
  io.axi.ar <> arQueue.io.deq
  

  
  io.rStream <> rQueue.io.deq
  rQueue.io.enq <> io.axi.r 
  


  when(io.axi.ar.fire) { 
    Debug.log("[DEBUG] [ReadBridge] AR Sent: ID=%x Addr=%x Size = %x Len=%x\n", io.axi.ar.bits.id, io.axi.ar.bits.addr, io.axi.ar.bits.size, io.axi.ar.bits.len) 
  }
  when(io.axi.r.fire) { 
    Debug.log("[DEBUG] [ReadBridge] R Recv: ID=%x Data=%x Last=%d\n", io.axi.r.bits.id, io.axi.r.bits.data, io.axi.r.bits.last) 
  }

}

// ==============================================================================
// Write Bridge
// ==============================================================================
class AXI4WriteBridge(addrWidth: Int = XLEN, dataWidth: Int = XLEN) extends Module {
  val io = IO(new Bundle{
    val wReq = Flipped(Decoupled(new AXI4BundleA(AXI_ID_WIDTH, addrWidth)))
    val wStream = Flipped(Decoupled(new AXI4BundleW(dataWidth)))
    val bResp = Decoupled(new AXI4BundleB(dataWidth))
    val axi = new AXI4WriteOnly(idWidth = AXI_ID_WIDTH, addrWidth, dataWidth)
  })
  val awQueue = Module(new Queue(new AXI4BundleA(AXI_ID_WIDTH, addrWidth), entries = 4))
  val wQueue  = Module(new Queue(new AXI4BundleW(addrWidth), entries = 4))
  val bQueue = Module(new Queue(new AXI4BundleB(dataWidth), entries = 4))

  awQueue.io.enq <> io.wReq
  io.axi.aw <> awQueue.io.deq

  wQueue.io.enq <> io.wStream
  io.axi.w <> wQueue.io.deq

  io.bResp <> bQueue.io.deq
  bQueue.io.enq <> io.axi.b

  // Debug Log
  when(io.axi.aw.fire) { Debug.log("[WriteBridge] AW Sent: ID=%x Addr=%x\n", io.axi.aw.bits.id, io.axi.aw.bits.addr) }
  when(io.axi.w.fire)  { Debug.log("[WriteBridge] W Sent: Data=%x Last=%d\n", io.axi.w.bits.data, io.axi.w.bits.last) }
  when(io.axi.b.fire)  { Debug.log("[WriteBridge] B Recv: ID=%x Resp=%x\n", io.axi.b.bits.id, io.axi.b.bits.resp) }

}