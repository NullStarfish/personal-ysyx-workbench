package mycpu.cache

import chisel3._
import chisel3.util._
import mycpu.common._
import mycpu.memory._

class ICacheIO(params: CacheParams = CacheParams()) extends Bundle {
  val cpuReq = Flipped(Decoupled(UInt(params.addrWidth.W)))
  val cpuReply = Decoupled(new FetchResp)

  val mem = new MemReadIO

  val fencei = Input(Bool())
  val prefetch = Flipped(Valid(UInt(params.addrWidth.W)))
}
