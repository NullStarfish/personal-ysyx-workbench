package mycpu.cache

import _root_.circt.stage.ChiselStage
import chisel3._
import chisel3.util._

class ICacheFormalHarness extends Module {
  private val params = CacheParams(lineBytes = 8, sets = 2, ways = 1)

  val io = IO(new ICacheIO(params))
  val dut = Module(new ICache(params))

  dut.io.cpuReq.valid := io.cpuReq.valid
  dut.io.cpuReq.bits := io.cpuReq.bits
  io.cpuReq.ready := dut.io.cpuReq.ready

  io.cpuReply.valid := dut.io.cpuReply.valid
  io.cpuReply.bits := dut.io.cpuReply.bits
  dut.io.cpuReply.ready := io.cpuReply.ready

  io.memReq.valid := dut.io.memReq.valid
  io.memReq.bits := dut.io.memReq.bits
  dut.io.memReq.ready := io.memReq.ready

  dut.io.memReply.valid := io.memReply.valid
  dut.io.memReply.bits := io.memReply.bits
  io.memReply.ready := dut.io.memReply.ready

  dut.io.redirect.valid := io.redirect.valid
  dut.io.redirect.bits := io.redirect.bits
  dut.io.prefetch.valid := io.prefetch.valid
  dut.io.prefetch.bits := io.prefetch.bits

  assume(!io.redirect.valid)
  assume(!io.prefetch.valid)

  val active = RegInit(false.B)
  val activePc = Reg(UInt(params.addrWidth.W))
  val currentPc = Mux(active, activePc, io.cpuReq.bits.pc)

  when(dut.io.cpuReq.fire) {
    activePc := dut.io.cpuReq.bits.pc
  }
  when(dut.io.cpuReq.fire && !dut.io.cpuReply.fire) {
    active := true.B
  }.elsewhen(!dut.io.cpuReq.fire && dut.io.cpuReply.fire) {
    active := false.B
  }.elsewhen(dut.io.cpuReq.fire && dut.io.cpuReply.fire) {
    active := false.B
  }

  val memOutstanding = RegInit(false.B)
  val outstandingBeat = RegInit(0.U(params.wordOffsetWidth.W))
  when(dut.io.memReq.fire) {
    memOutstanding := true.B
    outstandingBeat := 0.U
  }
  when(dut.io.memReply.fire) {
    when(outstandingBeat === (params.wordsPerLine - 1).U) {
      memOutstanding := false.B
      outstandingBeat := 0.U
    }.otherwise {
      outstandingBeat := outstandingBeat + 1.U
    }
  }

  when(!memOutstanding) {
    assume(!io.memReply.valid)
  }

  val refillBeat = RegInit(0.U(params.wordOffsetWidth.W))
  when(dut.io.memReply.fire) {
    when(refillBeat === (params.wordsPerLine - 1).U) {
      refillBeat := 0.U
    }.otherwise {
      refillBeat := refillBeat + 1.U
    }
  }

  when(!reset.asBool) {
    when(active) {
      assert(!dut.io.cpuReq.ready)
    }.otherwise {
      assert(dut.io.cpuReq.ready)
    }

    when(dut.io.cpuReply.valid) {
      assert(dut.io.cpuReply.bits.hit)
      assert(dut.io.cpuReply.bits.pc === currentPc)
      assert(active || io.cpuReq.valid)
    }

    when(dut.io.memReq.valid) {
      assert(active || dut.io.cpuReq.fire)
      assert(!memOutstanding)
      assert(dut.io.memReq.bits.addr(log2Ceil(params.wordBytes) - 1, 0) === 0.U)
      assert(dut.io.memReq.bits.size === 2.U)
      assert(dut.io.memReq.bits.beats === params.wordsPerLine.U)

      assert(dut.io.memReq.bits.addr === params.lineBase(currentPc))
    }

    when(dut.io.memReply.fire) {
      assert(memOutstanding)
      assert(active)
    }

    cover(dut.io.cpuReply.fire)
    cover(dut.io.memReq.fire)
    cover(dut.io.memReply.fire && refillBeat === (params.wordsPerLine - 1).U)
  }
}

object GenICacheFormal extends App {
  ChiselStage.emitSystemVerilogFile(
    new ICacheFormalHarness,
    args = Array("--target-dir", "src/main/verilog/ICacheFormal"),
    firtoolOpts = Array(
      "--disable-all-randomization",
      "--verification-flavor=immediate",
      "--lowering-options=disallowLocalVariables,disallowPackedArrays,noAlwaysComb,disallowPortDeclSharing",
    ),
  )
}
