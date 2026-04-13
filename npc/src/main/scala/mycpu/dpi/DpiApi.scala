package mycpu.dpi

import chisel3._

object DpiApi {
  def difftestSkip(clock: Clock, skip: Bool, localName: String = "difftest_skip"): Unit = {
    val m = Module(new DifftestSkipDPI).suggestName(localName)
    m.io.clock := clock
    m.io.skip := skip
  }

  def simEbreak(valid: Bool, isEbreak: UInt = 0.U(32.W), localName: String = "sim_ebreak"): Unit = {
    val m = Module(new SimEbreakDPI).suggestName(localName)
    m.io.valid := valid
    m.io.is_ebreak := isEbreak
  }

  def simState(clock: Clock, reset: Bool, state: SimStateBundle, localName: String = "sim_state"): Unit = {
    val m = Module(new SimStateDPI).suggestName(localName)
    m.io.clk := clock
    m.io.reset := reset
    m.io.valid := state.valid
    m.io.pc := state.pc
    m.io.dnpc := state.dnpc
    m.io.reg_wen := state.regWen
    m.io.reg_addr := state.regAddr
    m.io.reg_data := state.regData
    m.io.regs_flat := state.regsFlat
    m.io.mtvec := state.mtvec
    m.io.mepc := state.mepc
    m.io.mstatus := state.mstatus
    m.io.mcause := state.mcause
    m.io.inst := state.inst
  }
}
