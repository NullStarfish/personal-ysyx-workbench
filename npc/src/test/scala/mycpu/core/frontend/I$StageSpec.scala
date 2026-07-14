package mycpu.core.frontend

import chisel3._
import chisel3.simulator.EphemeralSimulator._
import mycpu.cache.CacheParams
import org.scalatest.flatspec.AnyFlatSpec

class I$0StageSpec extends AnyFlatSpec {
  private val params = CacheParams(lineBytes = 16, sets = 4, ways = 1)

  private def init(c: I$0Stage): Unit = {
    c.io.in.valid.poke(false.B)
    c.io.in.bits.pc.poke(0.U)
    c.io.cacheSetResp.hit.poke(false.B)
    c.io.cacheSetResp.way.poke(0.U)
    c.io.cacheSetResp.selectedValid.poke(false.B)
    c.io.cacheSetResp.storedTag.poke(0.U)
    c.io.cacheSetResp.line.poke(0.U)
    c.io.cacheSetResp.word.poke(0.U)
    c.io.out.ready.poke(false.B)
    c.io.flush.poke(false.B)
  }

  private def resetDut(c: I$0Stage): Unit = {
    init(c)
    c.reset.poke(true.B)
    c.clock.step()
    c.reset.poke(false.B)
  }

  "I$0" should "consume the external pipeline entry when capturing skid" in {
    simulate(new I$0Stage(params)) { c =>
      resetDut(c)

      c.io.in.valid.poke(true.B)
      c.io.in.bits.pc.poke("h100".U)
      c.io.cacheSetResp.hit.poke(true.B)
      c.io.cacheSetResp.word.poke("h11111111".U)
      c.io.out.ready.poke(false.B)

      // 下游停住时，旧entry仍然必须fire并被skid接住。
      c.io.in.ready.expect(true.B)
      c.io.blockFetch.expect(true.B)
      c.clock.step()

      c.io.in.valid.poke(false.B)
      c.io.out.valid.expect(true.B)
      c.io.out.bits.pc.expect("h100".U)
      c.io.out.bits.icacheResp.word.expect("h11111111".U)
      c.io.in.ready.expect(false.B)
      c.io.blockFetch.expect(true.B)

      c.io.out.ready.poke(true.B)
      c.io.blockFetch.expect(false.B)
      c.clock.step()
      c.io.out.valid.expect(false.B)
    }
  }

  it should "pass a hit directly when the external I$0/I$1 register is ready" in {
    simulate(new I$0Stage(params)) { c =>
      resetDut(c)

      c.io.in.valid.poke(true.B)
      c.io.in.bits.pc.poke("h100".U)
      c.io.cacheSetResp.hit.poke(true.B)
      c.io.cacheSetResp.word.poke("h12345678".U)
      c.io.out.ready.poke(true.B)

      c.io.in.ready.expect(true.B)
      c.io.out.valid.expect(true.B)
      c.io.out.bits.pc.expect("h100".U)
      c.io.out.bits.icacheResp.word.expect("h12345678".U)
      c.io.blockFetch.expect(false.B)
    }
  }

  it should "treat an accepted miss like any other Decoupled packet" in {
    simulate(new I$0Stage(params)) { c =>
      resetDut(c)

      c.io.in.valid.poke(true.B)
      c.io.in.bits.pc.poke("h100".U)
      c.io.cacheSetResp.hit.poke(false.B)
      c.io.out.ready.poke(true.B)

      c.io.in.ready.expect(true.B)
      c.io.out.valid.expect(true.B)
      c.io.blockFetch.expect(false.B)
    }
  }

  it should "drop a buffered skid packet on redirect flush" in {
    simulate(new I$0Stage(params)) { c =>
      resetDut(c)

      c.io.in.valid.poke(true.B)
      c.io.in.bits.pc.poke("h100".U)
      c.io.cacheSetResp.hit.poke(true.B)
      c.io.cacheSetResp.word.poke("hdeadbeef".U)
      c.io.out.ready.poke(false.B)
      c.clock.step()

      c.io.in.valid.poke(false.B)
      c.io.flush.poke(true.B)
      c.io.in.ready.expect(false.B)
      c.clock.step()

      c.io.flush.poke(false.B)
      c.io.in.valid.poke(false.B)
      c.io.out.valid.expect(false.B)
      c.io.in.ready.expect(true.B)
    }
  }
}

