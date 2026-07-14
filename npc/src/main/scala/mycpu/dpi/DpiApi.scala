package mycpu.dpi

import chisel3._

object DpiApi {

  final class SimCounters(clock: Clock, reset: Bool, enabled: Boolean) {
    def pushToSim(name: String, event: Bool): Unit =
      pushToSim(name, 1.U, event)

    def pushToSim(name: String, delta: UInt, valid: Bool): Unit = {
      require(delta.getWidth <= 64, s"simulation counter '$name' delta exceeds 64 bits")
      if (enabled) {
        val counter = Module(new SimCounterPushDPI(name))
        counter.io.clock := clock
        counter.io.reset := reset
        counter.io.valid := valid
        counter.io.delta := delta.pad(64)
      }
    }

    def readFromSim(name: String): UInt = {
      if (enabled) {
        val counter = Module(new SimCounterReadDPI(name))
        counter.io.clock := clock
        counter.io.value
      } else {
        0.U(64.W)
      }
    }
  }

  def counters(clock: Clock, reset: Bool, enabled: Boolean): SimCounters =
    new SimCounters(clock, reset, enabled)


  def simEbreak(valid: Bool, isEbreak: UInt = 0.U(32.W), localName: String = "sim_ebreak"): Unit = {
    val m = Module(new SimEbreakDPI).suggestName(localName)
    m.io.valid := valid
    m.io.is_ebreak := isEbreak
  }

  def recordFlush(clock: Clock, reset: Bool, valid: Bool, localName: String = "flush_record"): Unit = {
    val m = Module(new FlushDPI).suggestName(localName)
    m.io.clock := clock
    m.io.reset := reset
    m.io.valid := valid
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
    m.io.instType := state.instType
    m.io.icacheHit := state.icacheHit
  }
}
