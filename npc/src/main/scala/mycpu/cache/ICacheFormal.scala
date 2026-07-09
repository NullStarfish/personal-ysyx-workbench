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

  io.mem.a.valid := dut.io.mem.a.valid
  io.mem.a.bits := dut.io.mem.a.bits
  dut.io.mem.a.ready := io.mem.a.ready

  dut.io.mem.r.valid := io.mem.r.valid
  dut.io.mem.r.bits := io.mem.r.bits
  io.mem.r.ready := dut.io.mem.r.ready

  dut.io.fencei := io.fencei
  dut.io.prefetch.valid := io.prefetch.valid
  dut.io.prefetch.bits := io.prefetch.bits

  assume(!io.fencei)
  assume(!io.prefetch.valid)

  val active = RegInit(false.B)
  val activePc = Reg(UInt(params.addrWidth.W))
  val currentPc = Mux(active, activePc, io.cpuReq.bits)

  when(dut.io.cpuReq.fire) {
    activePc := dut.io.cpuReq.bits
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
  when(dut.io.mem.a.fire) {
    memOutstanding := true.B
    outstandingBeat := 0.U
  }
  when(dut.io.mem.r.fire) {
    when(dut.io.mem.r.bits.last) {
      memOutstanding := false.B
      outstandingBeat := 0.U
    }.otherwise {
      outstandingBeat := outstandingBeat + 1.U
    }
  }

  when(!memOutstanding) {
    assume(!io.mem.r.valid)
  }

  val refillBeat = RegInit(0.U(params.wordOffsetWidth.W))
  when(dut.io.mem.r.fire) {
    when(dut.io.mem.r.bits.last) {
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
      assert(active || io.cpuReq.valid)
    }

    when(dut.io.mem.a.valid) {
      assert(active || dut.io.cpuReq.fire)
      assert(!memOutstanding)
      assert(dut.io.mem.a.bits.addr(log2Ceil(params.wordBytes) - 1, 0) === 0.U)
      assert(dut.io.mem.a.bits.size === 2.U)
      assert(dut.io.mem.a.bits.len === (params.wordsPerLine - 1).U)

      assert(dut.io.mem.a.bits.addr === params.lineBase(currentPc))
    }

    when(dut.io.mem.r.fire) {
      assert(memOutstanding)
      assert(active)
      when(dut.io.mem.r.bits.last) {
        assert(outstandingBeat === (params.wordsPerLine - 1).U)
      }
    }

    cover(dut.io.cpuReply.fire)
    cover(dut.io.mem.a.fire)
    cover(dut.io.mem.r.fire && dut.io.mem.r.bits.last)
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
