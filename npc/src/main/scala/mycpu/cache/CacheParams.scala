package mycpu.cache

import chisel3._
import chisel3.util._
import mycpu.common._

case class CacheParams(
    addrBits: Int = XLEN,
    dataBits: Int = XLEN,
    lineBytes: Int = 32,
    sets: Int = 128,
    ways: Int = 2,
    replacement: ReplacementPolicy.Type = ReplacementPolicy.LRU,
) {
  require(addrBits > 0, "addrBits must be positive")
  require(dataBits > 0 && dataBits % 8 == 0, "dataBits must be a positive byte-aligned width")
  require(isPow2(lineBytes) && lineBytes >= dataBits / 8, "lineBytes must be a power of two and at least one data word")
  require(isPow2(sets) && sets > 0, "sets must be a positive power of two")
  require(isPow2(ways) && ways > 0, "ways must be a positive power of two")

  val bytesPerWord: Int = dataBits / 8
  val wordsPerLine: Int = lineBytes / bytesPerWord
  val lineBits: Int = lineBytes * 8
  val offsetBits: Int = log2Ceil(lineBytes)
  val wordOffsetBits: Int = log2Ceil(wordsPerLine)
  val indexBits: Int = log2Ceil(sets)
  val tagBits: Int = addrBits - indexBits - offsetBits
  val wayBits: Int = log2Ceil(ways).max(1)
  val capacityBytes: Int = lineBytes * sets * ways

  require(tagBits > 0, "addrBits must leave room for a non-empty tag")
  require(
    replacement != ReplacementPolicy.LRU || ways == 1 || ways == 2,
    "exact LRU currently supports direct-mapped or 2-way caches",
  )

  def lineBase(addr: UInt): UInt =
    Cat(addr(addrBits - 1, offsetBits), 0.U(offsetBits.W))

  def tag(addr: UInt): UInt =
    addr(addrBits - 1, indexBits + offsetBits)

  def index(addr: UInt): UInt =
    if (indexBits == 0) 0.U else addr(indexBits + offsetBits - 1, offsetBits)

  def wordOffset(addr: UInt): UInt =
    if (wordOffsetBits == 0) 0.U else addr(offsetBits - 1, log2Ceil(bytesPerWord))

  def wordFromLine(line: UInt, wordOffset: UInt): UInt =
    line.asTypeOf(Vec(wordsPerLine, UInt(dataBits.W)))(wordOffset)

  private def isPow2(x: Int): Boolean =
    x > 0 && (x & (x - 1)) == 0
}

object CacheConfigs {
  val CourseCache8KiB2Way32B: CacheParams = CacheParams(
    lineBytes = 32,
    sets = 128,
    ways = 2,
    replacement = ReplacementPolicy.LRU,
  )
}
