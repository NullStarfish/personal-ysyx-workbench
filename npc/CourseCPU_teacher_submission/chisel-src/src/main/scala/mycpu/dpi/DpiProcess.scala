package mycpu.dpi

import HwOS.kernel._
import chisel3._

trait DpiApiDecl {
  def difftestSkip(): HwInline[Unit]
  def simEbreak(isEbreak: UInt = 0.U(32.W)): HwInline[Unit]
  def simState(state: SimStateBundle): HwInline[Unit]
}

final class DpiProcess(
    hostClock: Clock,
    hostReset: Bool,
    localName: String = "Dpi",
)(implicit kernel: Kernel)
    extends HwProcess(localName) {

  private val difftestSkipMod = Module(new DifftestSkipDPI)
  private val simEbreakMod = Module(new SimEbreakDPI)
  private val simStateMod = Module(new SimStateDPI)

  private val skipPulseReg = RegInit(false.B)
  private val ebreakValidReg = RegInit(false.B)
  private val ebreakTagReg = RegInit(0.U(32.W))
  private val simStatePulseReg = RegInit(false.B)
  private val simStateReg = RegInit(0.U.asTypeOf(new SimStateBundle))

  difftestSkipMod.io.clock := hostClock
  difftestSkipMod.io.skip := skipPulseReg

  simEbreakMod.io.valid := ebreakValidReg
  simEbreakMod.io.is_ebreak := ebreakTagReg

  simStateMod.io.clk := hostClock
  simStateMod.io.reset := hostReset
  simStateMod.io.valid := simStatePulseReg && simStateReg.valid
  simStateMod.io.pc := simStateReg.pc
  simStateMod.io.dnpc := simStateReg.dnpc
  simStateMod.io.regs_flat := simStateReg.regsFlat
  simStateMod.io.mtvec := simStateReg.mtvec
  simStateMod.io.mepc := simStateReg.mepc
  simStateMod.io.mstatus := simStateReg.mstatus
  simStateMod.io.mcause := simStateReg.mcause
  simStateMod.io.inst := simStateReg.inst

  val api: DpiApiDecl = new DpiApiDecl {
    override def difftestSkip(): HwInline[Unit] = HwInline.atomic(s"${name}_difftest_skip") { _ =>
      skipPulseReg := true.B
    }

    override def simEbreak(isEbreak: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_sim_ebreak") { _ =>
      ebreakTagReg := isEbreak
      ebreakValidReg := true.B
    }

    override def simState(state: SimStateBundle): HwInline[Unit] = HwInline.atomic(s"${name}_sim_state") { _ =>
      simStateReg := state
      simStatePulseReg := true.B
    }
  }

  def RequestDpiApi(): HwInline[DpiApiDecl] = HwInline.bindings(s"${name}_dpi_api") { _ =>
    api
  }

  override def entry(): Unit = {
    val daemon = createLogic("Daemon")
    daemon.run {
      when(skipPulseReg) {
        skipPulseReg := false.B
      }
      when(ebreakValidReg) {
        ebreakValidReg := false.B
      }
      when(simStatePulseReg) {
        simStatePulseReg := false.B
      }
    }
  }
}
