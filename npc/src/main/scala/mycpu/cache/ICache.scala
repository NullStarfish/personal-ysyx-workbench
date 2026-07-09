package mycpu.cache

import chisel3._
import chisel3.util._

class ICache(
    params: CacheParams = CacheParams(),
    enableDpi: Boolean = false,
) extends Module {
  val io = IO(new ICacheIO(params))

  //cpu req. 打一拍
  val reqReg = Reg(chiselTypeOf(io.cpuReq.bits))
  val reqValid = RegInit(false.B)


  //response reg
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


//方便访问的变量
  val reqTag = params.tag(cpuReq)
  val reqIndex = params.index(cpuReq)
  val reqWordOffset = params.wordOffset(cpuReq)
  //lineBase是该行在物理地址中映射的起始地址
  val reqLineBase = params.lineBase(cpuReq)



  val cacheSet = Module(new CacheSet(params))
  val replacement = Replacement(params)
  val fencei = io.fencei
  

  io.cpuReq.ready := false.B

  cacheSet.io.flush := fencei
  cacheSet.io.lookup.valid := false.B
  cacheSet.io.lookup.bits.index := reqIndex
  cacheSet.io.lookup.bits.tag := reqTag
  cacheSet.io.lookup.bits.wordOffset := reqWordOffset


  io.cpuReply.valid := false.B
  io.cpuReply.bits.inst := responseInst



  replacement.victimReq.set := reqIndex
  replacement.touch.valid := false.B
  replacement.touch.bits.set := reqIndex
  replacement.touch.bits.way := responseWay


  val refillBeat = RegInit(0.U(params.wordOffsetWidth.W))
  val refillLine = Reg(Vec(params.wordsPerLine, UInt(params.dataWidth.W)))


  io.mem.a.valid := false.B
  io.mem.a.bits.addr := reqLineBase
  io.mem.a.bits.size := 2.U
  io.mem.a.bits.len := (params.wordsPerLine - 1).U
  io.mem.a.bits.write := false.B
  io.mem.a.bits.id := 0.U
  io.mem.r.ready := false.B





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
    io.cpuReq.ready := !reqValid && !reset.asBool && !fencei && !dropMemReply

    cacheSet.io.lookup.valid := hasLookupReq && !reset.asBool && !fencei && !dropMemReply
    cacheSet.io.lookup.bits.index := params.index(lookupReq)
    cacheSet.io.lookup.bits.tag := params.tag(lookupReq)
    cacheSet.io.lookup.bits.wordOffset := params.wordOffset(lookupReq)

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
      io.cpuReply.valid := !fencei
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
      io.mem.a.valid := reqValid && !memReqDone && !fencei && !dropMemReply
      io.mem.r.ready := reqValid && !fencei && !dropMemReply

      when(io.mem.a.fire) {
        memReqDone := true.B
      }

      when(io.mem.r.fire) {
        refillLine(refillBeat) := io.mem.r.bits.data
        when(io.mem.r.bits.last) {
          val completedLine = Wire(Vec(params.wordsPerLine, UInt(params.dataWidth.W)))
          completedLine := refillLine
          completedLine(refillBeat) := io.mem.r.bits.data

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
    io.mem.r.ready := !reset.asBool
    when(io.mem.r.fire) {
      when(io.mem.r.bits.last) {
        dropMemReply := false.B
      }
    }
  }

  when(fencei) {
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
