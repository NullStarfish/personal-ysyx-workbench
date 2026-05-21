package mycpu.cache

import chisel3._

class CacheLineMeta(params: CacheParams) extends Bundle {
  val tag = UInt(params.tagWidth.W)
}

class CacheLine(params: CacheParams) extends Bundle {
  val meta = new CacheLineMeta(params)
  val data = UInt(params.lineWidth.W)
}

class CacheSets(params: CacheParams) extends Bundle {
  val ways = Vec(params.ways, new CacheLine(params))
}

class CacheWayHit(params: CacheParams) extends Bundle {
  val valid = Bool()
  val way = UInt(params.wayWidth.W)
}
