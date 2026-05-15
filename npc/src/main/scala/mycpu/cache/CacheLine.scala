package mycpu.cache

import chisel3._

class CacheLineMeta(params: CacheParams) extends Bundle {
  val valid = Bool()
  val tag = UInt(params.tagBits.W)
}

class CacheLineBits(params: CacheParams) extends Bundle {
  val meta = new CacheLineMeta(params)
  val data = UInt(params.lineBits.W)
}

class CacheSetBits(params: CacheParams) extends Bundle {
  val ways = Vec(params.ways, new CacheLineBits(params))
}

class CacheWayHit(params: CacheParams) extends Bundle {
  val valid = Bool()
  val way = UInt(params.wayBits.W)
}

