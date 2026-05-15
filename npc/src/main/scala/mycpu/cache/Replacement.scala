package mycpu.cache

import chisel3._
import chisel3.util._

object ReplacementPolicy extends ChiselEnum {
  val DirectMapped, RoundRobin, LRU, TreePLRU, Random = Value
}

class ReplacementTouch(params: CacheParams) extends Bundle {
  val set = UInt(params.indexBits.W)
  val way = UInt(params.wayBits.W)
}

class ReplacementVictimReq(params: CacheParams) extends Bundle {
  val set = UInt(params.indexBits.W)
}

class ReplacementVictimResp(params: CacheParams) extends Bundle {
  val way = UInt(params.wayBits.W)
}

class ReplacementPolicyIO(params: CacheParams) extends Bundle {
  val touch = Flipped(Valid(new ReplacementTouch(params)))
  val victimReq = Input(new ReplacementVictimReq(params))
  val victimResp = Output(new ReplacementVictimResp(params))
}

class RoundRobinReplacement(params: CacheParams) extends Module {
  val io = IO(new ReplacementPolicyIO(params))

  val nextVictim = RegInit(VecInit(Seq.fill(params.sets)(0.U(params.wayBits.W))))
  io.victimResp.way := nextVictim(io.victimReq.set)

  when(io.touch.valid && params.ways.U =/= 1.U) {
    nextVictim(io.touch.bits.set) := io.touch.bits.way + 1.U
  }
}

class LRUReplacement(params: CacheParams) extends Module {
  require(params.ways == 1 || params.ways == 2, "LRUReplacement currently supports direct-mapped or 2-way caches")

  val io = IO(new ReplacementPolicyIO(params))

  if (params.ways == 1) {
    io.victimResp.way := 0.U
  } else {
    val lruWay = RegInit(VecInit(Seq.fill(params.sets)(0.U(params.wayBits.W))))
    io.victimResp.way := lruWay(io.victimReq.set)

    when(io.touch.valid) {
      lruWay(io.touch.bits.set) := ~io.touch.bits.way
    }
  }
}

object Replacement {
  def apply(params: CacheParams): ReplacementPolicyIO = {
    params.replacement match {
      case ReplacementPolicy.DirectMapped | ReplacementPolicy.LRU =>
        Module(new LRUReplacement(params)).io
      case ReplacementPolicy.RoundRobin =>
        Module(new RoundRobinReplacement(params)).io
      case other =>
        throw new IllegalArgumentException(s"replacement policy $other is not implemented")
    }
  }
}
