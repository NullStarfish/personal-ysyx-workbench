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
  val selectedValid = Bool()
  val storedTag = UInt(params.tagWidth.W)
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

  val lines = SyncReadMem(params.sets * params.ways, UInt(params.lineWidth.W))
  val tags = Reg(Vec(params.sets, Vec(params.ways, UInt(params.tagWidth.W))))
  val validBits = RegInit(VecInit(Seq.fill(params.sets) {
    VecInit(Seq.fill(params.ways)(false.B))
  }))

  private def lineAddr(index: UInt, way: UInt): UInt =
    index * params.ways.U + way

  val lookupReq = RegEnable(io.lookup.bits, 0.U.asTypeOf(new CacheSetLookupReq(params)), io.lookup.valid)
  val lookupValid = RegNext(io.lookup.valid, false.B)
  val lookupWayValid = RegEnable(validBits(io.lookup.bits.index), 0.U.asTypeOf(Vec(params.ways, Bool())), io.lookup.valid)
  val lookupTags = RegEnable(tags(io.lookup.bits.index), 0.U.asTypeOf(Vec(params.ways, UInt(params.tagWidth.W))), io.lookup.valid)
  val selectedData = Wire(Vec(params.ways, UInt(params.lineWidth.W)))

  for (way <- 0 until params.ways) {
    selectedData(way) := lines.read(lineAddr(io.lookup.bits.index, way.U), io.lookup.valid)
  }

  val wayHits = Wire(Vec(params.ways, Bool()))

  for (way <- 0 until params.ways) {
    wayHits(way) := lookupValid &&
      lookupWayValid(way) &&
      lookupTags(way) === lookupReq.tag
  }

  val hit = wayHits.asUInt.orR
  val hitWay = PriorityEncoder(wayHits)
  val hitData =
    if (params.ways == 1) selectedData(0)
    else selectedData(hitWay)
  val storedTag =
    if (params.ways == 1) lookupTags(0)
    else lookupTags(hitWay)

  io.lookupResp.hit := hit
  io.lookupResp.way := hitWay
  io.lookupResp.selectedValid := lookupWayValid(hitWay)
  io.lookupResp.storedTag := storedTag
  io.lookupResp.line := hitData
  io.lookupResp.word := params.wordFromLine(hitData, lookupReq.wordOffset)

  when(io.write.valid) {
    lines.write(lineAddr(io.write.bits.index, io.write.bits.way), io.write.bits.data)
    if (params.ways == 1) {
      tags(io.write.bits.index)(0) := io.write.bits.meta.tag
      validBits(io.write.bits.index)(0) := io.write.bits.valid
    } else {
      tags(io.write.bits.index)(io.write.bits.way) := io.write.bits.meta.tag
      validBits(io.write.bits.index)(io.write.bits.way) := io.write.bits.valid
    }
  }
}
