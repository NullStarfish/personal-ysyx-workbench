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



  replacement.victimReq.set := reqIndex
  replacement.touch.valid := false.B
  replacement.touch.bits.set := reqIndex
  replacement.touch.bits.way := responseWay


  val refillBeat = RegInit(0.U(params.wordOffsetWidth.W))
  val dropBeat = RegInit(0.U(params.wordOffsetWidth.W))
  val refillLine = Reg(Vec(params.wordsPerLine, UInt(params.dataWidth.W)))

  val refillLast = refillBeat === (params.wordsPerLine - 1).U
  val dropLast = dropBeat === (params.wordsPerLine - 1).U


  io.memReq.valid := false.B
  io.memReq.bits.addr := reqLineBase
  io.memReq.bits.size := 2.U
  io.memReq.bits.beats := params.wordsPerLine.U
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

  io.cpuReply.bits.hit := responseHit && !accessMiss

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
          memReqDone := false.B

        }.otherwise {
          refillBeat := refillBeat + 1.U
        }
      }
    }
  }

  when(dropMemReply) {
    io.memReply.ready := !reset.asBool
    when(io.memReply.fire) {
      when(dropLast) {
        dropMemReply := false.B
        dropBeat := 0.U
      }.otherwise {
        dropBeat := dropBeat + 1.U
      }
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
      dropBeat := refillBeat
    }
  }


  if (enableDpi) {
    val hit = io.cpuReply.fire && !accessMiss
    // A completed refill changes cache state even if a later redirect discards its CPU reply.
    val miss = cacheSet.io.write.valid

    val accessTrace = Module(new ICacheAccessTrace)
    accessTrace.io.clk := clock
    accessTrace.io.reset := reset.asBool
    accessTrace.io.hit := hit
    accessTrace.io.miss := miss
    accessTrace.io.latency := accessLatency + 1.U
  }


}
