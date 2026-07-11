package mycpu.core.frontend

import chisel3._
import chisel3.util._
import mycpu.cache._
import mycpu.common.XLenU
import mycpu.core.bundles.{FetchPacket, IFPacket}
import mycpu.memory.MemReadIO

class I$0Packet(params: CacheParams) extends Bundle {
  val pc = XLenU
  val icacheResp = new CacheSetLookupResp(params)
}

class I$1Packet extends Bundle {
  val pc = XLenU
  val inst = XLenU
}

class I$0Stage(params: CacheParams = CacheConfigs.SimpICache) extends Module {
  val io = IO(new Bundle {
    val in = Flipped(Decoupled(new IFPacket))
    val cacheSetResp = Input(new CacheSetLookupResp(params))
    val out = Decoupled(new I$0Packet(params))

    val flush = Input(Bool())
    val blockFetch = Output(Bool())
  })

  val inputPacket = Wire(new I$0Packet(params))
  inputPacket.pc := io.in.bits.pc
  inputPacket.icacheResp := io.cacheSetResp

  // Fetch/I$0由Core中的FlushableStage持有。skid只接住来不及停止的固定延迟返回。
  val skidValid = RegInit(false.B)
  val skidPacket = Reg(new I$0Packet(params))

  io.out.valid := !io.flush && (skidValid || io.in.valid)
  io.out.bits := Mux(skidValid, skidPacket, inputPacket)

  // 没有skid时，输入总会被直接发送或在本拍写入skid，因此必须真正deq。
  io.in.ready := !reset.asBool && !io.flush && !skidValid

  val captureSkid = io.in.fire && !io.out.ready
  val drainSkid = skidValid && io.out.fire

  when(io.flush) {
    skidValid := false.B
  }.elsewhen(captureSkid) {
    skidValid := true.B
    skidPacket := inputPacket
  }.elsewhen(drainSkid) {
    skidValid := false.B
  }

  // skid吸收固定延迟返回，block只表达普通Decoupled backpressure。
  io.blockFetch := io.out.valid && !io.out.ready
}

class I$1Stage(params: CacheParams = CacheConfigs.SimpICache) extends Module {
  val io = IO(new Bundle {
    val in = Flipped(Decoupled(new I$0Packet(params)))
    val out = Decoupled(new I$1Packet)
    val cacheSetWrite = Valid(new CacheSetWriteReq(params))
    val mem = new MemReadIO

    val flush = Input(Bool())
    val fencei = Input(Bool())
  })

  object State extends ChiselEnum {
    val Idle, MemReq, Refill, Reply, Drain, Discard = Value
  }

  val state = RegInit(State.Idle)
  val nextState = WireDefault(state)

  val missReq = Reg(new I$0Packet(params))
  val refillWay = Reg(UInt(params.wayWidth.W))
  val refillLine = Reg(Vec(params.wordsPerLine, UInt(params.dataWidth.W)))
  val refillBeat = RegInit(0.U(params.wordOffsetWidth.max(1).W))

  // 流水线中可能留有refill完成前产生的同line miss，保留最近完成的line供I$1消化它们。
  //不需要一个新的refillLineBuffer。我们只需要在state==Idle的时候，指示我们这个refillLine是否
  //valid即可。
  val refillBufferValid = RegInit(false.B)
  val refillBufferBase = Reg(UInt(params.addrWidth.W))
  val refillBufferWay = Reg(UInt(params.wayWidth.W))

  val replacement = Replacement(params)

  val inputValid = io.in.valid
  //两种hit，一种是直接hit，一种是命中我们的本地缓存了
  val inputDirectHit = inputValid && io.in.bits.icacheResp.hit
  val inputRefillHit = inputValid && refillBufferValid &&
    params.lineBase(io.in.bits.pc) === refillBufferBase

  val inputHit = inputDirectHit || inputRefillHit
  val inputMiss = inputValid && !inputHit
  val acceptMiss = io.in.fire && inputMiss

  val refillActive = state === State.Refill || state === State.Drain || state === State.Discard
  val lastRefillBeat = io.mem.r.fire && io.mem.r.bits.last

