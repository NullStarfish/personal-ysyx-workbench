package mycpu.mem

import HwOS.kernel._
import chisel3._
import chisel3.util._
import mycpu.axi._
import mycpu.common._

class DummyAxiSram(
    bus: AXI4Bundle,
    readonlyWords: Seq[(BigInt, BigInt)],
    mutableWords: Seq[(BigInt, BigInt)],
    localName: String = "DummyAxiSram",
)(implicit kernel: Kernel)
    extends HwProcess(localName) {

  require(mutableWords.nonEmpty, "DummyAxiSram requires at least one mutable word")

  private val readWorker = createThread("ReadWorker")
  private val writeAddrListener = createThread("WriteAddrListener")
  private val writeDataListener = createThread("WriteDataListener")
  private val writeRespWorker = createThread("WriteRespWorker")
  private val daemon = createLogic("Daemon")

  private val mutableAddrRegs = mutableWords.map { case (addr, _) => addr.U(XLEN.W) }
  private val mutableDataRegs = mutableWords.map { case (_, init) => RegInit(init.U(XLEN.W)) }

  val mutableData: Seq[UInt] = mutableDataRegs

  private val writeAddrReg = RegInit(0.U(XLEN.W))
  private val writeIdReg = RegInit(0.U(AXI_ID_WIDTH.W))
  private val writeDataReg = RegInit(0.U(XLEN.W))
  private val writeStrbReg = RegInit(0.U((XLEN / 8).W))
  private val writeAddrSeen = RegInit(false.B)
  private val writeDataSeen = RegInit(false.B)
  private val readReqAddrReg = RegInit(0.U(XLEN.W))
  private val readRespIdReg = RegInit(0.U(AXI_ID_WIDTH.W))
  private val readRespDataReg = RegInit(0.U(XLEN.W))

  private def readDataAt(addr: UInt): UInt = {
    val readonlyEntries = readonlyWords.map { case (a, d) => a.U(XLEN.W) -> d.U(XLEN.W) }
    val mutableEntries = mutableAddrRegs.zip(mutableDataRegs)
    MuxLookup(addr, 0.U(XLEN.W))(readonlyEntries ++ mutableEntries)
  }

  override def entry(): Unit = {
    readWorker.entry {
      val req = SysCall.Inline(AXI4SlaveApi.axi_listen_read_addr_once(bus))
      readWorker.Prev.edge.add {
        readReqAddrReg := req.addr
        readRespIdReg := req.id
      }
      readWorker.Step("PrepareReadResp") {
        readRespDataReg := readDataAt(readReqAddrReg)
      }
      readWorker.Step("SendReadResp") {}
      SysCall.Inline(AXI4SlaveApi.axi_send_read_data_once(bus, readRespIdReg, readRespDataReg))
      SysCall.Return()
    }

    writeAddrListener.entry {
      val reqA = SysCall.Inline(AXI4SlaveApi.axi_listen_write_addr_once(bus))
      writeAddrListener.Prev.edge.add {
        writeAddrReg := reqA.addr
        writeIdReg := reqA.id
        writeAddrSeen := true.B
      }
      SysCall.Return()
    }

    writeDataListener.entry {
      val reqW = SysCall.Inline(AXI4SlaveApi.axi_listen_write_data_once(bus))
      writeDataListener.Prev.edge.add {
        writeDataReg := reqW.data
        writeStrbReg := reqW.strb
        writeDataSeen := true.B
      }
      SysCall.Return()
    }

    writeRespWorker.entry {
      writeRespWorker.Step("WaitWriteReq") {
        writeRespWorker.waitCondition(writeAddrSeen && writeDataSeen)
      }
      writeRespWorker.Step("CommitWrite") {
        for ((addrReg, dataReg) <- mutableAddrRegs.zip(mutableDataRegs)) {
          when(writeAddrReg === addrReg) {
            val mergedBytes = Wire(Vec(XLEN / 8, UInt(8.W)))
            for (byteIdx <- 0 until (XLEN / 8)) {
              mergedBytes(byteIdx) := Mux(
                writeStrbReg(byteIdx),
                writeDataReg(byteIdx * 8 + 7, byteIdx * 8),
                dataReg(byteIdx * 8 + 7, byteIdx * 8),
              )
            }
            dataReg := mergedBytes.asUInt
          }
        }
      }
      writeRespWorker.Step("SendWriteResp") {}
      SysCall.Inline(AXI4SlaveApi.axi_send_write_resp_once(bus, writeIdReg))
      writeRespWorker.Prev.edge.add {
        writeAddrSeen := false.B
        writeDataSeen := false.B
      }
      SysCall.Return()
    }

    daemon.run {
      when(!readWorker.active) {
        SysCall.Inline(SysCall.start(readWorker))
      }
      when(!writeAddrListener.active && !writeAddrSeen) {
        SysCall.Inline(SysCall.start(writeAddrListener))
      }
      when(!writeDataListener.active && !writeDataSeen) {
        SysCall.Inline(SysCall.start(writeDataListener))
      }
      when(!writeRespWorker.active && writeAddrSeen && writeDataSeen) {
        SysCall.Inline(SysCall.start(writeRespWorker))
      }
    }
  }
}
