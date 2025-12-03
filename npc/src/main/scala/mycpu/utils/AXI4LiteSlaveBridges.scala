package mycpu.utils

import chisel3._
import chisel3.util._
import mycpu.common._

// ==============================================================================
// Read Slave Bridge
// ==============================================================================
class AXI4LiteReadSlaveBridge(addrWidth: Int = XLEN, dataWidth: Int = XLEN) extends Module {
  val io = IO(new Bundle {
    // [Updated] 使用拆分后的 ReadOnly 接口 (idWidth 默认为 1)
    val axi = Flipped(new AXI4ReadOnly(idWidth = 1, addrWidth, dataWidth))
    
    // User Req/Resp
    val req  = Decoupled(new SimpleReadReq(addrWidth))
    val resp = Flipped(Decoupled(new SimpleReadBusResp(dataWidth)))
  })

  // 1. AR 通道缓冲 
  // [Updated] 存储整个 AXI4BundleA 以保留 ID
  val arQueue = Module(new Queue(new AXI4BundleA(1, addrWidth), entries = 1))
  
  arQueue.io.enq <> io.axi.ar
  
  // 2. 转换为 SimpleReq
  io.req.valid     := arQueue.io.deq.valid
  io.req.bits.addr := arQueue.io.deq.bits.addr
  
  // 只有当 Req 被用户接受，且 Response 通道空闲(或者准备好发送)时，才弹出 AR
  // 注意：这里是一个简化的流控，实际上要等 Resp 回来才能完全释放 AR 的 ID 上下文，
  // 但对于顺序执行的 Lite 模型，这种简化是可以的。
  // 更严谨的做法是：Resp 发送时才视为一次事务彻底结束。
  // 这里的逻辑：Req 发送成功就 deq，ID 需要暂存到 Resp 阶段。
  
  // 改进：使用一个小寄存器或队列保存正在处理的 ID
  val processingIdQueue = Module(new Queue(UInt(1.W), 1))
  
  // 当 req 发送成功时，将 ID 放入 processing 队列
  val reqFire = io.req.ready && arQueue.io.deq.valid
  arQueue.io.deq.ready := reqFire && processingIdQueue.io.enq.ready
  
  processingIdQueue.io.enq.valid := reqFire
  processingIdQueue.io.enq.bits  := arQueue.io.deq.bits.id

  // 3. R 通道处理
  io.axi.r.valid     := io.resp.valid
  io.axi.r.bits.data := io.resp.bits.rdata
  io.axi.r.bits.resp := Mux(io.resp.bits.isError, AXI4Parameters.RESP_SLVERR, AXI4Parameters.RESP_OKAY)
  // [Updated] AXI4 信号补充
  io.axi.r.bits.last := true.B // Lite 总是 Last
  io.axi.r.bits.id   := processingIdQueue.io.deq.bits // 返回对应的 ID
  
  // 握手连接
  val rFire = io.axi.r.ready && io.resp.valid
  io.resp.ready := io.axi.r.ready
  
  // 只有当 R 发送成功，才释放 ID
  processingIdQueue.io.deq.ready := rFire

  // Debug
  when(io.axi.ar.fire) { Debug.log("[AXIReadSlave] AR: addr=%x id=%x\n", io.axi.ar.bits.addr, io.axi.ar.bits.id) }
  when(io.axi.r.fire)  { Debug.log("[AXIReadSlave] R: data=%x resp=%x\n", io.axi.r.bits.data, io.axi.r.bits.resp) }
}

// ==============================================================================
// Write Slave Bridge
// ==============================================================================
class AXI4LiteWriteSlaveBridge(addrWidth: Int = XLEN, dataWidth: Int = XLEN) extends Module {
  val io = IO(new Bundle {
    // [Updated] 使用拆分后的 WriteOnly 接口
    val axi = Flipped(new AXI4WriteOnly(idWidth = 1, addrWidth, dataWidth))

    val req  = Decoupled(new SimpleWriteReq(addrWidth, dataWidth))
    val resp = Flipped(Decoupled(new SimpleWriteBusResp(dataWidth)))
  })

  // 1. 缓冲 AW 和 W
  val awQueue = Module(new Queue(new AXI4BundleA(1, addrWidth), entries = 2))
  val wQueue  = Module(new Queue(new AXI4BundleW(dataWidth), entries = 2))
  
  // 保存正在处理的 ID
  val processingIdQueue = Module(new Queue(UInt(1.W), 2))

  awQueue.io.enq <> io.axi.aw
  wQueue.io.enq  <> io.axi.w

  // 2. Join Logic
  val validReq = awQueue.io.deq.valid && wQueue.io.deq.valid && processingIdQueue.io.enq.ready

  io.req.valid      := validReq
  io.req.bits.addr  := awQueue.io.deq.bits.addr
  io.req.bits.wdata := wQueue.io.deq.bits.data
  io.req.bits.wstrb := wQueue.io.deq.bits.strb

  val reqFire = validReq && io.req.ready
  
  awQueue.io.deq.ready := reqFire
  wQueue.io.deq.ready  := reqFire
  
  // 保存 AW 的 ID 用于 B 通道回复
  processingIdQueue.io.enq.valid := reqFire
  processingIdQueue.io.enq.bits  := awQueue.io.deq.bits.id

  // 3. B 通道处理
  io.axi.b.valid     := io.resp.valid
  io.axi.b.bits.resp := Mux(io.resp.bits.isError, AXI4Parameters.RESP_SLVERR, AXI4Parameters.RESP_OKAY)
  io.axi.b.bits.id   := processingIdQueue.io.deq.bits // 返回 ID

  val bFire = io.axi.b.ready && io.resp.valid
  io.resp.ready := io.axi.b.ready
  
  // B 发送成功后释放 ID
  processingIdQueue.io.deq.ready := bFire

  when(io.req.fire)   { Debug.log("[AXIWriteSlave] Issue: addr=%x data=%x\n", io.req.bits.addr, io.req.bits.wdata) }
  when(io.axi.b.fire) { Debug.log("[AXIWriteSlave] B Sent: id=%x\n", io.axi.b.bits.id) }
}