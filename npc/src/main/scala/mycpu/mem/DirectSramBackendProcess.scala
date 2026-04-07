package mycpu.mem

import HwOS.kernel._
import chisel3._
import chisel3.util._
import mycpu.common._

class DirectSramBackendProcess(
    readonlyWords: Seq[(BigInt, BigInt)],
    mutableWords: Seq[(BigInt, BigInt)],
    localName: String = "DirectSramBackend",
)(implicit kernel: Kernel)
    extends HwProcess(localName) {

  require(mutableWords.nonEmpty, "DirectSramBackendProcess requires at least one mutable word")

  private val mutableAddrRegs = mutableWords.map { case (addr, _) => addr.U(XLEN.W) }
  private val mutableDataRegs = mutableWords.map { case (_, init) => RegInit(init.U(XLEN.W)) }

  val mutableData: Seq[UInt] = mutableDataRegs

  private def readDataAt(addr: UInt): UInt = {
    val readonlyEntries = readonlyWords.map { case (a, d) => a.U(XLEN.W) -> d.U(XLEN.W) }
    val mutableEntries = mutableAddrRegs.zip(mutableDataRegs)
    MuxLookup(addr, 0.U(XLEN.W))(readonlyEntries ++ mutableEntries)
  }

  val api: MemoryBackendApiDecl = new MemoryBackendApiDecl {
    override def read_once(addr: UInt, size: UInt): HwInline[UInt] = HwInline.thread("direct_mem_read") { t =>
      val readData = WireDefault(readDataAt(addr))
      t.Step("Read") {}
      readData
    }

    override def write_once(addr: UInt, size: UInt, data: UInt, strb: UInt): HwInline[Unit] =
      HwInline.thread("direct_mem_write") { t =>
        t.Step("Write") {
          for ((addrReg, dataReg) <- mutableAddrRegs.zip(mutableDataRegs)) {
            when(addr === addrReg) {
              val mergedBytes = Wire(Vec(XLEN / 8, UInt(8.W)))
              for (byteIdx <- 0 until (XLEN / 8)) {
                mergedBytes(byteIdx) := Mux(
                  strb(byteIdx),
                  data(byteIdx * 8 + 7, byteIdx * 8),
                  dataReg(byteIdx * 8 + 7, byteIdx * 8),
                )
              }
              dataReg := mergedBytes.asUInt
            }
          }
        }
      }
  }
}
