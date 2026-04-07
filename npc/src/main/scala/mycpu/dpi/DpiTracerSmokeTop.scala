package mycpu.dpi

import _root_.circt.stage.ChiselStage
import HwOS.kernel._
import chisel3._
import chisel3.util._
import mycpu.pipeline._

final class DummyFetchProbeProcess(localName: String = "DummyFetch")(implicit kernel: Kernel)
    extends HwProcess(localName) {

  private val pcReg = RegInit(0.U(32.W))

  val api: FetchApiDecl = new FetchApiDecl {
    override def writePC(nextPc: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_write_pc") { _ =>
      pcReg := nextPc
    }

    override def offsetPC(delta: SInt): HwInline[Unit] = HwInline.atomic(s"${name}_offset_pc") { _ =>
      pcReg := (pcReg.asSInt + delta).asUInt
    }

    override def currentPC(): HwInline[UInt] = HwInline.bindings(s"${name}_current_pc") { _ =>
      pcReg
    }
  }

  override def entry(): Unit = {}
}

final class DummyRegfileProbeProcess(localName: String = "DummyRegProbe")(implicit kernel: Kernel)
    extends HwProcess(localName) {

  private val flatRegs = RegInit(VecInit((0 until 32).map(i => (BigInt(i) << 4).U(32.W))))

  val api: RegfileProbeApiDecl = new RegfileProbeApiDecl {
    override def read(addr: UInt): HwInline[UInt] = HwInline.bindings(s"${name}_read") { _ =>
      flatRegs(addr)
    }

    override def readAllFlat(): HwInline[UInt] = HwInline.bindings(s"${name}_read_all_flat") { _ =>
      flatRegs.asUInt
    }
  }

  override def entry(): Unit = {}
}

final class DummyCsrProbeProcess(localName: String = "DummyCsrProbe")(implicit kernel: Kernel)
    extends HwProcess(localName) {

  private val mtvecReg = RegInit("h30001000".U(32.W))
  private val mepcReg = RegInit("h30000080".U(32.W))
  private val mstatusReg = RegInit("h00001800".U(32.W))
  private val mcauseReg = RegInit("h0000000b".U(32.W))

  val api: CsrProbeApiDecl = new CsrProbeApiDecl {
    override def read(addr: UInt): HwInline[UInt] = HwInline.bindings(s"${name}_read") { _ =>
      MuxLookup(addr, 0.U)(Seq(
        "h305".U -> mtvecReg,
        "h341".U -> mepcReg,
        "h300".U -> mstatusReg,
        "h342".U -> mcauseReg,
      ))
    }

    override def mtvec(): HwInline[UInt] = HwInline.bindings(s"${name}_mtvec") { _ => mtvecReg }
    override def mepc(): HwInline[UInt] = HwInline.bindings(s"${name}_mepc") { _ => mepcReg }
    override def mstatus(): HwInline[UInt] = HwInline.bindings(s"${name}_mstatus") { _ => mstatusReg }
    override def mcause(): HwInline[UInt] = HwInline.bindings(s"${name}_mcause") { _ => mcauseReg }
  }

  override def entry(): Unit = {}
}

class DpiTracerSmokeTop extends Module {
  val io = IO(new Bundle {
    val start = Input(Bool())
    val done = Output(Bool())
  })

  implicit val kernel: Kernel = new Kernel()

  object Init extends HwProcess("Init") {
    val fetchRef = new ApiRef[FetchApiDecl]
    val regfileProbeRef = new ApiRef[RegfileProbeApiDecl]
    val csrProbeRef = new ApiRef[CsrProbeApiDecl]

    val fetch = spawn(new DummyFetchProbeProcess("FetchProbe"))
    val regProbe = spawn(new DummyRegfileProbeProcess("RegfileProbe"))
    val csrProbe = spawn(new DummyCsrProbeProcess("CsrProbe"))
    val tracer = adopt(new TracerProcess(fetchRef, regfileProbeRef, csrProbeRef, clock, reset.asBool, localName = "Tracer"))
    val dpi = spawn(new DpiProcess(clock, reset.asBool, "Dpi"))

    private val stim = createThread("Stim")
    private val daemon = createLogic("Daemon")
    private val startedReg = RegInit(false.B)

    override def entry(): Unit = {
      stim.entry {
        val traceApi = SysCall.Inline(tracer.RequestTraceApi())
        val dpiApi = SysCall.Inline(dpi.RequestDpiApi())
        val fetchApi = fetch.api

        stim.Step("PrimePc0") {
          SysCall.Inline(fetchApi.writePC("h30000004".U))
        }
        stim.Step("DifftestSkip") {
          SysCall.Inline(dpiApi.difftestSkip())
        }
        stim.Step("Issue0") {
          SysCall.Inline(traceApi.issue("h30000000".U, "h00100093".U))
        }
        stim.Step("Commit0") {
          SysCall.Inline(traceApi.commit())
        }
        stim.Step("PrimePc1") {
          SysCall.Inline(fetchApi.writePC("h30000008".U))
        }
        stim.Step("Issue1") {
          SysCall.Inline(traceApi.issue("h30000004".U, "h00208113".U))
        }
        stim.Step("Commit1") {
          SysCall.Inline(traceApi.commit())
        }
        stim.Step("Ebreak") {
          SysCall.Inline(dpiApi.simEbreak(1.U))
        }
        SysCall.Return()
      }

      daemon.run {
        when(io.start && !startedReg) {
          startedReg := true.B
          SysCall.Inline(SysCall.start(stim))
        }
        io.done := stim.done
      }
    }
  }

  Init.fetchRef.bind(Init.fetch.api)
  Init.regfileProbeRef.bind(Init.regProbe.api)
  Init.csrProbeRef.bind(Init.csrProbe.api)
  Init.tracer.build()
  Init.build()
}

object GenDpiTracerSmokeTop extends App {
  ChiselStage.emitSystemVerilogFile(
    new DpiTracerSmokeTop,
    Array("--target-dir", "generated/dpi_smoke"),
  )
}
