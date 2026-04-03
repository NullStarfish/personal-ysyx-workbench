package mycpu.pipeline

import HwOS.kernel._
import chisel3._
import mycpu.common._
import mycpu.mem.Memory

final class FetchProcess(
    memory: Memory,
    decodeRef: ApiRef[DecodeApiDecl],
    localName: String = "Fetch",
)(implicit kernel: Kernel)
    extends HwProcess(localName) {

  private val fetchThread = createThread("Fetch")
  private val pcReg = RegInit(START_ADDR.U(XLEN.W))
  private val issuedForCurrentPc = RegInit(false.B)
  private val fetchAddrReg = RegInit(0.U(XLEN.W))
  private val fetchedInstReg = RegInit(0.U(32.W))

  val api: FetchApiDecl = new FetchApiDecl {
    def writePC(nextPc: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_write_pc") { _ =>
      pcReg := nextPc
      issuedForCurrentPc := false.B
    }

    def offsetPC(delta: SInt): HwInline[Unit] = HwInline.atomic(s"${name}_offset_pc") { _ =>
      pcReg := (pcReg.asSInt + delta).asUInt
      issuedForCurrentPc := false.B
    }

    def currentPC(): HwInline[UInt] = HwInline.bindings(s"${name}_current_pc") { _ =>
      pcReg
    }
  }

  def RequestFetchApi(): HwInline[FetchApiDecl] = HwInline.bindings(s"${name}_fetch_api") { _ =>
    api
  }

  override def entry(): Unit = {
    fetchThread.entry {
      val memApi = SysCall.Inline(memory.RequestMemoryApi(0))
      val fetchedInst = SysCall.Inline(memApi.read_once(fetchAddrReg, 2.U))
      fetchThread.Prev.edge.add {
        fetchedInstReg := fetchedInst
      }

      fetchThread.Step("AdvancePc") {
        pcReg := fetchAddrReg + 4.U
        issuedForCurrentPc := true.B
      }

      fetchThread.Step("Decode") {
        SysCall.Call(decodeRef.get.decodeInst(fetchedInstReg), "AfterDecode")
      }
      fetchThread.Step("AfterDecode") {}
      SysCall.Return()
    }

    val daemon = createLogic("Daemon")
    daemon.run {
      when(!fetchThread.active && !issuedForCurrentPc) {
        fetchAddrReg := pcReg
        SysCall.Inline(SysCall.start(fetchThread))
      }
    }
  }
}
