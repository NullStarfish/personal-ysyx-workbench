package mycpu.pipeline

import HwOS.kernel._
import chisel3._
import chisel3.simulator.EphemeralSimulator._
import mycpu.common._
import org.scalatest.flatspec.AnyFlatSpec

class RegfileReadWriteHarness extends Module {
  val io = IO(new Bundle {
    val done = Output(Bool())
    val x1 = Output(UInt(XLEN.W))
  })

  implicit val kernel: Kernel = new Kernel()

  object Init extends HwProcess("Init") {
    val regfile = spawn(new RegfileProcess("Regfile"))
    private val worker = createThread("Worker")
    private val doneReg = RegInit(false.B)
    private val x1Reg = RegInit(0.U(XLEN.W))

    override def entry(): Unit = {
      val regApi = regfile.api
      val probeApi = regfile.probeApi

      worker.entry {
        worker.Step("Write") {
          SysCall.Inline(regApi.write(1.U, "h12345678".U(XLEN.W)))
        }
        worker.Step("Read") {
          x1Reg := SysCall.Inline(regApi.read(1.U))
        }
        worker.Step("Probe") {
          x1Reg := SysCall.Inline(probeApi.read(1.U))
          doneReg := true.B
        }
        SysCall.Return()
      }

      val daemon = createLogic("Daemon")
      daemon.run {
        when(!worker.active && !worker.done) {
          SysCall.Inline(SysCall.start(worker))
        }
        io.done := doneReg
        io.x1 := x1Reg
      }
    }
  }

  Init.build()
}

class RegfileReserveReturnHarness extends Module {
  val io = IO(new Bundle {
    val done = Output(Bool())
    val token = Output(UInt(4.W))
  })

  implicit val kernel: Kernel = new Kernel()

  object Init extends HwProcess("Init") {
    val regfile = spawn(new RegfileProcess("Regfile"))
    private val worker = createThread("Worker")
    private val doneReg = RegInit(false.B)
    private val tokenReg = RegInit(0.U(4.W))

    override def entry(): Unit = {
      val regApi = regfile.api

      worker.entry {
        val token = SysCall.Inline(regApi.reserve(1.U))
        worker.Step("Latch") {
          tokenReg := token
          doneReg := true.B
        }
        SysCall.Return()
      }

      val daemon = createLogic("Daemon")
      daemon.run {
        when(!worker.active && !worker.done) {
          SysCall.Inline(SysCall.start(worker))
        }
        io.done := doneReg
        io.token := tokenReg
      }
    }
  }

  Init.build()
}

class RegfileReservePathHarness extends Module {
  val io = IO(new Bundle {
    val done = Output(Bool())
    val token = Output(UInt(4.W))
  })

  implicit val kernel: Kernel = new Kernel()

  object Init extends HwProcess("Init") {
    val regfile = spawn(new RegfileProcess("Regfile"))
    private val worker = createThread("Worker")
    private val doneReg = RegInit(false.B)
    private val tokenReg = RegInit(0.U(4.W))

    override def entry(): Unit = {
      val regApi = regfile.api

      worker.entry {
        worker.Step("Issue") {
          SysCall.Inline(regApi.reservePath(1.U))
        }
        worker.Step("WaitDone") {
          worker.waitCondition(SysCall.Inline(regApi.reserveDone()))
        }
        worker.Step("Latch") {
          tokenReg := SysCall.Inline(regApi.reserveToken())
          SysCall.Inline(regApi.consumeReserveResp())
          doneReg := true.B
        }
        SysCall.Return()
      }

      val daemon = createLogic("Daemon")
      daemon.run {
        when(!worker.active && !worker.done) {
          SysCall.Inline(SysCall.start(worker))
        }
        io.done := doneReg
        io.token := tokenReg
      }
    }
  }

  Init.build()
}

class RegfileCommitHarness extends Module {
  val io = IO(new Bundle {
    val done = Output(Bool())
    val token = Output(UInt(4.W))
    val x1 = Output(UInt(XLEN.W))
  })

  implicit val kernel: Kernel = new Kernel()

  object Init extends HwProcess("Init") {
    val regfile = spawn(new RegfileProcess("Regfile"))
    private val worker = createThread("Worker")
    private val doneReg = RegInit(false.B)
    private val tokenReg = RegInit(0.U(4.W))
    private val x1Reg = RegInit(0.U(XLEN.W))

