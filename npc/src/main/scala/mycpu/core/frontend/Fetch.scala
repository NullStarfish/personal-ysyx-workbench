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



  val pc = RegInit(START_ADDR.U(XLEN.W))
  when (io.redirect.valid){
    pc := io.redirect.bits
  } .elsewhen(io.fetch.fire) {
    pc := pc + 4.U //默认不跳转
  }

  val shooted = RegInit(false.B)

  io.fetch.valid := !reset.asBool && !io.redirect.valid && !shooted
  io.fetch.bits := pc
//对于fetch：可能会发生：请求in-flight的情况：
//因此需要用epoch reg管理
  val epoch = RegInit(false.B)
//当fetch fire的时候，记录本次epoch。当reply之后，比较epoch是否一致 
//这个方法仅适用单发射，不会连续发射的fetch
  val lastEpoch = RegInit(true.B)
  val launchedPc = RegInit(START_ADDR.U(XLEN.W))

  when (io.fetch.fire) {
    lastEpoch := epoch
    launchedPc := pc
    shooted := true.B
  }

  when (io.reply.fire) {
    shooted := false.B
  }

  when (io.redirect.valid) {
    epoch := !epoch
  }

  //当epoch不一致的时候，丢弃reply包
  //即来一次dry io.reply.ready：这意味着，此时io.out.valid为0,并耗费一个周期去拉高io.reply.ready 
  //只有新的reply inst返回时，才能发送
  
  io.reply.ready := !reset.asBool && (io.out.ready || (epoch =/= lastEpoch) && io.redirect.valid)
  //当epoch不一致，始终扔掉。当io.redirect.valid的时候，扔掉
  io.out.valid := (epoch === lastEpoch) && io.reply.valid  && !io.redirect.valid
  io.out.bits.pc := launchedPc
  io.out.bits.inst := io.reply.bits
  
  


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
    fetchTrace.io.gotInst := io.out.fire
    fetchTrace.io.pc := io.out.bits.pc
    fetchTrace.io.inst := io.out.bits.inst
  }

}
