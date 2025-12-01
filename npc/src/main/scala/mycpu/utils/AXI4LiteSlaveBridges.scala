package mycpu.utils

import chisel3._
import chisel3.util._
import mycpu.common._

// ==============================================================================
// Read Slave Bridge
// 功能：将 AXI4Lite 从机接口转换为简单的 Req/Resp 握手
// 流程：AXI AR -> User Req -> User Logic -> User Resp -> AXI R
// ==============================================================================
class AXI4LiteReadSlaveBridge(addrWidth: Int = XLEN, dataWidth: Int = XLEN) extends Module {
  val io = IO(new Bundle {
    // AXI Slave 接口 (输入)
    val axi = Flipped(new AXI4LiteReadBundle(addrWidth, dataWidth))
    
    // 用户逻辑接口 (下游)
    // Req: 输出给用户逻辑的地址请求
    val req  = Decoupled(new SimpleReadReq(addrWidth))
    // Resp: 从用户逻辑接收的数据响应
    val resp = Flipped(Decoupled(new SimpleReadBusResp(dataWidth)))
  })

  // 1. AR 通道缓冲 (解耦 + 时序优化)
  // 只有当 AR 握手成功，且下游准备好时，请求才会传递下去
  val arQueue = Module(new Queue(new AXIAddress(addrWidth), entries = 1))
  
  arQueue.io.enq <> io.axi.ar
  
  // 2. 将 AR 转换为 SimpleReadReq 发送给用户
  io.req.valid     := arQueue.io.deq.valid
  io.req.bits.addr := arQueue.io.deq.bits.addr
  arQueue.io.deq.ready := io.req.ready

  // 3. R 通道处理
  // 直接对接用户的 Resp，并映射错误码
  // AXI Spec: R 必须在 AR 之后，但这里我们完全依赖用户逻辑的响应顺序
  io.axi.r.valid     := io.resp.valid
  io.axi.r.bits.data := io.resp.bits.rdata
  // 如果 isError 为 true，返回 SLVERR (2'b10)，否则返回 OKAY (2'b00)
  io.axi.r.bits.resp := Mux(io.resp.bits.isError, 2.U, 0.U) 
  io.resp.ready      := io.axi.r.ready

  // Debug Prints
  when(io.axi.ar.fire) {
    Debug.log("[DEBUG] [AXIReadSlave] AR received: addr=%x\n", io.axi.ar.bits.addr)
  }
  when(io.axi.r.fire) {
    Debug.log("[DEBUG] [AXIReadSlave] R sent: data=%x err=%d\n", io.axi.r.bits.data, io.axi.r.bits.resp)
  }
}

// ==============================================================================
// Write Slave Bridge
// 功能：将 AXI4Lite 从机接口转换为简单的 Req/Resp 握手
// 特性：支持 AW 和 W 通道的“乱序”到达（通过队列自动对齐）
// ==============================================================================
class AXI4LiteWriteSlaveBridge(addrWidth: Int = XLEN, dataWidth: Int = XLEN) extends Module {
  val io = IO(new Bundle {
    // AXI Slave 接口 (输入)
    val axi = Flipped(new AXI4LiteWriteBundle(addrWidth, dataWidth))

    // 用户逻辑接口 (下游)
    // Req: 同时包含地址和数据，只有当两者都齐备时才 valid
    val req  = Decoupled(new SimpleWriteReq(addrWidth, dataWidth))
    // Resp: 从用户逻辑接收写完成信号 (包含错误位)
    val resp = Flipped(Decoupled(new SimpleWriteBusResp(dataWidth)))
  })

  // 1. 独立的 AW 和 W 队列
  // 这允许 Master 先发 W 后发 AW，或者同时发送，Bridge 都能接住
  val awQueue = Module(new Queue(new AXIAddress(addrWidth), entries = 2)) // 深度可调
  val wQueue  = Module(new Queue(new AXIWriteData(dataWidth), entries = 2))

  awQueue.io.enq <> io.axi.aw
  wQueue.io.enq  <> io.axi.w

  // 2. 合并逻辑 (Join)
  // 只有当两个队列都有数据时，才向用户发起请求
  val validReq = awQueue.io.deq.valid && wQueue.io.deq.valid

  io.req.valid      := validReq
  io.req.bits.addr  := awQueue.io.deq.bits.addr
  io.req.bits.wdata := wQueue.io.deq.bits.data
  io.req.bits.wstrb := wQueue.io.deq.bits.strb

  // 只有当用户接收了请求 (io.req.fire)，我们才从两个队列中弹出数据
  awQueue.io.deq.ready := validReq && io.req.ready
  wQueue.io.deq.ready  := validReq && io.req.ready

  // 3. B 通道处理 (写响应)
  // 等待用户逻辑处理完毕并返回 Resp
  io.axi.b.valid     := io.resp.valid
  io.axi.b.bits.resp := Mux(io.resp.bits.isError, 2.U, 0.U) // 0: OKAY, 2: SLVERR
  io.resp.ready      := io.axi.b.ready

  // Debug Prints
  when(io.axi.aw.fire) {
    Debug.log("[DEBUG] [AXIWriteSlave] AW received: addr=%x\n", io.axi.aw.bits.addr)
  }
  when(io.axi.w.fire) {
    Debug.log("[DEBUG] [AXIWriteSlave] W received: data=%x strb=%x\n", io.axi.w.bits.data, io.axi.w.bits.strb)
  }
  when(io.req.fire) {
    Debug.log("[DEBUG] [AXIWriteSlave] Issue to User: addr=%x data=%x\n", io.req.bits.addr, io.req.bits.wdata)
  }
  when(io.axi.b.fire) {
    Debug.log("[DEBUG] [AXIWriteSlave] B sent: resp=%x\n", io.axi.b.bits.resp)
  }
}