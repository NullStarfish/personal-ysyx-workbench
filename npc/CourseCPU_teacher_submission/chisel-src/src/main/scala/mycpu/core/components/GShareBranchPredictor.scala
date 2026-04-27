package mycpu.core.components

import chisel3._
import chisel3.util._
import mycpu.common._

class GShareBranchPredictor(
    entries: Int = 32,
    historyLength: Int = 5,
) extends Module {
  require(entries > 0 && isPow2(entries))
  require(historyLength > 0 && historyLength <= log2Ceil(entries))

  private val indexWidth = log2Ceil(entries)

  val io = IO(new Bundle {
    val pc = Input(UInt(XLEN.W))
    val predictTaken = Output(Bool())
    val predictIndex = Output(UInt(indexWidth.W))

    val update = Input(Bool())
    val updateIndex = Input(UInt(indexWidth.W))
    val actualTaken = Input(Bool())
    val predictedTaken = Input(Bool())
  })

  val historyReg = RegInit(0.U(historyLength.W))
  val counters = RegInit(VecInit(Seq.fill(entries)(1.U(2.W)))) // weakly not taken

  private def pcIndex(pc: UInt): UInt = pc(indexWidth + 1, 2)
  private def gshareIndex(pc: UInt): UInt = {
    val history = if (historyLength == indexWidth) {
      historyReg
    } else {
      Cat(0.U((indexWidth - historyLength).W), historyReg)
    }
    pcIndex(pc) ^ history
  }

  val readIndex = gshareIndex(io.pc)
  io.predictIndex := readIndex
  io.predictTaken := counters(readIndex)(1)

  when(io.update) {
    val oldCounter = counters(io.updateIndex)
    counters(io.updateIndex) := Mux(
      io.actualTaken,
      Mux(oldCounter === 3.U, oldCounter, oldCounter + 1.U),
      Mux(oldCounter === 0.U, oldCounter, oldCounter - 1.U),
    )

    if (historyLength == 1) {
      historyReg := io.actualTaken
    } else {
      historyReg := Cat(historyReg(historyLength - 2, 0), io.actualTaken)
    }
  }
}