  // State transition logic
  switch(state) {
    is(State.Idle) {
      when(acceptMiss) {
        nextState := State.MemReq
      }
    }

    is(State.MemReq) {
      when(io.flush) {
        nextState := State.Idle
      }.elsewhen(io.mem.a.fire) {
        nextState := State.Refill
      }
    }

    is(State.Refill) {
      when(io.flush) {
        when(lastRefillBeat) {
          nextState := State.Idle
        }.elsewhen(io.fencei) {
          nextState := State.Discard
        }.otherwise {
          nextState := State.Drain
        }
      }.elsewhen(lastRefillBeat) {
        nextState := State.Reply
      }
    }

    is(State.Reply) {
      when(io.flush || io.out.fire) {
        nextState := State.Idle
      }
    }

    is(State.Drain) {
      when(lastRefillBeat) {
        nextState := State.Idle
      }.elsewhen(io.fencei) {
        nextState := State.Discard
      }
    }

    is(State.Discard) {
      when(lastRefillBeat) {
        nextState := State.Idle
      }
    }
  }

  state := nextState

  // Output logic
  io.in.ready :=
    state === State.Idle &&
    !io.flush &&
    Mux(inputHit, io.out.ready, true.B) //hit 没有寄存路径，必须立刻走


  io.out.valid := !reset.asBool && !io.flush &&
    ((state === State.Idle && inputHit) || state === State.Reply)

  io.out.bits.pc := Mux(state === State.Reply, missReq.pc, io.in.bits.pc)
  io.out.bits.inst := Mux(
    state === State.Reply,
    refillLine(params.wordOffset(missReq.pc)),
    Mux(
      inputDirectHit,
      io.in.bits.icacheResp.word,
      params.wordFromLine(refillLine.asUInt, params.wordOffset(io.in.bits.pc)),
    ),
  )

  io.mem.a.valid := state === State.MemReq && !io.flush
  io.mem.a.bits.addr := params.lineBase(missReq.pc)
  io.mem.a.bits.size := 2.U
  io.mem.a.bits.len := (params.wordsPerLine - 1).U
  io.mem.a.bits.write := false.B
  io.mem.a.bits.id := 0.U

  io.mem.r.ready := refillActive

  val completedLine = Wire(Vec(params.wordsPerLine, UInt(params.dataWidth.W)))
  completedLine := refillLine
  completedLine(refillBeat) := io.mem.r.bits.data

  // Redirect后的Drain仍可填Cache；fence.i对应的Discard只消费memory response。
  val writeRefill = lastRefillBeat &&
    ((state === State.Refill && !io.fencei) ||
      (state === State.Drain && !io.fencei))

  io.cacheSetWrite.valid := writeRefill
  io.cacheSetWrite.bits.index := params.index(missReq.pc)
  io.cacheSetWrite.bits.way := refillWay
  io.cacheSetWrite.bits.valid := true.B
  io.cacheSetWrite.bits.meta.tag := params.tag(missReq.pc)
  io.cacheSetWrite.bits.data := completedLine.asUInt

  val hitRetired = state === State.Idle && inputHit && io.out.fire
  replacement.victimReq.set := params.index(Mux(state === State.Idle, io.in.bits.pc, missReq.pc))
  replacement.touch.valid := hitRetired || writeRefill
  replacement.touch.bits.set := Mux(hitRetired, params.index(io.in.bits.pc), params.index(missReq.pc))
  replacement.touch.bits.way := Mux(
    hitRetired,
    Mux(inputDirectHit, io.in.bits.icacheResp.way, refillBufferWay),
    refillWay,
  )

  // Datapath register updates
  when(acceptMiss) {
    missReq := io.in.bits
    refillWay := replacement.victimResp.way
    refillBeat := 0.U
  }

  when(io.mem.r.fire) {
    refillLine(refillBeat) := io.mem.r.bits.data
    refillBeat := Mux(io.mem.r.bits.last, 0.U, refillBeat + 1.U)

    when(io.mem.r.bits.last) {
      assert(refillBeat === (params.wordsPerLine - 1).U, "refill last arrived on the wrong beat")
    }
  }

  when(io.fencei) {
    refillBufferValid := false.B
  }.elsewhen(writeRefill) {
    refillBufferValid := true.B
    refillBufferBase := params.lineBase(missReq.pc)
    refillBufferWay := refillWay
  }
}
