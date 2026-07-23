package ysyx

import chisel3._
import chisel3.util._

import org.chipsalliance.cde.config.Parameters
import freechips.rocketchip.amba._
import freechips.rocketchip.amba.apb._
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.util._

class APBDelayerIO extends Bundle {
  val clock = Input(Clock())
  val reset = Input(Reset())
  val in = Flipped(new APBBundle(APBBundleParameters(addrBits = 32, dataBits = 32)))
  val out = new APBBundle(APBBundleParameters(addrBits = 32, dataBits = 32))
}

class apb_delayer extends BlackBox {
  val io = IO(new APBDelayerIO)
}

class APBDelayerChisel extends Module {
  val io = IO(new APBDelayerIO)

  val r = 1
  val s = 8

  val step = math.round((r - 1) * s).toInt

  val state = RegInit(0.U(2.W))
  val IDLE = 0
  val ACCUMU = 1
  val DELAY = 2

  val delayCnt = RegInit(0.U(32.W))
  val start = io.in.psel && !io.in.penable && state === IDLE.U
  val done = state === ACCUMU.U && io.in.psel && io.in.penable && io.out.pready


  io.out <> io.in
  io.in.pready := false.B
  io.out.psel := false.B
  io.out.penable := false.B

  val pslverrLatch = Reg(Bool())
  val prdataLatch = Reg(chiselTypeOf(io.in.prdata))

  io.in.pslverr := pslverrLatch
  io.in.prdata := prdataLatch

  when (done) {
    pslverrLatch := io.out.pslverr
    prdataLatch := io.out.prdata
  }

  when (state === IDLE.U || state === ACCUMU.U) {
    io.out.psel := io.in.psel
    io.out.penable := io.in.penable
  }


  switch (state) {
    is (IDLE.U) {
      when (start) {
        state := ACCUMU.U
        delayCnt := delayCnt + step.U
      }
    }
    is (ACCUMU.U) {
      delayCnt := delayCnt + step.U
      when (done) {
        state := DELAY.U
        delayCnt := (delayCnt + step.U) >> 3.U
      }
    }
    is (DELAY.U) {
      delayCnt := delayCnt - 1.U
      when (delayCnt === 0.U) {
        io.in.pready := true.B
        state := IDLE.U
        delayCnt := 0.U
      }
    }
  }
  
}

class APBDelayerWrapper(implicit p: Parameters) extends LazyModule {
  val node = APBIdentityNode()

  lazy val module = new Impl
  class Impl extends LazyModuleImp(this) {
    (node.in zip node.out) foreach { case ((in, edgeIn), (out, edgeOut)) =>
      val delayer = Module(new APBDelayerChisel)
      delayer.io.clock := clock
      delayer.io.reset := reset
      delayer.io.in <> in
      out <> delayer.io.out
    }
  }
}

object APBDelayer {
  def apply()(implicit p: Parameters): APBNode = {
    val apbdelay = LazyModule(new APBDelayerWrapper)
    apbdelay.node
  }
}
