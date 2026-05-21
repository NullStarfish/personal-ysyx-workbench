package mycpu.cache

import chisel3._
import chisel3.util._

class CacheSetLookupReq(params: CacheParams) extends Bundle {
  val index = UInt(params.indexWidth.W)
  val tag = UInt(params.tagWidth.W)
  val wordOffset = UInt(params.wordOffsetWidth.W)
}

class CacheSetLookupResp(params: CacheParams) extends Bundle {
  val hit = Bool()
  val way = UInt(params.wayWidth.W)
  val line = UInt(params.lineWidth.W)
  val word = UInt(params.dataWidth.W)
}

class CacheSetWriteReq(params: CacheParams) extends Bundle {
  val index = UInt(params.indexWidth.W)
  val way = UInt(params.wayWidth.W)
  val valid = Bool()
  val meta = new CacheLineMeta(params)
  val data = UInt(params.lineWidth.W)
}

class CacheSetIO(params: CacheParams) extends Bundle {
  val lookup = Flipped(Valid(new CacheSetLookupReq(params)))
  val lookupResp = Output(new CacheSetLookupResp(params))
  val write = Flipped(Valid(new CacheSetWriteReq(params)))
}

class CacheSet(params: CacheParams) extends Module {
  val io = IO(new CacheSetIO(params))

  val lines = SyncReadMem(params.sets * params.ways, new CacheLine(params))
  val validBits = RegInit(VecInit(Seq.fill(params.sets) {
    VecInit(Seq.fill(params.ways)(false.B))
  }))

  private def lineAddr(index: UInt, way: UInt): UInt =
    index * params.ways.U + way

  val lookupReq = RegEnable(io.lookup.bits, 0.U.asTypeOf(new CacheSetLookupReq(params)), io.lookup.valid)
  val lookupValid = RegNext(io.lookup.valid, false.B)
  val lookupWayValid = RegEnable(validBits(io.lookup.bits.index), 0.U.asTypeOf(Vec(params.ways, Bool())), io.lookup.valid)
  val selectedSet = Wire(Vec(params.ways, new CacheLine(params)))

  for (way <- 0 until params.ways) {
    selectedSet(way) := lines.read(lineAddr(io.lookup.bits.index, way.U), io.lookup.valid)
  }

  val wayHits = Wire(Vec(params.ways, Bool()))

  for (way <- 0 until params.ways) {
    wayHits(way) := lookupValid &&
      lookupWayValid(way) &&
      selectedSet(way).meta.tag === lookupReq.tag
  }

  val hit = wayHits.asUInt.orR
  val hitWay = PriorityEncoder(wayHits)
  val hitLine =
    if (params.ways == 1) selectedSet(0)
    else selectedSet(hitWay)

  io.lookupResp.hit := hit
  io.lookupResp.way := hitWay
  io.lookupResp.line := hitLine.data
  io.lookupResp.word := params.wordFromLine(hitLine.data, lookupReq.wordOffset)

  when(io.write.valid) {
    val writeLine = Wire(new CacheLine(params))
    writeLine.meta := io.write.bits.meta
    writeLine.data := io.write.bits.data
    lines.write(lineAddr(io.write.bits.index, io.write.bits.way), writeLine)
    if (params.ways == 1) {
      validBits(io.write.bits.index)(0) := io.write.bits.valid
    } else {
      validBits(io.write.bits.index)(io.write.bits.way) := io.write.bits.valid
    }
  }
}
