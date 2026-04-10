package mycpu.core.components

import chisel3._
import chisel3.simulator.EphemeralSimulator._
import org.scalatest.flatspec.AnyFlatSpec

class PerceptronBranchPredictorSpec extends AnyFlatSpec {
  private case class BranchEvent(pc: BigInt, taken: Boolean)

  private def init(c: PerceptronBranchPredictor): Unit = {
    c.io.pc.poke(0.U)
    c.io.update.poke(false.B)
    c.io.updatePc.poke(0.U)
    c.io.actualTaken.poke(false.B)
    c.io.predictedTaken.poke(false.B)
  }

  private def train(c: PerceptronBranchPredictor, pc: BigInt, taken: Boolean, rounds: Int): Unit = {
    var i = 0
    while (i < rounds) {
      c.io.pc.poke(pc.U)
      c.io.update.poke(false.B)
      val predicted = c.io.predictTaken.peek().litToBoolean
      c.io.updatePc.poke(pc.U)
      c.io.predictedTaken.poke((if (predicted) true.B else false.B))
      c.io.actualTaken.poke(taken.B)
      c.io.update.poke(true.B)
      c.clock.step()
      c.io.update.poke(false.B)
      i += 1
    }
    c.io.pc.poke(pc.U)
    c.io.update.poke(false.B)
  }

  private def runTrace(
      c: PerceptronBranchPredictor,
      trace: Seq[BranchEvent],
      warmupEvents: Int,
      updateDuringMeasure: Boolean = true,
  ): Double = {
    var correct = 0
    var measured = 0
    trace.zipWithIndex.foreach { case (event, idx) =>
      c.io.pc.poke(event.pc.U)
      c.io.update.poke(false.B)
      val predicted = c.io.predictTaken.peek().litToBoolean
      if (idx >= warmupEvents) {
        measured += 1
        if (predicted == event.taken) correct += 1
      }

      if (updateDuringMeasure || idx < warmupEvents) {
        c.io.updatePc.poke(event.pc.U)
        c.io.predictedTaken.poke((if (predicted) true.B else false.B))
        c.io.actualTaken.poke(event.taken.B)
        c.io.update.poke(true.B)
        c.clock.step()
        c.io.update.poke(false.B)
      }
    }
    if (measured == 0) 1.0 else correct.toDouble / measured.toDouble
  }

  "PerceptronBranchPredictor" should "expose a stable initial prediction and score" in {
    simulate(new PerceptronBranchPredictor(entries = 8, historyLength = 4)) { c =>
      init(c)
      c.io.pc.poke("h30000000".U)
      val firstPrediction = c.io.predictTaken.peek().litToBoolean
      val firstScore = c.io.score.peek().litValue
      c.io.predictTaken.expect((if (firstPrediction) true.B else false.B))
      assert(c.io.score.peek().litValue == firstScore)
    }
  }

  it should "learn an always-not-taken branch" in {
    simulate(new PerceptronBranchPredictor(entries = 8, historyLength = 8)) { c =>
      init(c)

      train(c, pc = BigInt("30000040", 16), taken = false, rounds = 8)

      c.io.pc.poke("h30000040".U)
      c.io.predictTaken.expect(false.B)
    }
  }

  it should "keep different PCs in different perceptron entries" in {
    simulate(new PerceptronBranchPredictor(entries = 16, historyLength = 8)) { c =>
      init(c)

      train(c, pc = BigInt("30000084", 16), taken = true, rounds = 6)
      train(c, pc = BigInt("30000088", 16), taken = false, rounds = 8)

      c.io.pc.poke("h30000084".U)
      c.io.predictTaken.expect(true.B)

      c.io.pc.poke("h30000088".U)
      c.io.predictTaken.expect(false.B)
    }
  }

  it should "reach a good prediction rate on a realistic branch trace" in {
    simulate(new PerceptronBranchPredictor(entries = 32, historyLength = 8)) { c =>
      init(c)

      val boundsCheckPc = BigInt("30000120", 16)
      train(c, pc = boundsCheckPc, taken = false, rounds = 12)

      val trace =
        (0 until 96).map { iter =>
          // A hot bounds-check branch: overwhelmingly not taken, with rare slow-path hits.
          BranchEvent(boundsCheckPc, taken = (iter % 24) == 23)
        }

      val accuracy = runTrace(c, trace, warmupEvents = 0, updateDuringMeasure = false)
      assert(accuracy >= 0.90, s"expected >= 90% accuracy on a bounds-check trace, got ${accuracy * 100}%.")
    }
  }
}
