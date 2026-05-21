package mycpu.cache

import chisel3._
import chisel3.util._
import mycpu.common._

case class CacheParams(
    addrWidth: Int = XLEN,
    dataWidth: Int = XLEN,
    lineBytes: Int = 32,
    sets: Int = 128,
    ways: Int = 2,
    replacement: ReplacementPolicy.Type = ReplacementPolicy.LRU,
) {
  require(addrWidth > 0, "addrWidth must be positive")
  require(dataWidth > 0 && dataWidth % 8 == 0, "dataWidth must be a positive byte-aligned width")
  require(isPow2(lineBytes) && lineBytes >= dataWidth / 8, "lineBytes must be a power of two and at least one data word")
  require(isPow2(sets) && sets > 0, "sets must be a positive power of two")
  require(isPow2(ways) && ways > 0, "ways must be a positive power of two")

  val wordBytes: Int = dataWidth / 8
  val wordsPerLine: Int = lineBytes / wordBytes
  val lineWidth: Int = lineBytes * 8
  val offsetWidth: Int = log2Ceil(lineBytes)
  val wordOffsetWidth: Int = log2Ceil(wordsPerLine)
  val indexWidth: Int = log2Ceil(sets)
  val tagWidth: Int = addrWidth - indexWidth - offsetWidth
  val wayWidth: Int = log2Ceil(ways).max(1)
  val capacityBytes: Int = lineBytes * sets * ways

  require(tagWidth > 0, "addrWidth must leave room for a non-empty tag")
  require(
    replacement != ReplacementPolicy.LRU || ways == 1 || ways == 2,
    "exact LRU currently supports direct-mapped or 2-way caches",
  )

  def lineBase(addr: UInt): UInt =
    Cat(addr(addrWidth - 1, offsetWidth), 0.U(offsetWidth.W))

  def tag(addr: UInt): UInt =
    addr(addrWidth - 1, indexWidth + offsetWidth)

  def index(addr: UInt): UInt =
    if (indexWidth == 0) 0.U else addr(indexWidth + offsetWidth - 1, offsetWidth)

  def wordOffset(addr: UInt): UInt =
    if (wordOffsetWidth == 0) 0.U else addr(offsetWidth - 1, log2Ceil(wordBytes))

  def wordFromLine(line: UInt, wordOffset: UInt): UInt =
    line.asTypeOf(Vec(wordsPerLine, UInt(dataWidth.W)))(wordOffset)

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
  val SimpICache: CacheParams = CacheParams(
    lineBytes = 4,
    sets = 32,
    ways = 1,
    replacement = ReplacementPolicy.LRU,
  )
}
