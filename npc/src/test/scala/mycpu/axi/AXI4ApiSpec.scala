package mycpu.axi

import HwOS.kernel.process.HwProcess
import HwOS.kernel.system.{Kernel, SysCall}
import chisel3._
import chisel3.simulator.EphemeralSimulator._
import mycpu.common._
import org.scalatest.flatspec.AnyFlatSpec

class AxiReadOnceHarness extends Module {
  val io = IO(new Bundle {
    val start = Input(Bool())
    val addr = Input(UInt(XLEN.W))
    val busy = Output(Bool())
    val done = Output(Bool())
    val data = Output(UInt(XLEN.W))
  })

  implicit val kernel: Kernel = new Kernel()

  val bus = Wire(new AXI4Bundle(AXI_ID_WIDTH, XLEN, XLEN))
  bus.setAsMasterInit()

  io.busy := DontCare
  io.done := DontCare
  io.data := DontCare

  object Init extends HwProcess("Init") {
    private val worker = createThread("Reader")
    private val daemon = createLogic("Daemon")
    private val addrReg = RegInit(0.U(XLEN.W))
    private val dataReg = RegInit(0.U(XLEN.W))
    private val doneReg = RegInit(false.B)

    override def entry(): Unit = {
      worker.entry {
        val value = WireDefault(0.U(XLEN.W))
        val site = SysCall.CallSite(AXI4Api.axi_read_once(bus, 0.U, addrReg, 2.U))
        site.edge.add {
          dataReg := value
          doneReg := true.B
        }

        value := SysCall.Call(site)
      }

      daemon.run {
        when(io.start && !worker.active) {
          addrReg := io.addr
          doneReg := false.B
          SysCall.Inline(SysCall.start(worker))
        }

        io.busy := worker.active
        io.done := doneReg
        io.data := dataReg
      }
    }
  }

  Init.build()

  val readPending = RegInit(false.B)
  val readAddr = RegInit(0.U(XLEN.W))
  val readDelay = RegInit(0.U(2.W))
  val memory = VecInit(Seq.tabulate(16)(i => (i * 9 + 5).U(XLEN.W)))

  bus.aw.ready := false.B
  bus.w.ready := false.B
  bus.b.valid := false.B
  bus.b.bits := DontCare

  bus.ar.ready := !readPending
  bus.r.valid := false.B
  bus.r.bits := DontCare

  when(bus.ar.valid && bus.ar.ready) {
    readPending := true.B
    readAddr := bus.ar.bits.addr
    readDelay := 0.U
  }

  when(readPending) {
    readDelay := readDelay + 1.U
    when(readDelay >= 1.U) {
      bus.r.valid := true.B
      bus.r.bits.id := 0.U
      bus.r.bits.data := memory(readAddr(3, 0))
      bus.r.bits.resp := AXI4Parameters.RESP_OKAY
      bus.r.bits.last := true.B
      when(bus.r.ready) {
        readPending := false.B
      }
    }
  }
}

class AxiWriteOnceHarness extends Module {
  val io = IO(new Bundle {
    val start = Input(Bool())
    val addr = Input(UInt(XLEN.W))
    val dataIn = Input(UInt(XLEN.W))
    val strb = Input(UInt((XLEN / 8).W))
    val busy = Output(Bool())
    val done = Output(Bool())
    val committed = Output(UInt(XLEN.W))
  })

  implicit val kernel: Kernel = new Kernel()

  val bus = Wire(new AXI4Bundle(AXI_ID_WIDTH, XLEN, XLEN))
  bus.setAsMasterInit()

  io.busy := DontCare
  io.done := DontCare
  io.committed := DontCare

  object Init extends HwProcess("Init") {
    private val worker = createThread("Writer")
    private val daemon = createLogic("Daemon")
    private val addrReg = RegInit(0.U(XLEN.W))
    private val dataReg = RegInit(0.U(XLEN.W))
    private val strbReg = RegInit(0.U((XLEN / 8).W))
    private val doneReg = RegInit(false.B)

