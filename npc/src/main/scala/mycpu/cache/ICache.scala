package mycpu.cache

import chisel3._
import chisel3.util._

class ICache(
    params: CacheParams = CacheParams(),
    enableDpi: Boolean = false,
) extends Module {
  val io = IO(new ICacheIO(params))

  val reqReg = Reg(chiselTypeOf(io.cpuReq.bits))
  val reqValid = RegInit(false.B)
  val responseResolved = RegInit(false.B)
  val responseHit = RegInit(false.B)
  val responseInst = Reg(UInt(params.dataWidth.W))
  val responseWay = Reg(UInt(params.wayWidth.W))

  object State extends ChiselEnum {
    val Lookup, Response = Value
  }
  val state = RegInit(State.Lookup)

  val lookupReq = Wire(chiselTypeOf(io.cpuReq.bits))
  lookupReq := Mux(reqValid, reqReg, io.cpuReq.bits)
  val cpuReq = reqReg

  val reqTag = params.tag(cpuReq.pc)
  val reqIndex = params.index(cpuReq.pc)
  val reqWordOffset = params.wordOffset(cpuReq.pc)
  val reqLineBase = params.lineBase(cpuReq.pc)
  val cacheSet = Module(new CacheSet(params))
  val replacement = Replacement(params)
  

  io.cpuReq.ready := false.B

  cacheSet.io.lookup.valid := false.B
  cacheSet.io.lookup.bits.index := reqIndex
  cacheSet.io.lookup.bits.tag := reqTag
  cacheSet.io.lookup.bits.wordOffset := reqWordOffset


  io.cpuReply.valid := false.B
  io.cpuReply.bits.pc := cpuReq.pc
  io.cpuReply.bits.inst := responseInst
  io.cpuReply.bits.hit := responseHit



  replacement.victimReq.set := reqIndex
  replacement.touch.valid := false.B
  replacement.touch.bits.set := reqIndex
  replacement.touch.bits.way := responseWay


  val refillBeat = RegInit(0.U(params.wordOffsetWidth.W))
  val refillLine = Reg(Vec(params.wordsPerLine, UInt(params.dataWidth.W)))

  val refillAddr = reqLineBase + (refillBeat << log2Ceil(params.wordBytes)).asUInt
  val refillLast = refillBeat === (params.wordsPerLine - 1).U


  io.memReq.valid := false.B
  io.memReq.bits.addr := refillAddr
  io.memReply.ready := false.B





  cacheSet.io.write.valid := false.B
  cacheSet.io.write.bits.index := reqIndex
  cacheSet.io.write.bits.way := replacement.victimResp.way
  cacheSet.io.write.bits.valid := true.B
  cacheSet.io.write.bits.meta.tag := reqTag
  cacheSet.io.write.bits.data := refillLine.asUInt

  val memReqDone = RegInit(false.B)
  val dropMemReply = RegInit(false.B)
  val accessLatency = RegInit(0.U(32.W))
  val accessMiss = RegInit(false.B)

  val hasLookupReq = reqValid || io.cpuReq.valid

  when(reqValid) {
    accessLatency := accessLatency + 1.U
  }

  when (state === State.Lookup) {
    io.cpuReq.ready := !reqValid && !reset.asBool && !io.redirect.valid && !dropMemReply

    cacheSet.io.lookup.valid := hasLookupReq && !reset.asBool && !io.redirect.valid && !dropMemReply
    cacheSet.io.lookup.bits.index := params.index(lookupReq.pc)
    cacheSet.io.lookup.bits.tag := params.tag(lookupReq.pc)
    cacheSet.io.lookup.bits.wordOffset := params.wordOffset(lookupReq.pc)

    when(io.cpuReq.fire) {
      reqReg := io.cpuReq.bits
      reqValid := true.B
      accessLatency := 0.U
      accessMiss := false.B
    }

    when(cacheSet.io.lookup.valid) {
      state := State.Response
      responseResolved := false.B
    }
  }

  when (state === State.Response) {
    when(!responseResolved) {
      responseResolved := true.B
      responseHit := cacheSet.io.lookupResp.hit
      responseInst := cacheSet.io.lookupResp.word
      responseWay := cacheSet.io.lookupResp.way
    }.elsewhen(responseHit) {
      io.cpuReply.valid := !io.redirect.valid
      when(io.cpuReply.fire) {
        replacement.touch.valid := true.B
        reqValid := false.B
        responseResolved := false.B
        accessLatency := 0.U
        accessMiss := false.B
        state := State.Lookup
      }
    }.otherwise {
      accessMiss := true.B
      io.memReq.valid := reqValid && !memReqDone && !io.redirect.valid && !dropMemReply
      io.memReply.ready := reqValid && !io.redirect.valid && !dropMemReply

      when(io.memReq.fire) {
        memReqDone := true.B
      }

      when(io.memReply.fire) {
        refillLine(refillBeat) := io.memReply.bits.data
        memReqDone := false.B
        when(refillLast) {
          val completedLine = Wire(Vec(params.wordsPerLine, UInt(params.dataWidth.W)))
          completedLine := refillLine
          completedLine(refillBeat) := io.memReply.bits.data

          cacheSet.io.write.valid := true.B
          cacheSet.io.write.bits.data := completedLine.asUInt
          replacement.touch.valid := true.B
          replacement.touch.bits.way := replacement.victimResp.way
          refillBeat := 0.U


          state := State.Lookup
          responseResolved := false.B

        }.otherwise {
          refillBeat := refillBeat + 1.U
        }
      }
    }
  }

  when(dropMemReply) {
    io.memReply.ready := !reset.asBool
    when(io.memReply.fire) {
      dropMemReply := false.B
    }
  }

  when(io.redirect.valid) {
    reqValid := false.B
    accessLatency := 0.U
    accessMiss := false.B
    refillBeat := 0.U
    responseResolved := false.B
    state := State.Lookup
    when(memReqDone) {
      memReqDone := false.B
      dropMemReply := true.B
    }
  }


  if (enableDpi) {
    val trace = Module(new ICacheTrace)
    trace.io.clk := clock
    trace.io.reset := reset.asBool
    trace.io.req := io.cpuReq.fire
    trace.io.reqPc := io.cpuReq.bits.pc
    trace.io.flush := io.redirect.valid && reqValid
    trace.io.hit := io.cpuReply.fire && !accessMiss
    // A completed refill changes cache state even if a later redirect discards its CPU reply.
    trace.io.miss := cacheSet.io.write.valid
    trace.io.resultPc := cpuReq.pc
    trace.io.selectedValid := cacheSet.io.lookupResp.selectedValid
    trace.io.storedTag := cacheSet.io.lookupResp.storedTag
    trace.io.latency := accessLatency + 1.U
  }


}

