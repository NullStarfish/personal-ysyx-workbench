package mycpu.cache

import chisel3._
import chisel3.util._

class CacheSetLookupReq(params: CacheParams) extends Bundle {
  val index = UInt(params.indexBits.W)
  val tag = UInt(params.tagBits.W)
  val wordOffset = UInt(params.wordOffsetBits.W)
}

class CacheSetLookupResp(params: CacheParams) extends Bundle {
  val hit = Bool()
  val way = UInt(params.wayBits.W)
  val line = UInt(params.lineBits.W)
  val word = UInt(params.dataBits.W)
}

class CacheSetWriteReq(params: CacheParams) extends Bundle {
  val index = UInt(params.indexBits.W)
  val way = UInt(params.wayBits.W)
  val meta = new CacheLineMeta(params)
  val data = UInt(params.lineBits.W)
}

class CacheSetIO(params: CacheParams) extends Bundle {
  val lookup = Flipped(Valid(new CacheSetLookupReq(params)))
  val lookupResp = Output(new CacheSetLookupResp(params))
  val write = Flipped(Valid(new CacheSetWriteReq(params)))
}

class CacheSet(params: CacheParams) extends Module {
  val io = IO(new CacheSetIO(params))

  val lines = RegInit(VecInit(Seq.fill(params.sets) {
    VecInit(Seq.fill(params.ways) {
      0.U.asTypeOf(new CacheLineBits(params))
    })
  }))

  val selectedSet = lines(io.lookup.bits.index)
  val wayHits = Wire(Vec(params.ways, Bool()))

  for (way <- 0 until params.ways) {
    wayHits(way) := io.lookup.valid &&
      selectedSet(way).meta.valid &&
      selectedSet(way).meta.tag === io.lookup.bits.tag
  }

  val hit = wayHits.asUInt.orR
  val hitWay = PriorityEncoder(wayHits)
  val hitLine =
    if (params.ways == 1) selectedSet(0)
    else selectedSet(hitWay)

  io.lookupResp.hit := hit
  io.lookupResp.way := hitWay
  io.lookupResp.line := hitLine.data
  io.lookupResp.word := params.wordFromLine(hitLine.data, io.lookup.bits.wordOffset)

  when(io.write.valid) {
    if (params.ways == 1) {
      lines(io.write.bits.index)(0).meta := io.write.bits.meta
      lines(io.write.bits.index)(0).data := io.write.bits.data
    } else {
      lines(io.write.bits.index)(io.write.bits.way).meta := io.write.bits.meta
      lines(io.write.bits.index)(io.write.bits.way).data := io.write.bits.data
    }
  }
}
