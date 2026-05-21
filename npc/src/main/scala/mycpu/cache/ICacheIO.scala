package mycpu.cache

import chisel3._
import chisel3.util._
import mycpu.common._

class ICacheCpuReq(params: CacheParams) extends Bundle {
  val pc = UInt(params.addrWidth.W)
}

class ICacheCpuReply(params: CacheParams) extends Bundle {
  val pc = UInt(params.addrWidth.W)
  val inst = UInt(params.dataWidth.W)
  val hit = Bool()
}

class ICacheMemReq(params: CacheParams) extends Bundle {
  val addr = UInt(params.addrWidth.W)
}

class ICacheMemReply(params: CacheParams) extends Bundle {
  val data = UInt(params.dataWidth.W)
}

class ICachePrefetchReq(params: CacheParams) extends Bundle {
  val addr = UInt(params.addrWidth.W)
}

class ICacheIO(params: CacheParams = CacheParams()) extends Bundle {
  val cpuReq = Flipped(Decoupled(new ICacheCpuReq(params)))
  val cpuReply = Decoupled(new ICacheCpuReply(params))

  val memReq = Decoupled(new ICacheMemReq(params))
  val memReply = Flipped(Decoupled(new ICacheMemReply(params)))

  val redirect = Flipped(Valid(UInt(params.addrWidth.W)))
  val prefetch = Flipped(Valid(new ICachePrefetchReq(params)))
}

