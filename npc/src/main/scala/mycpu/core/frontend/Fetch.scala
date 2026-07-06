package mycpu.core.frontend

import chisel3._
import chisel3.util._
import mycpu.common._
import mycpu.core.bundles._
import mycpu.memory.FetchResp
import mycpu.utils._

class Fetch(
    enableTraceFields: Boolean = ENABLE_TRACE_FIELDS,
    enableDpi: Boolean = false,
) extends Module {
  val io = IO(new Bundle {
    //val axi = new AXI4LiteBundle(XLEN, XLEN)
    val instReq = Decoupled(UInt(32.W))
    val instResp = Flipped(Decoupled(new FetchResp))

    val out = Decoupled(new FetchPacket)
    val redirect = Input(Valid(UInt(XLEN.W)))
  })


//流水级最好不要留这个valid。让flushablestage来管理这个valid。
//流水级只知道valid-ready阻塞。flushablestage可以把valid reg数据valid翻译成backpressure valid

  io.out.valid := false.B
  io.instReq.valid := false.B
  io.instResp.ready := false.B



//对于fetch：可能会发生：请求in-flight的情况：
//因此需要用epoch reg管理
// set in-flight max = 3
  val epoch = RegInit(0.U(2.W))

  //内存只支持单in-flight，epoch是为了防止在flight途中被redirect，导致返回失效。
  //我们维护两个Queue：FetchReq和FetchReply。都使能flush就行。我们并不需要多个epoch位
  

  val fetchReq = Module(new Queue(XLenU, entries = 3, hasFlush = true))
  val fetchReply = Module(new Queue(new Bundle{val pc = XLenU; val inst = XLenU; val icacheHit = Bool()}, entries = 3, hasFlush = true))
  val pc = RegInit(START_ADDR.U(XLEN.W))
  val reqPc = Module(new Queue(XLenU, entries = 3, hasFlush = true))


  val outInst = fetchReply.io.deq.bits.inst
  val outPc = fetchReply.io.deq.bits.pc
  val frontFlush = io.redirect.valid

  when(io.redirect.valid) {
    pc := io.redirect.bits
  }.elsewhen(fetchReq.io.enq.fire) {
    pc := pc + 4.U
  }

  fetchReq.io.enq.valid := !reset.asBool && !frontFlush
  fetchReq.io.enq.bits := pc
  io.instReq.valid := fetchReq.io.deq.valid && reqPc.io.enq.ready && !frontFlush
  io.instReq.bits := fetchReq.io.deq.bits
  fetchReq.io.deq.ready := io.instReq.ready && reqPc.io.enq.ready && !frontFlush


  reqPc.io.enq.valid := io.instReq.fire && !frontFlush
  reqPc.io.enq.bits := fetchReq.io.deq.bits


  fetchReply.io.enq.valid := io.instResp.valid && reqPc.io.deq.valid && !frontFlush
  io.instResp.ready := false.B

  when (!reset.asBool) {
    when (io.redirect.valid) {
      io.instResp.ready := true.B
    } .otherwise {
      when (!reqPc.io.deq.valid) {
        io.instResp.ready := true.B
      }.otherwise {
        //唯一不丢弃的路径
        io.instResp.ready := fetchReply.io.enq.ready
      }
    }
  }

  reqPc.io.deq.ready := io.instResp.fire

  fetchReply.io.enq.bits.pc := reqPc.io.deq.bits
  fetchReply.io.enq.bits.inst := io.instResp.bits.inst
  fetchReply.io.enq.bits.icacheHit := io.instResp.bits.hit


  fetchReq.io.flush.get := frontFlush
  fetchReply.io.flush.get := frontFlush
  reqPc.io.flush.get := frontFlush
  

  when (frontFlush) {
    epoch := epoch + 1.U
  }

  //当epoch不一致的时候，丢弃reply包
  //即来一次dry io.reply.ready：这意味着，此时io.out.valid为0,并耗费一个周期去拉高io.reply.ready 
  //只有新的reply inst返回时，才能发送
  
  //当epoch不一致，始终扔掉。当io.redirect.valid的时候，扔掉
  io.out.valid := fetchReply.io.deq.valid
  fetchReply.io.deq.ready := io.out.ready
  io.out.bits.pc := outPc
  io.out.bits.inst := outInst
  io.out.bits.icacheHit := fetchReply.io.deq.bits.icacheHit
  
  


  io.out.bits.isException := false.B


  // when (io.instReq.fire) {
  //   printf(p"fetch accept: $pc\n")
  // }
  // when (io.instResp.fire) {
  //   printf(p"reply: ${io.instResp.bits.inst}\n")
  // }

  if (enableDpi) {
    val fetchTrace = Module(new FetchTrace)
    fetchTrace.io.clk := clock
    fetchTrace.io.reset := reset.asBool
    fetchTrace.io.reqInst := io.instReq.fire
    fetchTrace.io.gotReply := io.instResp.fire
    fetchTrace.io.gotInst := io.out.fire
    fetchTrace.io.flush := frontFlush
    fetchTrace.io.reqBlocked := io.instReq.valid && !io.instReq.ready
    fetchTrace.io.outBlocked := io.out.valid && !io.out.ready
    fetchTrace.io.pc := io.out.bits.pc
    fetchTrace.io.inst := io.out.bits.inst
  }

}
