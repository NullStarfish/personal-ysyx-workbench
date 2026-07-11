package mycpu.core.frontend

import chisel3._
import chisel3.simulator.EphemeralSimulator._
import chisel3.util._
import mycpu.cache.{CacheConfigs, CacheSetLookupResp}
import mycpu.common._
import mycpu.core.bundles._
import mycpu.core.components.FlushableStage
import mycpu.memory.MemReadIO
import org.scalatest.flatspec.AnyFlatSpec

class FetchDecodeHarness extends Module {
  private val params = CacheConfigs.SimpICache

  val io = IO(new Bundle {
    val lookupPc = Valid(UInt(XLEN.W))
    val cacheResp = Input(new CacheSetLookupResp(params))
    val redirect = Input(Valid(UInt(XLEN.W)))
    val flush = Input(Bool())
    val stageStall = Input(Bool())
    val mem = new MemReadIO
    val out = Decoupled(new DecodePacket)
    val regWrite = Flipped(new WriteBackIO)
  })

  val fetch = Module(new Fetch)
  val iCache0 = Module(new I$0Stage(params))
  val iCache1 = Module(new I$1Stage(params))
  val decode = Module(new Decode)
  val ifI0 = Module(new FlushableStage(new IFPacket))
  val i0I1 = Module(new FlushableStage(new I$0Packet(params)))
  val i1Id = Module(new FlushableStage(new I$1Packet))

  val frontFlush = io.flush || io.redirect.valid

  fetch.io.out <> ifI0.io.enq
  iCache0.io.in <> ifI0.io.deq
  iCache0.io.cacheSetResp := io.cacheResp
  iCache0.io.flush := frontFlush

  iCache0.io.out <> i0I1.io.enq
  iCache1.io.in <> i0I1.io.deq
  iCache1.io.flush := frontFlush
  iCache1.io.fencei := false.B
  io.mem <> iCache1.io.mem
  iCache1.io.out <> i1Id.io.enq

  fetch.io.block := iCache0.io.blockFetch
  ifI0.io.blockEnq := iCache0.io.blockFetch
  ifI0.io.blockDeq := false.B
  ifI0.io.flush := frontFlush
  i0I1.io.blockEnq := false.B
  i0I1.io.blockDeq := false.B
  i0I1.io.flush := frontFlush
  i1Id.io.blockEnq := false.B
  i1Id.io.blockDeq := io.stageStall
  i1Id.io.flush := frontFlush

  fetch.io.redirect := io.redirect
  io.lookupPc.valid := fetch.io.out.fire
  io.lookupPc.bits := fetch.io.out.bits.pc

  decode.io.in <> i1Id.io.deq

  decode.io.regWrite <> io.regWrite
  decode.io.forwards.foreach { forward =>
    forward.valid := false.B
    forward.addr := 0.U
    forward.data := 0.U
  }
  io.out <> decode.io.out
}

class FetchDecodeSim extends AnyFlatSpec {
  private val pc0 = BigInt(START_ADDR)

  private def addi(rd: Int, rs1: Int, imm: Int): BigInt =
    ((BigInt(imm) & 0xfff) << 20) |
      (BigInt(rs1 & 0x1f) << 15) |
      (BigInt(rd & 0x1f) << 7) |
      BigInt("0010011", 2)

  private def init(c: FetchDecodeHarness): Unit = {
    c.io.cacheResp.hit.poke(true.B)
    c.io.cacheResp.way.poke(0.U)
    c.io.cacheResp.selectedValid.poke(true.B)
    c.io.cacheResp.storedTag.poke(0.U)
    c.io.cacheResp.line.poke(0.U)
    c.io.cacheResp.word.poke(0.U)
    c.io.redirect.valid.poke(false.B)
    c.io.redirect.bits.poke(0.U)
    c.io.flush.poke(false.B)
    c.io.stageStall.poke(false.B)
    c.io.mem.a.ready.poke(false.B)
    c.io.mem.r.valid.poke(false.B)
    c.io.mem.r.bits.data.poke(0.U)
    c.io.mem.r.bits.resp.poke(0.U)
    c.io.mem.r.bits.last.poke(false.B)
    c.io.mem.r.bits.id.poke(0.U)
    c.io.out.ready.poke(false.B)
    c.io.regWrite.regWrite.wen.poke(false.B)
    c.io.regWrite.regWrite.rd.poke(0.U)
    c.io.regWrite.regWrite.wdata.poke(0.U)
  }

