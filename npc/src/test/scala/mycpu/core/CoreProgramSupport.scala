package mycpu.core

import chisel3._
import chisel3.simulator.EphemeralSimulator._
import mycpu.common._

import scala.collection.mutable

trait CoreProgramSupport {
  protected case class ReadTxn(addr: BigInt, delay: Int)
  protected case class WriteResp(delay: Int)

  protected def initBus(c: Core): Unit = {
    c.io.master.aw.ready.poke(false.B)
    c.io.master.w.ready.poke(false.B)
    c.io.master.b.valid.poke(false.B)
    c.io.master.b.bits.id.poke(0.U)
    c.io.master.b.bits.resp.poke(0.U)
    c.io.master.ar.ready.poke(false.B)
    c.io.master.r.valid.poke(false.B)
    c.io.master.r.bits.id.poke(0.U)
    c.io.master.r.bits.data.poke(0.U)
    c.io.master.r.bits.resp.poke(0.U)
    c.io.master.r.bits.last.poke(false.B)
  }

  protected def serviceReadBus(
      c: Core,
      memory: Map[BigInt, BigInt],
      pending: Option[ReadTxn],
  ): Option[ReadTxn] = {
    initBus(c)

    val arValid = c.io.master.ar.valid.peek().litValue == 1
    val nextPending =
      if (pending.isEmpty && arValid) {
        c.io.master.ar.ready.poke(true.B)
        Some(ReadTxn(c.io.master.ar.bits.addr.peek().litValue, delay = 1))
      } else pending

    nextPending match {
      case Some(ReadTxn(addr, 0)) =>
        c.io.master.r.valid.poke(true.B)
        c.io.master.r.bits.id.poke(0.U)
        c.io.master.r.bits.data.poke(memory.getOrElse(addr, BigInt(0)).U)
        c.io.master.r.bits.resp.poke(0.U)
        c.io.master.r.bits.last.poke(true.B)
        if (c.io.master.r.ready.peek().litValue == 1) None else nextPending
      case Some(txn) =>
        Some(txn.copy(delay = txn.delay - 1))
      case None =>
        None
    }
  }

  protected def serviceBus(
      c: Core,
      memory: mutable.Map[BigInt, BigInt],
      pendingRead: Option[ReadTxn],
      pendingWriteResp: Option[WriteResp],
  ): (Option[ReadTxn], Option[WriteResp]) = {
    initBus(c)

    val nextRead =
      if (pendingRead.isEmpty && c.io.master.ar.valid.peek().litValue == 1) {
        c.io.master.ar.ready.poke(true.B)
        Some(ReadTxn(c.io.master.ar.bits.addr.peek().litValue, delay = 1))
      } else pendingRead

    val nextWriteResp =
      if (pendingWriteResp.isEmpty &&
          c.io.master.aw.valid.peek().litValue == 1 &&
          c.io.master.w.valid.peek().litValue == 1) {
        c.io.master.aw.ready.poke(true.B)
        c.io.master.w.ready.poke(true.B)

        val addr = c.io.master.aw.bits.addr.peek().litValue
        val data = c.io.master.w.bits.data.peek().litValue
        val strb = c.io.master.w.bits.strb.peek().litValue.toInt
        val old = memory.getOrElse(addr, BigInt(0))

        var merged = old
        for (i <- 0 until 4) {
          if (((strb >> i) & 1) == 1) {
            val byte = (data >> (i * 8)) & 0xff
            val mask = (BigInt(0xff) << (i * 8))
            merged = (merged & ~mask) | (byte << (i * 8))
          }
        }
        memory(addr) = merged
        Some(WriteResp(delay = 1))
      } else pendingWriteResp

    val servicedRead = nextRead match {
      case Some(ReadTxn(addr, 0)) =>
        c.io.master.r.valid.poke(true.B)
        c.io.master.r.bits.id.poke(0.U)
        c.io.master.r.bits.data.poke(memory.getOrElse(addr, BigInt(0)).U)
        c.io.master.r.bits.resp.poke(0.U)
        c.io.master.r.bits.last.poke(true.B)
        if (c.io.master.r.ready.peek().litValue == 1) None else nextRead
      case Some(txn) =>
        Some(txn.copy(delay = txn.delay - 1))
      case None =>
        None
    }

    val servicedWriteResp = nextWriteResp match {
      case Some(WriteResp(0)) =>
        c.io.master.b.valid.poke(true.B)
        c.io.master.b.bits.id.poke(0.U)
        c.io.master.b.bits.resp.poke(0.U)
        if (c.io.master.b.ready.peek().litValue == 1) None else nextWriteResp
      case Some(txn) =>
        Some(txn.copy(delay = txn.delay - 1))
      case None =>
        None
    }

    (servicedRead, servicedWriteResp)
  }
}