final class ICacheTrace extends BlackBox with HasBlackBoxInline {
  val io = IO(new Bundle {
    val clk = Input(Clock())
    val reset = Input(Bool())
    val req = Input(Bool())
    val reqPc = Input(UInt(32.W))
    val flush = Input(Bool())
    val hit = Input(Bool())
    val miss = Input(Bool())
    val resultPc = Input(UInt(32.W))
    val selectedValid = Input(Bool())
    val storedTag = Input(UInt(32.W))
    val latency = Input(UInt(32.W))
  })
  setInline(
    "ICacheTrace.sv",
    """module ICacheTrace(
      |    input logic clk,
      |    input logic reset,
      |    input logic req,
      |    input logic [31:0] reqPc,
      |    input logic flush,
      |    input logic hit,
      |    input logic miss,
      |    input logic [31:0] resultPc,
      |    input logic selectedValid,
      |    input logic [31:0] storedTag,
      |    input logic [31:0] latency
      |);
      | import "DPI-C" function void icache_req_trace(
      |   input int pc
      |);
      | import "DPI-C" function void icache_ref_flush(
      |   input bit flush
      |);
      | import "DPI-C" function void icache_trace(
      |   input bit hit,
      |   input bit miss,
      |   input int resultPc,
      |   input bit selectedValid,
      |   input int storedTag,
      |   input int latency
      |);
      |
      |always_ff @(posedge clk) begin
      | if(!reset) begin
      |   if(req) begin
      |     icache_req_trace(reqPc);
      |   end
      |   if(flush) begin
      |     icache_ref_flush(flush);
      |   end
      |   if(hit || miss) begin
      |     icache_trace(hit, miss, resultPc, selectedValid, storedTag, latency);
      |   end
      | end
      |end
      |
      |endmodule
      |""".stripMargin
  )
}
