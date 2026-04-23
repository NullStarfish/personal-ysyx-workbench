package mycpu.core.components

import chisel3._
import chisel3.simulator.EphemeralSimulator._
import org.scalatest.flatspec.AnyFlatSpec

class GShareBranchPredictorSpec extends AnyFlatSpec {
  private def init(c: GShareBranchPredictor): Unit = {
    c.reset.poke(true.B)
    c.io.pc.poke(0.U)
    c.io.update.poke(false.B)
    c.io.updateIndex.poke(0.U)
    c.io.actualTaken.poke(false.B)
    c.io.predictedTaken.poke(false.B)
    c.clock.step()
    c.reset.poke(false.B)
  }

  private def update(c: GShareBranchPredictor, pc: BigInt, taken: Boolean): Boolean = {
    c.io.pc.poke(pc.U)
    c.io.update.poke(false.B)
    val predicted = c.io.predictTaken.peek().litToBoolean
    val index = c.io.predictIndex.peek()
    c.io.updateIndex.poke(index)
    c.io.predictedTaken.poke(predicted.B)
    c.io.actualTaken.poke(taken.B)
    c.io.update.poke(true.B)
    c.clock.step()
    c.io.update.poke(false.B)
    predicted
  }

  "GShareBranchPredictor" should "start weakly not taken" in {
    simulate(new GShareBranchPredictor(entries = 32, historyLength = 5)) { c =>
      init(c)
      c.io.pc.poke("h30000040".U)
      c.io.predictTaken.expect(false.B)
    }
  }

  it should "learn a mostly taken hot branch" in {
    simulate(new GShareBranchPredictor(entries = 32, historyLength = 5)) { c =>
      init(c)
      val pc = BigInt("30000080", 16)

      for (_ <- 0 until 12) update(c, pc, taken = true)

      c.io.pc.poke(pc.U)
      c.io.update.poke(false.B)
      c.io.predictTaken.expect(true.B)
    }
  }

  it should "reach a high prediction rate on a loop-exit style branch" in {
    simulate(new GShareBranchPredictor(entries = 32, historyLength = 5)) { c =>
      init(c)
      val pc = BigInt("30000120", 16)
      var correct = 0
      var total = 0

      for (iter <- 0 until 96) {
        val taken = (iter % 24) == 23
        val predicted = update(c, pc, taken)
        if (iter >= 24) {
          total += 1
          if (predicted == taken) correct += 1
        }
      }

      assert(correct >= 66, s"expected at least 66 / $total correct predictions, got $correct")
    }
  }
}
