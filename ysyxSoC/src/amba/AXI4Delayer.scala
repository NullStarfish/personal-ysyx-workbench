package ysyx

import chisel3._
import chisel3.util._

import org.chipsalliance.cde.config.Parameters
import freechips.rocketchip.amba._
import freechips.rocketchip.amba.axi4._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.util._

class AXI4DelayerIO extends Bundle {
  val clock = Input(Clock())
  val reset = Input(Reset())
  val in = Flipped(new AXI4Bundle(AXI4BundleParameters(addrBits = 32, dataBits = 32, idBits = 4)))
  val out = new AXI4Bundle(AXI4BundleParameters(addrBits = 32, dataBits = 32, idBits = 4))
}

class AXI4DelayChannel[T <: Data](gen: T, delayCycles: Int) extends Module {
  val io = IO(new Bundle {
    val in = Flipped(Decoupled(gen))
    val out = Decoupled(gen)
  })

  val IDLE = 0
  val DELAY = 1
  val SEND = 2

  val state = RegInit(IDLE.U(2.W))
  val delayCnt = RegInit(0.U(32.W))
  val bits = Reg(chiselTypeOf(io.in.bits))

  io.in.ready := state === IDLE.U
  io.out.valid := state === SEND.U
  io.out.bits := bits

  switch (state) {
    is (IDLE.U) {
      when (io.in.fire) {
        bits := io.in.bits
        delayCnt := delayCycles.U
        state := DELAY.U
      }
    }
    is (DELAY.U) {
      delayCnt := delayCnt - 1.U
      when (delayCnt === 0.U) {
        state := SEND.U
        delayCnt := 0.U
      }
    }
    is (SEND.U) {
      when (io.out.fire) {
        state := IDLE.U
      }
    }
  }
}

class AXI4DelayerChisel extends Module {
  val io = IO(new AXI4DelayerIO)

  val r = 1
  val s = 8

  val step = math.round((r - 1) * s).toInt
  val delayCycles = (step * 2) >> 3

  val ar = Module(new AXI4DelayChannel(chiselTypeOf(io.in.ar.bits), delayCycles))
  ar.io.in.valid := io.in.ar.valid
  ar.io.in.bits := io.in.ar.bits
  io.in.ar.ready := ar.io.in.ready
  io.out.ar.valid := ar.io.out.valid
  io.out.ar.bits := ar.io.out.bits
  ar.io.out.ready := io.out.ar.ready

  val rch = Module(new AXI4DelayChannel(chiselTypeOf(io.out.r.bits), delayCycles))
  rch.io.in.valid := io.out.r.valid
  rch.io.in.bits := io.out.r.bits
  io.out.r.ready := rch.io.in.ready
  io.in.r.valid := rch.io.out.valid
  io.in.r.bits := rch.io.out.bits
  rch.io.out.ready := io.in.r.ready

  val aw = Module(new AXI4DelayChannel(chiselTypeOf(io.in.aw.bits), delayCycles))
  aw.io.in.valid := io.in.aw.valid
  aw.io.in.bits := io.in.aw.bits
  io.in.aw.ready := aw.io.in.ready
  io.out.aw.valid := aw.io.out.valid
  io.out.aw.bits := aw.io.out.bits
  aw.io.out.ready := io.out.aw.ready

  val w = Module(new AXI4DelayChannel(chiselTypeOf(io.in.w.bits), delayCycles))
  w.io.in.valid := io.in.w.valid
  w.io.in.bits := io.in.w.bits
  io.in.w.ready := w.io.in.ready
  io.out.w.valid := w.io.out.valid
  io.out.w.bits := w.io.out.bits
  w.io.out.ready := io.out.w.ready

  val b = Module(new AXI4DelayChannel(chiselTypeOf(io.out.b.bits), delayCycles))
  b.io.in.valid := io.out.b.valid
  b.io.in.bits := io.out.b.bits
  io.out.b.ready := b.io.in.ready
  io.in.b.valid := b.io.out.valid
  io.in.b.bits := b.io.out.bits
  b.io.out.ready := io.in.b.ready
}

class AXI4DelayerWrapper(implicit p: Parameters) extends LazyModule {
  val node = AXI4IdentityNode()

  lazy val module = new Impl
  class Impl extends LazyModuleImp(this) {
    (node.in zip node.out) foreach { case ((in, edgeIn), (out, edgeOut)) =>
      val delayer = Module(new AXI4DelayerChisel)
      delayer.io.clock := clock
      delayer.io.reset := reset
      delayer.io.in <> in
      out <> delayer.io.out
    }
  }
}

object AXI4Delayer {
  def apply()(implicit p: Parameters): AXI4Node = {
    val axi4delay = LazyModule(new AXI4DelayerWrapper)
    axi4delay.node
  }
}