class I$1StageSpec extends AnyFlatSpec {
  private val params = CacheParams(lineBytes = 16, sets = 4, ways = 1)

  private def init(c: I$1Stage): Unit = {
    c.io.in.valid.poke(false.B)
    c.io.in.bits.pc.poke(0.U)
    c.io.in.bits.icacheResp.hit.poke(false.B)
    c.io.in.bits.icacheResp.way.poke(0.U)
    c.io.in.bits.icacheResp.selectedValid.poke(false.B)
    c.io.in.bits.icacheResp.storedTag.poke(0.U)
    c.io.in.bits.icacheResp.line.poke(0.U)
    c.io.in.bits.icacheResp.word.poke(0.U)
    c.io.out.ready.poke(false.B)
    c.io.mem.a.ready.poke(false.B)
    c.io.mem.r.valid.poke(false.B)
    c.io.mem.r.bits.data.poke(0.U)
    c.io.mem.r.bits.resp.poke(0.U)
    c.io.mem.r.bits.last.poke(false.B)
    c.io.mem.r.bits.id.poke(0.U)
    c.io.flush.poke(false.B)
    c.io.fencei.poke(false.B)
  }

  private def resetDut(c: I$1Stage): Unit = {
    init(c)
    c.reset.poke(true.B)
    c.clock.step()
    c.reset.poke(false.B)
  }

  private def acceptMiss(c: I$1Stage, pc: BigInt): Unit = {
    c.io.in.valid.poke(true.B)
    c.io.in.bits.pc.poke(pc.U)
    c.io.in.bits.icacheResp.hit.poke(false.B)
    c.io.in.ready.expect(true.B)
    c.clock.step()
    c.io.in.valid.poke(false.B)

    c.io.mem.a.valid.expect(true.B)
    c.io.mem.a.bits.addr.expect((pc & ~(BigInt(params.lineBytes) - 1)).U)
    c.io.mem.a.bits.len.expect((params.wordsPerLine - 1).U)
    c.io.mem.a.ready.poke(true.B)
    c.clock.step()
    c.io.mem.a.ready.poke(false.B)
  }

  private def completeRefill(c: I$1Stage, words: Seq[BigInt]): Unit = {
    for ((word, beat) <- words.zipWithIndex) {
      c.io.mem.r.valid.poke(true.B)
      c.io.mem.r.bits.data.poke(word.U)
      c.io.mem.r.bits.last.poke((beat == words.size - 1).B)
      c.clock.step()
    }
    c.io.mem.r.valid.poke(false.B)
    c.io.out.ready.poke(true.B)
    c.io.out.valid.expect(true.B)
    c.clock.step()
    c.io.out.ready.poke(false.B)
  }

  "I$1" should "refill the selected line and hold the miss response for Decode" in {
    simulate(new I$1Stage(params)) { c =>
      val pc = BigInt("104", 16)
      val words = Seq(
        BigInt("11111111", 16),
        BigInt("22222222", 16),
        BigInt("33333333", 16),
        BigInt("44444444", 16),
      )
      resetDut(c)
      acceptMiss(c, pc)

      for ((word, beat) <- words.zipWithIndex) {
        c.io.mem.r.valid.poke(true.B)
        c.io.mem.r.bits.data.poke(word.U)
        c.io.mem.r.bits.last.poke((beat == words.size - 1).B)
        if (beat == words.size - 1) {
          c.io.cacheSetWrite.valid.expect(true.B)
          c.io.cacheSetWrite.bits.index.expect(((pc >> params.offsetWidth) & (params.sets - 1)).U)
          c.io.cacheSetWrite.bits.meta.tag.expect((pc >> (params.offsetWidth + params.indexWidth)).U)
          c.io.cacheSetWrite.bits.data.expect(words.reverse.foldLeft(BigInt(0))((line, data) => (line << 32) | data).U)
        }
        c.clock.step()
      }
      c.io.mem.r.valid.poke(false.B)

      c.io.out.valid.expect(true.B)
      c.io.out.bits.pc.expect(pc.U)
      c.io.out.bits.inst.expect(words(1).U)
      c.clock.step(2)
      c.io.out.valid.expect(true.B)
      c.io.out.bits.inst.expect(words(1).U)

      c.io.out.ready.poke(true.B)
      c.clock.step()
      c.io.out.valid.expect(false.B)

      // 这条查询发生在refill完成前，packet中的miss已经过期，应命中refill buffer。
      c.io.in.valid.poke(true.B)
      c.io.in.bits.pc.poke("h108".U)
      c.io.in.bits.icacheResp.hit.poke(false.B)
      c.io.in.ready.expect(true.B)
      c.io.out.valid.expect(true.B)
      c.io.out.bits.inst.expect(words(2).U)
      c.io.mem.a.valid.expect(false.B)
    }
  }