  private def resetDut(c: FetchDecodeHarness): Unit = {
    init(c)
    c.reset.poke(true.B)
    c.clock.step()
    c.reset.poke(false.B)
  }

  "Fetch + I$ + Decode" should "flow back-to-back hit instructions" in {
    simulate(new FetchDecodeHarness) { c =>
      val inst0 = addi(rd = 1, rs1 = 0, imm = 5)
      val inst1 = addi(rd = 2, rs1 = 1, imm = 8)
      resetDut(c)
      c.io.out.ready.poke(true.B)

      c.io.lookupPc.valid.expect(true.B)
      c.io.lookupPc.bits.expect(pc0.U)
      c.clock.step()

      c.io.cacheResp.word.poke(inst0.U)
      c.io.lookupPc.valid.expect(true.B)
      c.io.lookupPc.bits.expect((pc0 + 4).U)
      c.clock.step()

      c.io.cacheResp.word.poke(inst1.U)
      c.clock.step()
      c.io.out.valid.expect(true.B)
      c.io.out.bits.execData.pc.expect(pc0.U)
      c.io.out.bits.execData.imm.expect(5.U)
      c.clock.step()

      c.io.out.valid.expect(true.B)
      c.io.out.bits.execData.pc.expect((pc0 + 4).U)
      c.io.out.bits.execData.imm.expect(8.U)
    }
  }

  it should "keep the decoded head stable while the second response uses skid" in {
    simulate(new FetchDecodeHarness) { c =>
      val inst0 = addi(rd = 3, rs1 = 0, imm = 9)
      val inst1 = addi(rd = 4, rs1 = 0, imm = 10)
      resetDut(c)

      c.clock.step()
      c.io.cacheResp.word.poke(inst0.U)
      c.clock.step()

      c.io.cacheResp.word.poke(inst1.U)
      c.clock.step()
      c.io.out.valid.expect(true.B)
      c.io.out.bits.wbCtrl.rd.expect(3.U)
      c.clock.step()

      c.io.out.valid.expect(true.B)
      c.io.out.bits.wbCtrl.rd.expect(3.U)
      c.clock.step(2)
      c.io.out.bits.wbCtrl.rd.expect(3.U)

      c.io.out.ready.poke(true.B)
      c.clock.step()
      c.io.out.valid.expect(true.B)
      c.io.out.bits.wbCtrl.rd.expect(4.U)
    }
  }

  it should "drop the old fixed-latency response and restart at redirect target" in {
    simulate(new FetchDecodeHarness) { c =>
      val target = pc0 + 0x40
      val targetInst = addi(rd = 5, rs1 = 0, imm = 12)
      resetDut(c)
      c.io.out.ready.poke(true.B)

      c.clock.step()
      c.io.cacheResp.word.poke("hdeadbeef".U)
      c.io.redirect.valid.poke(true.B)
      c.io.redirect.bits.poke(target.U)
      c.clock.step()

      c.io.redirect.valid.poke(false.B)
      c.io.lookupPc.valid.expect(true.B)
      c.io.lookupPc.bits.expect(target.U)
      c.clock.step()

      c.io.cacheResp.word.poke(targetInst.U)
      c.clock.step()
      c.clock.step()
      c.io.out.valid.expect(true.B)
      c.io.out.bits.execData.pc.expect(target.U)
      c.io.out.bits.wbCtrl.rd.expect(5.U)
    }
  }
}
