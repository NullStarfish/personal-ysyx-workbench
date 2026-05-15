package mycpu.cache

import chisel3._
import chisel3.util._

class ICache(
    params: CacheParams = CacheParams(),
    enableDpi: Boolean = false,
) extends Module {
  val io = IO(new ICacheIO(params))

  object State extends ChiselEnum {
    val Idle, Lookup, RefillReq, RefillResp = Value
  }

  val state = RegInit(State.Idle)
  val reqReg = Reg(new ICacheCpuReq(params))
  val abortRefill = RegInit(false.B)
  val refillBeat = RegInit(0.U(params.wordOffsetBits.W))
  val refillLine = Reg(Vec(params.wordsPerLine, UInt(params.dataBits.W)))
  val refillVictimWay = Reg(UInt(params.wayBits.W))

  val reqQ = Module(new Queue(new ICacheCpuReq(params), entries = 8, flow = true, hasFlush = true))
  val replyQ = Module(new Queue(new ICacheCpuReply(params), entries = 8, flow = true, hasFlush = true))
  val cacheSet = Module(new CacheSet(params))
  val replacement = Replacement(params)

  val reqTag = params.tag(reqReg.pc)
  val reqIndex = params.index(reqReg.pc)
  val reqWordOffset = params.wordOffset(reqReg.pc)
  val reqLineBase = params.lineBase(reqReg.pc)
  val refillAddr = reqLineBase + (refillBeat << log2Ceil(params.bytesPerWord)).asUInt
  val refillLast = refillBeat === (params.wordsPerLine - 1).U

  reqQ.io.enq.valid := io.cpuReq.valid && !reset.asBool && !io.redirect.valid
  reqQ.io.enq.bits := io.cpuReq.bits
  reqQ.io.flush.get := io.redirect.valid
  io.cpuReq.ready := reqQ.io.enq.ready && !reset.asBool && !io.redirect.valid

  io.cpuReply <> replyQ.io.deq
  replyQ.io.flush.get := io.redirect.valid
  replyQ.io.enq.valid := false.B
  replyQ.io.enq.bits.pc := reqReg.pc
  replyQ.io.enq.bits.inst := cacheSet.io.lookupResp.word
  replyQ.io.enq.bits.hit := cacheSet.io.lookupResp.hit

  io.memReq.valid := state === State.RefillReq
  io.memReq.bits.addr := refillAddr
  io.memReply.ready := state === State.RefillResp

  cacheSet.io.lookup.valid := state === State.Lookup
  cacheSet.io.lookup.bits.index := reqIndex
  cacheSet.io.lookup.bits.tag := reqTag
  cacheSet.io.lookup.bits.wordOffset := reqWordOffset

  cacheSet.io.write.valid := false.B
  cacheSet.io.write.bits.index := reqIndex
  cacheSet.io.write.bits.way := refillVictimWay
  cacheSet.io.write.bits.meta.valid := true.B
  cacheSet.io.write.bits.meta.tag := reqTag
  cacheSet.io.write.bits.data := refillLine.asUInt

  replacement.victimReq.set := reqIndex
  replacement.touch.valid := false.B
  replacement.touch.bits.set := reqIndex
  replacement.touch.bits.way := cacheSet.io.lookupResp.way

  reqQ.io.deq.ready := state === State.Idle && replyQ.io.enq.ready && !io.redirect.valid
  when(reqQ.io.deq.fire) {
    reqReg := reqQ.io.deq.bits
    state := State.Lookup
  }

  when(state === State.Lookup) {
    when(cacheSet.io.lookupResp.hit) {
      replyQ.io.enq.valid := true.B
      when(replyQ.io.enq.fire) {
        replacement.touch.valid := true.B
        replacement.touch.bits.way := cacheSet.io.lookupResp.way
        state := State.Idle
      }
    }.otherwise {
      refillBeat := 0.U
      refillVictimWay := replacement.victimResp.way
      state := State.RefillReq
    }
  }

  if (enableDpi) {
    val trace = Module(new ICacheTrace)
    trace.io.clk := clock
    trace.io.reset := reset.asBool
    trace.io.hit := state === State.Lookup && cacheSet.io.lookupResp.hit && replyQ.io.enq.fire
    trace.io.miss := state === State.Lookup && !cacheSet.io.lookupResp.hit
  }

  when(state === State.RefillReq && io.memReq.fire) {
    state := State.RefillResp
  }

  when(state === State.RefillResp && io.memReply.fire) {
    when(abortRefill) {
      abortRefill := false.B
      state := State.Idle
    }.otherwise {
      refillLine(refillBeat) := io.memReply.bits.data
      when(refillLast) {
        val completedLine = Wire(Vec(params.wordsPerLine, UInt(params.dataBits.W)))
        completedLine := refillLine
        completedLine(refillBeat) := io.memReply.bits.data

        cacheSet.io.write.valid := true.B
        cacheSet.io.write.bits.data := completedLine.asUInt
        replacement.touch.valid := true.B
        replacement.touch.bits.way := refillVictimWay
        state := State.Lookup
      }.otherwise {
        refillBeat := refillBeat + 1.U
        state := State.RefillReq
      }
    }
  }

  when(io.redirect.valid) {
    switch(state) {
      is(State.Lookup) {
        state := State.Idle
      }
      is(State.RefillReq) {
        when(io.memReq.fire) {
          abortRefill := true.B
          state := State.RefillResp
        }.otherwise {
          state := State.Idle
        }
      }
      is(State.RefillResp) {
        abortRefill := true.B
      }
    }
  }
}

final class ICacheTrace extends BlackBox with HasBlackBoxInline {
  val io = IO(new Bundle {
    val clk = Input(Clock())
    val reset = Input(Bool())
    val hit = Input(Bool())
    val miss = Input(Bool())
  })
  setInline(
    "ICacheTrace.sv",
    """module ICacheTrace(
      |    input logic clk,
      |    input logic reset,
      |    input logic hit,
      |    input logic miss
      |);
      | import "DPI-C" function void icache_trace(
      |   input bit hit,
      |   input bit miss
      |);
      |
      |always_ff @(posedge clk) begin
      | if(!reset && (hit || miss)) begin
      |   icache_trace(hit, miss);
      | end
      |end
      |
      |endmodule
      |""".stripMargin
  )
}
