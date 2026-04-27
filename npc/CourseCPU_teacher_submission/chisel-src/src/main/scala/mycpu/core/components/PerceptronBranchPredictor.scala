package mycpu.core.components

import chisel3._
import chisel3.util._
import mycpu.common._

class PerceptronBranchPredictor(
    entries: Int = 32,
    historyLength: Int = 8,
    weightWidth: Int = 8,
) extends Module {
  require(entries > 0 && isPow2(entries))
  require(historyLength > 0)
  require(weightWidth >= 2)

  private val indexWidth = log2Ceil(entries)
  private val scoreWidth = weightWidth + log2Ceil(historyLength + 1) + 2
  private val weightMax = ((1 << (scoreWidth - 1)) - 1).S(scoreWidth.W)
  private val weightMin = (-(1 << (scoreWidth - 1))).S(scoreWidth.W)

  val io = IO(new Bundle {
    val pc = Input(UInt(XLEN.W))
    val predictTaken = Output(Bool())
    val score = Output(SInt(scoreWidth.W))

    val update = Input(Bool())
    val updatePc = Input(UInt(XLEN.W))
    val actualTaken = Input(Bool())
    val predictedTaken = Input(Bool())
  })

  val historyReg = RegInit(0.U(historyLength.W))
  val biasTable = RegInit(VecInit(Seq.fill(entries)(1.S(scoreWidth.W))))
  val weightTable = RegInit(VecInit(Seq.fill(entries)(VecInit(Seq.fill(historyLength)(0.S(scoreWidth.W))))))

  private def pcIndex(pc: UInt): UInt = pc(indexWidth + 1, 2)
  private def saturatingInc(x: SInt): SInt = Mux(x === weightMax, x, (x + 1.S(scoreWidth.W)).asSInt)
  private def saturatingDec(x: SInt): SInt = Mux(x === weightMin, x, (x - 1.S(scoreWidth.W)).asSInt)
  private def historyAsSign(bit: Bool): SInt = Mux(bit, 1.S(scoreWidth.W), (-1).S(scoreWidth.W))

  val readIndex = pcIndex(io.pc)
  val perceptronTerms = Wire(Vec(historyLength, SInt(scoreWidth.W)))
  for (i <- 0 until historyLength) {
    perceptronTerms(i) := Mux(historyReg(i), weightTable(readIndex)(i), (-weightTable(readIndex)(i)).asSInt)
  }
  val currentScore = perceptronTerms.foldLeft(biasTable(readIndex))(_ + _)
  io.score := currentScore
  io.predictTaken := currentScore >= 0.S

  when(io.update) {
    val updateIndex = pcIndex(io.updatePc)
    val oldBias = biasTable(updateIndex)
    val oldWeights = weightTable(updateIndex)
    val actualSign = Mux(io.actualTaken, 1.S(scoreWidth.W), (-1).S(scoreWidth.W))

    biasTable(updateIndex) := Mux(io.actualTaken, saturatingInc(oldBias), saturatingDec(oldBias))
    for (i <- 0 until historyLength) {
      val historySign = historyAsSign(historyReg(i))
      val agrees = historySign === actualSign
      weightTable(updateIndex)(i) := Mux(agrees, saturatingInc(oldWeights(i)), saturatingDec(oldWeights(i)))
    }

    if (historyLength == 1) {
      historyReg := io.actualTaken
    } else {
      historyReg := Cat(historyReg(historyLength - 2, 0), io.actualTaken)
    }
  }
}