    override def entry(): Unit = {
      val regApi = regfile.api
      val probeApi = regfile.probeApi

      worker.entry {
        val token = SysCall.Inline(regApi.reserve(1.U))
        worker.Step("LatchToken") {
          tokenReg := token
        }
        SysCall.Inline(regApi.writebackAndClear(token, "hdeadbeef".U(XLEN.W)))
        worker.Step("Probe") {
          x1Reg := SysCall.Inline(probeApi.read(1.U))
          doneReg := true.B
        }
        SysCall.Return()
      }

      val daemon = createLogic("Daemon")
      daemon.run {
        when(!worker.active && !worker.done) {
          SysCall.Inline(SysCall.start(worker))
        }
        io.done := doneReg
        io.token := tokenReg
        io.x1 := x1Reg
      }
    }
  }

  Init.build()
}

class RegfileCommitLatchedHarness extends Module {
  val io = IO(new Bundle {
    val done = Output(Bool())
    val token = Output(UInt(4.W))
    val x1 = Output(UInt(XLEN.W))
  })

  implicit val kernel: Kernel = new Kernel()

  object Init extends HwProcess("Init") {
    val regfile = spawn(new RegfileProcess("Regfile"))
    private val worker = createThread("Worker")
    private val doneReg = RegInit(false.B)
    private val tokenReg = RegInit(0.U(4.W))
    private val x1Reg = RegInit(0.U(XLEN.W))

    override def entry(): Unit = {
      val regApi = regfile.api
      val probeApi = regfile.probeApi

      worker.entry {
        val token =  SysCall.Inline(regApi.reserve(1.U))
        SysCall.Inline(regApi.writebackAndClear(token, "hdeadbeef".U(XLEN.W)))
        worker.Step("Probe") {
          x1Reg := SysCall.Inline(probeApi.read(1.U))
          doneReg := true.B
        }
        SysCall.Return()
      }

      val daemon = createLogic("Daemon")
      daemon.run {
        when(!worker.active && !worker.done) {
          SysCall.Inline(SysCall.start(worker))
        }
        io.done := doneReg
        io.token := tokenReg
        io.x1 := x1Reg
      }
    }
  }

  Init.build()
}

class RegfileProcessInterfaceSpec extends AnyFlatSpec {
  "RegfileProcess write/read" should "support direct write and read" in {
    simulate(new RegfileReadWriteHarness) { c =>
      c.reset.poke(true.B)
      c.clock.step()
      c.reset.poke(false.B)

      var cycles = 0
      while (c.io.done.peek().litValue == 0 && cycles < 20) {
        c.clock.step()
        cycles += 1
      }

      c.io.done.expect(true.B)
      c.io.x1.expect("h12345678".U(XLEN.W))
    }
  }

  it should "support reserve returning a token directly" in {
    simulate(new RegfileReserveReturnHarness) { c =>
      c.reset.poke(true.B)
      c.clock.step()
      c.reset.poke(false.B)

      var cycles = 0
      while (c.io.done.peek().litValue == 0 && cycles < 20) {
        c.clock.step()
        cycles += 1
      }

      c.io.done.expect(true.B)
      c.io.token.expect(1.U)
    }
  }

  it should "support reserve response path style API" in {
    simulate(new RegfileReservePathHarness) { c =>
      c.reset.poke(true.B)
      c.clock.step()
      c.reset.poke(false.B)

      var cycles = 0
      while (c.io.done.peek().litValue == 0 && cycles < 20) {
        c.clock.step()
        cycles += 1
      }

      c.io.done.expect(true.B)
      c.io.token.expect(1.U)
    }
  }

  it should "support writebackAndClear after reserve" in {
    simulate(new RegfileCommitHarness) { c =>
      c.reset.poke(true.B)
      c.clock.step()
      c.reset.poke(false.B)

      var cycles = 0
      while (c.io.done.peek().litValue == 0 && cycles < 20) {
        c.clock.step()
        cycles += 1
      }

      c.io.done.expect(true.B)
      c.io.token.expect(1.U)
      c.io.x1.expect("hdeadbeef".U(XLEN.W))
    }
  }

  it should "support writebackAndClear after latching the reserved token" in {
    simulate(new RegfileCommitLatchedHarness) { c =>
      c.reset.poke(true.B)
      c.clock.step()
      c.reset.poke(false.B)

      var cycles = 0
      while (c.io.done.peek().litValue == 0 && cycles < 20) {
        c.clock.step()
        cycles += 1
      }

      c.io.done.expect(true.B)
      c.io.token.expect(1.U)
      c.io.x1.expect("hdeadbeef".U(XLEN.W))
    }
  }
}