  it should "retain the two most recently refilled lines" in {
    simulate(new I$1Stage(params)) { c =>
      val firstWords = Seq.tabulate(params.wordsPerLine)(beat => BigInt("10000000", 16) + beat)
      val secondWords = Seq.tabulate(params.wordsPerLine)(beat => BigInt("20000000", 16) + beat)
      resetDut(c)

      acceptMiss(c, BigInt("104", 16))
      completeRefill(c, firstWords)
      acceptMiss(c, BigInt("114", 16))
      completeRefill(c, secondWords)

      // 两个packet都带着refill前产生的miss响应，不能再次访问memory。
      c.io.out.ready.poke(true.B)
      c.io.in.valid.poke(true.B)
      c.io.in.bits.icacheResp.hit.poke(false.B)

      c.io.in.bits.pc.poke("h108".U)
      c.io.out.valid.expect(true.B)
      c.io.out.bits.inst.expect(firstWords(2).U)
      c.io.mem.a.valid.expect(false.B)
      c.clock.step()

      c.io.in.bits.pc.poke("h11c".U)
      c.io.out.valid.expect(true.B)
      c.io.out.bits.inst.expect(secondWords(3).U)
      c.io.mem.a.valid.expect(false.B)
    }
  }

  it should "ignore hit metadata while the input packet is invalid" in {
    simulate(new I$1Stage(params)) { c =>
      resetDut(c)

      c.io.in.valid.poke(false.B)
      c.io.in.bits.icacheResp.hit.poke(false.B)
      c.io.in.ready.expect(true.B)

      c.io.in.bits.icacheResp.hit.poke(true.B)
      c.io.in.ready.expect(true.B)
    }
  }

  it should "drain and fill CacheSet after redirect without replying" in {
    simulate(new I$1Stage(params)) { c =>
      resetDut(c)
      acceptMiss(c, BigInt("104", 16))

      c.io.flush.poke(true.B)
      c.clock.step()
      c.io.flush.poke(false.B)

      for (beat <- 0 until params.wordsPerLine) {
        c.io.mem.r.valid.poke(true.B)
        c.io.mem.r.bits.data.poke((beat + 1).U)
        c.io.mem.r.bits.last.poke((beat == params.wordsPerLine - 1).B)
        c.io.cacheSetWrite.valid.expect((beat == params.wordsPerLine - 1).B)
        c.clock.step()
      }
      c.io.mem.r.valid.poke(false.B)

      c.io.out.valid.expect(false.B)
      c.io.mem.r.ready.expect(false.B)
    }
  }

  it should "drain but not write an old refill after fence.i" in {
    simulate(new I$1Stage(params)) { c =>
      resetDut(c)
      acceptMiss(c, BigInt("104", 16))

      c.io.flush.poke(true.B)
      c.io.fencei.poke(true.B)
      c.clock.step()
      c.io.flush.poke(false.B)
      c.io.fencei.poke(false.B)

      for (beat <- 0 until params.wordsPerLine) {
        c.io.mem.r.valid.poke(true.B)
        c.io.mem.r.bits.data.poke((beat + 1).U)
        c.io.mem.r.bits.last.poke((beat == params.wordsPerLine - 1).B)
        c.io.cacheSetWrite.valid.expect(false.B)
        c.clock.step()
      }
      c.io.mem.r.valid.poke(false.B)

      c.io.out.valid.expect(false.B)
      c.io.mem.r.ready.expect(false.B)
      c.io.cacheSetWrite.valid.expect(false.B)
    }
  }
}