    override def entry(): Unit = {
      worker.entry {
        SysCall.Call(AXI4Api.axi_write_once(bus, 0.U, addrReg, 2.U, dataReg, strbReg), "Done")
        worker.Step("Done") {
          doneReg := true.B
        }
        SysCall.Return()
      }

      daemon.run {
        when(io.start && !worker.active) {
          addrReg := io.addr
          dataReg := io.dataIn
          strbReg := io.strb
          doneReg := false.B
          SysCall.Inline(SysCall.start(worker))
        }

        io.busy := worker.active
        io.done := doneReg
      }
    }
  }

  Init.build()

  val memory = RegInit(VecInit(Seq.fill(16)(0.U(XLEN.W))))
  val writePending = RegInit(false.B)
  val pendingAddr = RegInit(0.U(XLEN.W))
  val pendingData = RegInit(0.U(XLEN.W))
  val pendingStrb = RegInit(0.U((XLEN / 8).W))
  val writeDelay = RegInit(0.U(2.W))

  bus.ar.ready := false.B
  bus.r.valid := false.B
  bus.r.bits := DontCare

  bus.aw.ready := !writePending
  bus.w.ready := !writePending
  bus.b.valid := false.B
  bus.b.bits := DontCare

  when(bus.aw.valid && bus.aw.ready && bus.w.valid && bus.w.ready) {
    writePending := true.B
    pendingAddr := bus.aw.bits.addr
    pendingData := bus.w.bits.data
    pendingStrb := bus.w.bits.strb
    writeDelay := 0.U
  }

  when(writePending) {
    writeDelay := writeDelay + 1.U
    when(writeDelay >= 1.U) {
      val idx = pendingAddr(3, 0)
      val oldValue = memory(idx)
      val mergedBytes = Wire(Vec(XLEN / 8, UInt(8.W)))
      for (byteIdx <- 0 until (XLEN / 8)) {
        mergedBytes(byteIdx) := Mux(
          pendingStrb(byteIdx),
          pendingData(byteIdx * 8 + 7, byteIdx * 8),
          oldValue(byteIdx * 8 + 7, byteIdx * 8),
        )
      }
      val nextValue = mergedBytes.asUInt
      memory(idx) := nextValue

      bus.b.valid := true.B
      bus.b.bits.id := 0.U
      bus.b.bits.resp := AXI4Parameters.RESP_OKAY
      when(bus.b.ready) {
        writePending := false.B
      }
    }
  }

  io.committed := memory(io.addr(3, 0))
}

class AXI4ApiSpec extends AnyFlatSpec {
  "axi_read_once" should "turn a single AXI read transaction into a blocking HwInline call" in {
    simulate(new AxiReadOnceHarness) { c =>
      c.reset.poke(true.B)
      c.io.start.poke(false.B)
      c.io.addr.poke(0.U)
      c.clock.step()

      c.reset.poke(false.B)
      c.io.addr.poke(5.U)
      c.io.start.poke(true.B)
      c.clock.step()
      c.io.start.poke(false.B)

      var cycles = 0
      while (c.io.done.peek().litValue == 0 && cycles < 20) {
        c.clock.step()
        cycles += 1
      }

      c.io.done.expect(true.B)
      c.clock.step(2)
      c.io.data.expect(50.U)
    }
  }

  "axi_write_once" should "turn a single AXI write transaction into a blocking HwInline call" in {
    simulate(new AxiWriteOnceHarness) { c =>
      c.reset.poke(true.B)
      c.io.start.poke(false.B)
      c.io.addr.poke(0.U)
      c.io.dataIn.poke(0.U)
      c.io.strb.poke(0.U)
      c.clock.step()

      c.reset.poke(false.B)
      c.io.addr.poke(6.U)
      c.io.dataIn.poke("h12345678".U)
      c.io.strb.poke("b1111".U)
      c.io.start.poke(true.B)
      c.clock.step()
      c.io.start.poke(false.B)

      var cycles = 0
      while (c.io.done.peek().litValue == 0 && cycles < 20) {
        c.clock.step()
        cycles += 1
      }

      c.io.done.expect(true.B)
      c.clock.step(2)
      c.io.committed.expect("h12345678".U)
    }
  }
}
