package mycpu.core.frontend

import chisel3._
import chisel3.simulator.EphemeralSimulator._
import chisel3.util._
import mycpu.common._
import mycpu.core.bundles._
import mycpu.core.components.FlushableStage
import org.scalatest.flatspec.AnyFlatSpec

class FetchDecodeHarness extends Module {
  val io = IO(new Bundle {
    val fetchReq = Decoupled(UInt(32.W))
    val reply = Flipped(Decoupled(UInt(32.W)))
    val redirect = Input(Valid(UInt(XLEN.W)))
    val flush = Input(Bool())
    val stageStall = Input(Bool())
    val out = Decoupled(new DecodePacket)
    val regWrite = Flipped(new WriteBackIO)
  })

  val fetch = Module(new Fetch)
  val ifId = Module(new FlushableStage(new FetchPacket))
  val decode = Module(new Decode)

  io.fetchReq <> fetch.io.fetch
  fetch.io.reply <> io.reply
  fetch.io.redirect := io.redirect

  ifId.io.enq <> fetch.io.out
  ifId.io.flush := io.flush
  ifId.io.stall := io.stageStall

  decode.io.in <> ifId.io.deq
  decode.io.regWrite <> io.regWrite
  decode.io.forwards.foreach { forward =>
    forward.valid := false.B
    forward.addr := 0.U
    forward.data := 0.U
  }
  io.out <> decode.io.out
}

class FetchDecodeSim extends AnyFlatSpec {
  private val maxWait = 30
  private val pc0 = BigInt(START_ADDR)
  private val pc1 = pc0 + 4
  private val pc2 = pc0 + 8
  private val target = pc0 + 0x40

  private def iType(imm: Int, rs1: Int, funct3: Int, rd: Int, opcode: Int): BigInt =
    ((BigInt(imm) & 0xfff) << 20) |
      (BigInt(rs1 & 0x1f) << 15) |
      (BigInt(funct3 & 0x7) << 12) |
      (BigInt(rd & 0x1f) << 7) |
      BigInt(opcode & 0x7f)

  private def bType(imm: Int, rs2: Int, rs1: Int, funct3: Int): BigInt = {
    val value = imm & 0x1fff
    (BigInt((value >> 12) & 0x1) << 31) |
      (BigInt((value >> 5) & 0x3f) << 25) |
      (BigInt(rs2 & 0x1f) << 20) |
      (BigInt(rs1 & 0x1f) << 15) |
      (BigInt(funct3 & 0x7) << 12) |
      (BigInt((value >> 1) & 0xf) << 8) |
      (BigInt((value >> 11) & 0x1) << 7) |
      BigInt("1100011", 2)
  }

  private def addi(rd: Int, rs1: Int, imm: Int): BigInt =
    iType(imm, rs1, funct3 = 0, rd, opcode = BigInt("0010011", 2).toInt)

  private def lw(rd: Int, rs1: Int, imm: Int): BigInt =
    iType(imm, rs1, funct3 = 2, rd, opcode = BigInt("0000011", 2).toInt)

  private def beq(rs1: Int, rs2: Int, imm: Int): BigInt =
    bType(imm, rs2, rs1, funct3 = 0)

