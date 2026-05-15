package mycpu.core.frontend

import chisel3._
import chisel3.util._
import mycpu.common._
import mycpu.core.bundles._
import mycpu.utils._
import mycpu.core.components._

class Fetch(
    enableTraceFields: Boolean = ENABLE_TRACE_FIELDS,
    enableDpi: Boolean = false,
) extends Module {
  val io = IO(new Bundle {
    //val axi = new AXI4LiteBundle(XLEN, XLEN)
    val fetch = Decoupled(UInt(32.W))
    val reply = Flipped(Decoupled(UInt(32.W)))

    val out = Decoupled(new FetchPacket)
    val redirect = Input(Valid(UInt(XLEN.W)))
  })


//流水级最好不要留这个valid。让flushablestage来管理这个valid。
//流水级只知道valid-ready阻塞。flushablestage可以把valid reg数据valid翻译成backpressure valid

  io.out.valid := false.B
  io.fetch.valid := false.B
  io.reply.ready := false.B



//对于fetch：可能会发生：请求in-flight的情况：
//因此需要用epoch reg管理
// set in-flight max = 3
  val epoch = RegInit(false.B)
//当fetch fire的时候，记录本次epoch。当reply之后，比较epoch是否一致 
//这个方法仅适用单发射，不会连续发射的fetch
  val lastEpoch = RegInit(true.B)


  //内存只支持单in-flight，epoch是为了防止在flight途中被redirect，导致返回失效。
  //我们维护两个Queue：FetchReq和FetchReply。都使能flush就行。我们并不需要多个epoch位
  

  val fetchReq = Module(new Queue(XLenU, entries = 3, hasFlush = true))
  val fetchReply = Module(new Queue(new Bundle{val pc = XLenU; val inst = XLenU}, entries = 3, hasFlush = true))
  val pc = RegInit(START_ADDR.U(XLEN.W))
  val reqPc = RegInit(START_ADDR.U(XLEN.W))

  val outInst = fetchReply.io.deq.bits.inst
  val outPc = fetchReply.io.deq.bits.pc
  val replyIsJal = outInst(6, 0) === "b1101111".U
  val jalImm = Cat(Fill(11, outInst(31)), outInst(31), outInst(19, 12), outInst(20), outInst(30, 21), 0.U(1.W))
  val jalTarget = (outPc.asSInt + jalImm.asSInt).asUInt
  val jalRedirect = io.out.fire && replyIsJal

  when(io.redirect.valid) {
    pc := io.redirect.bits
  }.elsewhen(jalRedirect) {
    pc := jalTarget
  }.elsewhen(fetchReq.io.enq.fire) {
    pc := pc + 4.U
  }

  fetchReq.io.enq.valid := !reset.asBool && !io.redirect.valid && !jalRedirect
  fetchReq.io.enq.bits := pc
  io.fetch <> fetchReq.io.deq
  when (fetchReq.io.deq.fire) {
    reqPc := fetchReq.io.deq.bits
  }

  fetchReply.io.enq.valid := (epoch === lastEpoch) && io.reply.valid && !io.redirect.valid
  io.reply.ready := !reset.asBool && (fetchReply.io.enq.ready || (epoch =/= lastEpoch) || io.redirect.valid)
  fetchReply.io.enq.bits.pc := reqPc
  fetchReply.io.enq.bits.inst := io.reply.bits


  fetchReq.io.flush.get := io.redirect.valid || jalRedirect
  fetchReply.io.flush.get := io.redirect.valid || jalRedirect
  

  when (io.fetch.fire) {
    lastEpoch := epoch
  }

  when (io.redirect.valid) {
    epoch := !epoch
  }

  //当epoch不一致的时候，丢弃reply包
  //即来一次dry io.reply.ready：这意味着，此时io.out.valid为0,并耗费一个周期去拉高io.reply.ready 
  //只有新的reply inst返回时，才能发送
  
  //当epoch不一致，始终扔掉。当io.redirect.valid的时候，扔掉
  io.out.valid := fetchReply.io.deq.valid
  fetchReply.io.deq.ready := io.out.ready
  io.out.bits.pc := outPc
  io.out.bits.inst := outInst
  
  


  io.out.bits.isException := false.B


  // when (io.fetch.fire) {
  //   printf(p"fetch accept: $pc\n")
  // }
  // when (io.reply.fire) {
  //   printf(p"reply: ${io.reply.bits}\n")
  // }

  if (enableDpi) {
    val fetchTrace = Module(new FetchTrace)
    fetchTrace.io.clk := clock
    fetchTrace.io.reset := reset.asBool
    fetchTrace.io.reqInst := io.fetch.fire
    fetchTrace.io.gotReply := io.reply.fire
    fetchTrace.io.gotInst := io.out.fire
    fetchTrace.io.flush := io.redirect.valid || jalRedirect
    fetchTrace.io.pc := io.out.bits.pc
    fetchTrace.io.inst := io.out.bits.inst
  }

}
