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

  protected def stepUntilRetireCount(
      c: Core,
      targetRetires: Int,
      maxCycles: Int,
      service: => Unit,
  ): Unit = {
    var cycles = 0
    while (c.io.trace.retireCount.peek().litValue < targetRetires && cycles < maxCycles) {
      service
      c.clock.step()
      cycles += 1
    }
  }

  protected def stepUntilRetireCountCollect(
      c: Core,
      targetRetires: Int,
      maxCycles: Int,
      service: => Unit,
  ): Seq[(BigInt, BigInt, BigInt)] = {
    var cycles = 0
    var lastRetireCount = c.io.trace.retireCount.peek().litValue
    val retired = scala.collection.mutable.ArrayBuffer.empty[(BigInt, BigInt, BigInt)]
    while (c.io.trace.retireCount.peek().litValue < targetRetires && cycles < maxCycles) {
      service
      c.clock.step()
      cycles += 1
      val retireCount = c.io.trace.retireCount.peek().litValue
      if (retireCount != lastRetireCount) {
        retired += ((
          c.io.trace.lastRetire.bits.pc.peek().litValue,
          c.io.trace.lastRetire.bits.inst.peek().litValue,
          c.io.trace.lastRetire.bits.dnpc.peek().litValue,
        ))
        lastRetireCount = retireCount
      }
    }
    retired.toSeq
  }

  protected def serviceReadBus(
      c: Core,
      memory: Map[BigInt, BigInt],
      pending: List[ReadTxn],
  ): List[ReadTxn] = {
    initBus(c)

    val arValid = c.io.master.ar.valid.peek().litValue == 1
    val nextPending =
      if (arValid) {
        c.io.master.ar.ready.poke(true.B)
        pending :+ ReadTxn(c.io.master.ar.bits.addr.peek().litValue, delay = 1)
      } else pending

    nextPending match {
      case ReadTxn(addr, 0) :: tail =>
        c.io.master.r.valid.poke(true.B)
        c.io.master.r.bits.id.poke(0.U)
        c.io.master.r.bits.data.poke(memory.getOrElse(addr, BigInt(0)).U)
        c.io.master.r.bits.resp.poke(0.U)
        c.io.master.r.bits.last.poke(true.B)
        if (c.io.master.r.ready.peek().litValue == 1) tail else nextPending
      case head :: tail =>
        head.copy(delay = head.delay - 1) :: tail
      case Nil =>
        Nil
    }
  }

  protected def serviceBus(
      c: Core,
      memory: mutable.Map[BigInt, BigInt],
      pendingRead: List[ReadTxn],
      pendingWriteResp: Option[WriteResp],
  ): (List[ReadTxn], Option[WriteResp]) = {
    initBus(c)

    val nextRead =
      if (c.io.master.ar.valid.peek().litValue == 1) {
        c.io.master.ar.ready.poke(true.B)
        pendingRead :+ ReadTxn(c.io.master.ar.bits.addr.peek().litValue, delay = 1)
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
      case ReadTxn(addr, 0) :: tail =>
        c.io.master.r.valid.poke(true.B)
        c.io.master.r.bits.id.poke(0.U)
        c.io.master.r.bits.data.poke(memory.getOrElse(addr, BigInt(0)).U)
        c.io.master.r.bits.resp.poke(0.U)
        c.io.master.r.bits.last.poke(true.B)
        if (c.io.master.r.ready.peek().litValue == 1) tail else nextRead
      case head :: tail =>
        head.copy(delay = head.delay - 1) :: tail
      case Nil =>
        Nil
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