  private def init(c: FetchDecodeHarness): Unit = {
    c.io.fetchReq.ready.poke(false.B)
    c.io.reply.valid.poke(false.B)
    c.io.reply.bits.poke(0.U)
    c.io.redirect.valid.poke(false.B)
    c.io.redirect.bits.poke(0.U)
    c.io.flush.poke(false.B)
    c.io.stageStall.poke(false.B)
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

  private def acceptFetch(c: FetchDecodeHarness, pc: BigInt): Unit = {
    var cycles = 0
    c.io.fetchReq.ready.poke(false.B)
    while (c.io.fetchReq.valid.peek().litValue == 0 && cycles < maxWait) {
      c.clock.step()
      cycles += 1
    }
    assert(cycles < maxWait, s"FetchDecode did not present fetch request for 0x${pc.toString(16)}")
    c.io.fetchReq.bits.expect(pc.U)
    c.io.fetchReq.ready.poke(true.B)
    c.clock.step()
    c.io.fetchReq.ready.poke(false.B)
  }

  private def returnInst(c: FetchDecodeHarness, inst: BigInt): Unit = {
    var cycles = 0
    c.io.reply.valid.poke(true.B)
    c.io.reply.bits.poke(inst.U)
    while (c.io.reply.ready.peek().litValue == 0 && cycles < maxWait) {
      c.clock.step()
      cycles += 1
    }
    assert(cycles < maxWait, s"FetchDecode did not accept reply 0x${inst.toString(16)}")
    c.clock.step()
    c.io.reply.valid.poke(false.B)
  }

  private def waitOut(c: FetchDecodeHarness, pc: BigInt): Unit = {
    var cycles = 0
    while (c.io.out.valid.peek().litValue == 0 && cycles < maxWait) {
      c.clock.step()
      cycles += 1
    }
    assert(cycles < maxWait, s"FetchDecode did not emit decoded packet for 0x${pc.toString(16)}")
    c.io.out.bits.execData.pc.expect(pc.U)
  }

  "Fetch + Decode" should "flow two sequential instructions through IF/ID" in {
    simulate(new FetchDecodeHarness) { c =>
      val inst0 = addi(rd = 1, rs1 = 0, imm = 5)
      val inst1 = lw(rd = 2, rs1 = 1, imm = 8)

      resetDut(c)
      c.io.out.ready.poke(true.B)

      acceptFetch(c, pc0)
      returnInst(c, inst0)

      waitOut(c, pc0)
      c.io.out.bits.execData.imm.expect(5.U)
      c.io.out.bits.wbCtrl.wen.expect(true.B)
      c.io.out.bits.wbCtrl.rd.expect(1.U)
      c.io.out.bits.memCtrl.en.expect(false.B)
      c.io.out.bits.retireTrace.get.instType.expect(InstType.arith)

      acceptFetch(c, pc1)
      returnInst(c, inst1)

      waitOut(c, pc1)
      c.io.out.bits.rs1.valid.expect(true.B)
      c.io.out.bits.rs1.bits.addr.expect(1.U)
      c.io.out.bits.execData.imm.expect(8.U)
      c.io.out.bits.wbCtrl.wen.expect(true.B)
      c.io.out.bits.wbCtrl.rd.expect(2.U)
      c.io.out.bits.memCtrl.en.expect(true.B)
      c.io.out.bits.memCtrl.write.expect(false.B)
      c.io.out.bits.memCtrl.subop.expect(SizeSubop.Word)
      c.io.out.bits.retireTrace.get.instType.expect(InstType.mem)
    }
  }

  it should "hold decoded output stable while downstream is not ready" in {
    simulate(new FetchDecodeHarness) { c =>
      val inst0 = addi(rd = 3, rs1 = 0, imm = 9)
      val inst1 = addi(rd = 4, rs1 = 0, imm = 10)

      resetDut(c)

      acceptFetch(c, pc0)
      c.io.out.ready.poke(false.B)
      returnInst(c, inst0)

      waitOut(c, pc0)

      acceptFetch(c, pc1)
      returnInst(c, inst1)
      waitOut(c, pc0)
      c.io.out.bits.execData.pc.expect(pc0.U)
      c.io.out.bits.wbCtrl.rd.expect(3.U)

      c.io.out.ready.poke(true.B)
      c.clock.step()

      waitOut(c, pc1)
      c.io.out.bits.wbCtrl.rd.expect(4.U)
    }
  }

  it should "stall IF/ID enqueue without hiding the decoded output" in {
    simulate(new FetchDecodeHarness) { c =>
      val inst0 = addi(rd = 4, rs1 = 0, imm = 1)

      resetDut(c)

      acceptFetch(c, pc0)
      returnInst(c, inst0)
      waitOut(c, pc0)

      c.io.stageStall.poke(true.B)
      c.io.out.ready.poke(false.B)
      waitOut(c, pc0)

      acceptFetch(c, pc1)
      returnInst(c, addi(rd = 5, rs1 = 0, imm = 2))
      waitOut(c, pc0)

      c.io.stageStall.poke(false.B)
      c.io.out.ready.poke(true.B)
      c.clock.step()
      waitOut(c, pc1)
    }
  }

  it should "flush the IF/ID stage and redirect fetch to the target" in {
    simulate(new FetchDecodeHarness) { c =>
      val wrongPath = addi(rd = 6, rs1 = 0, imm = 6)
      val targetInst = beq(rs1 = 1, rs2 = 2, imm = 12)

      resetDut(c)
      c.io.out.ready.poke(false.B)

      acceptFetch(c, pc0)
      returnInst(c, wrongPath)
      waitOut(c, pc0)

      c.io.redirect.valid.poke(true.B)
      c.io.redirect.bits.poke(target.U)
      c.io.flush.poke(true.B)
      c.clock.step()
      c.io.redirect.valid.poke(false.B)
      c.io.flush.poke(false.B)

      c.io.out.valid.expect(false.B)
      acceptFetch(c, target)

      c.io.out.ready.poke(true.B)
      returnInst(c, targetInst)
      waitOut(c, target)
      c.io.out.bits.execCtrl.branchType.expect(BranchType.Eq)
      c.io.out.bits.retireTrace.get.instType.expect(InstType.redirect)
    }
  }
}
