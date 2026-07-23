package mycpu.dpi

import chisel3._
import chisel3.util.Cat

object DpiApi {

  final case class IlaProbe(name: String, value: UInt)

  def ilaProbe(name: String, value: UInt): IlaProbe = IlaProbe(name, value)

  def ila(
      clock: Clock,
      reset: Bool,
      enabled: Boolean,
      source: String,
      probes: Seq[IlaProbe],
  ): Unit = {
    val identifier = "[A-Za-z_][A-Za-z0-9_]*"
    require(
      source.matches(identifier),
      s"ILA source '$source' is not a valid identifier",
    )
    require(
      probes.nonEmpty,
      s"ILA source '$source' must contain at least one probe",
    )
    require(
      probes.map(_.name).distinct.size == probes.size,
      s"ILA source '$source' contains duplicate probe names",
    )
    probes.foreach { probe =>
      require(
        probe.name.matches(identifier),
        s"ILA probe '$source.${probe.name}' is not a valid identifier",
      )
      require(
        probe.value.getWidth > 0,
        s"ILA probe '$source.${probe.name}' has zero width",
      )
    }

    if (enabled) {
      val payloadWidth = probes.map(_.value.getWidth).sum
      val packedWidth = math.max(64, ((payloadWidth + 31) / 32) * 32)
      val payload = Cat(probes.reverse.map(_.value))
      val schema = probes.map(p => s"${p.name}:${p.value.getWidth}").mkString(",")
      val sampler = Module(new IlaProbeDPI(source, schema, packedWidth))
      sampler.io.clock := clock
      sampler.io.reset := reset
      sampler.io.sampleData := payload.pad(packedWidth)
    }
  }

  final case class SimCounterRef(tag: String, name: String)

  final class SimCounterGroup private[DpiApi] (owner: SimCounters, val tag: String) {
    def ref(name: String): SimCounterRef = owner.ref(tag, name)

    def pushToSim(name: String, event: Bool): SimCounterRef =
      owner.pushToSim(tag, name, 1.U, event)

    def pushToSim(name: String, delta: UInt, valid: Bool): SimCounterRef =
      owner.pushToSim(tag, name, delta, valid)

    def readFromSim(name: String): UInt = owner.readFromSim(tag, name)

    def ratio(name: String, numerator: SimCounterRef, denominator: SimCounterRef): Unit =
      owner.registerRatio(tag, name, numerator, denominator, percentage = false)

    def percentage(name: String, numerator: SimCounterRef, denominator: SimCounterRef): Unit =
      owner.registerRatio(tag, name, numerator, denominator, percentage = true)
  }

  final class SimCounters(clock: Clock, reset: Bool, enabled: Boolean) {
    private def requireName(kind: String, value: String): Unit =
      require(value.nonEmpty, s"simulation counter $kind must not be empty")

    def tag(tag: String): SimCounterGroup = {
      requireName("tag", tag)
      new SimCounterGroup(this, tag)
    }

    def ref(tag: String, name: String): SimCounterRef = {
      requireName("tag", tag)
      requireName("name", name)
      SimCounterRef(tag, name)
    }

    private[DpiApi] def pushToSim(
        tag: String,
        name: String,
        delta: UInt,
        valid: Bool,
    ): SimCounterRef = {
      val counterRef = ref(tag, name)
      require(delta.getWidth <= 64, s"simulation counter '$tag.$name' delta exceeds 64 bits")
      if (enabled) {
        val counter = Module(new SimCounterPushDPI(tag, name))
        counter.io.clock := clock
        counter.io.reset := reset
        counter.io.valid := valid
        counter.io.delta := delta.pad(64)
      }
      counterRef
    }

    private[DpiApi] def readFromSim(tag: String, name: String): UInt = {
      ref(tag, name)
      if (enabled) {
        val counter = Module(new SimCounterReadDPI(tag, name))
        counter.io.clock := clock
        counter.io.value
      } else {
        0.U(64.W)
      }
    }

    private[DpiApi] def registerRatio(
        tag: String,
        name: String,
        numerator: SimCounterRef,
        denominator: SimCounterRef,
        percentage: Boolean,
    ): Unit = {
      ref(tag, name)
      if (enabled) {
        Module(new SimCounterRatioDPI(
          tag,
          name,
          numerator.tag,
          numerator.name,
          denominator.tag,
          denominator.name,
          percentage,
        ))
      }
    }
  }

  def counters(clock: Clock, reset: Bool, enabled: Boolean): SimCounters =
    new SimCounters(clock, reset, enabled)


  def simEbreak(
      clock: Clock,
      reset: Bool,
      valid: Bool,
      isEbreak: UInt = 0.U(32.W),
      localName: String = "sim_ebreak",
  ): Unit = {
    val m = Module(new SimEbreakDPI).suggestName(localName)
    m.io.clock := clock
    m.io.reset := reset
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
  }
}
