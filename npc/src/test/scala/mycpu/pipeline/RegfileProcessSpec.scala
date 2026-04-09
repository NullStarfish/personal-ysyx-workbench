package mycpu.pipeline

import HwOS.kernel._
import chisel3._
import chisel3.simulator.EphemeralSimulator._
import mycpu.common._
import org.scalatest.flatspec.AnyFlatSpec

class RegfileProcessHarness extends Module {
  val io = IO(new Bundle {
    val done = Output(Bool())
    val token1 = Output(UInt(4.W))
    val token2 = Output(UInt(4.W))
    val x1 = Output(UInt(XLEN.W))
    val x2 = Output(UInt(XLEN.W))
  })

  implicit val kernel: Kernel = new Kernel()

  object Init extends HwProcess("Init") {
    val regfile = spawn(new RegfileProcess("Regfile"))
    private val reserveX1Worker = createThread("ReserveX1Worker")
    private val reserveX2Worker = createThread("ReserveX2Worker")
    private val commitWorker = createThread("CommitWorker")
    private val observer = createThread("Observer")
    private val daemon = createLogic("Daemon")

    private val doneReg = RegInit(false.B)
    private val token1Reg = RegInit(0.U(4.W))
    private val token2Reg = RegInit(0.U(4.W))
    private val x1Reg = RegInit(0.U(XLEN.W))
    private val x2Reg = RegInit(0.U(XLEN.W))

    val regApi = regfile.api
    override def entry(): Unit = {
      reserveX1Worker.entry {
        reserveX1Worker.Step("Issue") {
          SysCall.Inline(regApi.reservePath(1.U))
        }
        reserveX1Worker.Step("WaitResp") {
          reserveX1Worker.waitCondition(SysCall.Inline(regApi.reserveDone()))
        }
        reserveX1Worker.Step("Latch") {
          token1Reg := SysCall.Inline(regApi.reserveToken())
          SysCall.Inline(regApi.consumeReserveResp())
        }
        reserveX1Worker.Step("Done") {}
        SysCall.Return()
      }

      reserveX2Worker.entry {
        reserveX2Worker.Step("Issue") {
          SysCall.Inline(regApi.reservePath(2.U))
        }
        reserveX2Worker.Step("WaitResp") {
          reserveX2Worker.waitCondition(SysCall.Inline(regApi.reserveDone()))
        }
        reserveX2Worker.Step("Latch") {
          token2Reg := SysCall.Inline(regApi.reserveToken())
          SysCall.Inline(regApi.consumeReserveResp())
        }
        reserveX2Worker.Step("Done") {}
        SysCall.Return()
      }

      commitWorker.entry {
        SysCall.Inline(regApi.writebackAndClear(token1Reg, "h11111111".U(XLEN.W)))
        SysCall.Inline(regApi.writebackAndClear(token2Reg, "h22222222".U(XLEN.W)))
        commitWorker.Step("Done") {}
        SysCall.Return()
      }

      val probeApi = regfile.probeApi
      observer.entry {
        val x1Seen = RegInit(0.U(XLEN.W))
        val x2Seen = RegInit(0.U(XLEN.W))
        observer.Step("WaitWrites") {
          val x1Now = SysCall.Inline(probeApi.read(1.U))
          val x2Now = SysCall.Inline(probeApi.read(2.U))
          x1Seen := x1Now
          x2Seen := x2Now
          observer.waitCondition(
            x1Now === "h11111111".U(XLEN.W) &&
              x2Now === "h22222222".U(XLEN.W),
          )
        }
        observer.Step("Sample") {
          x1Reg := x1Seen
          x2Reg := x2Seen
          doneReg := true.B
        }
        SysCall.Return()
      }

      daemon.run {
        when(!reserveX1Worker.active && !reserveX1Worker.done) {
          SysCall.Inline(SysCall.start(reserveX1Worker))
        }
        when(reserveX1Worker.done && !reserveX2Worker.active && !reserveX2Worker.done) {
          SysCall.Inline(SysCall.start(reserveX2Worker))
        }
        when(reserveX2Worker.done && !commitWorker.active && !commitWorker.done) {
          SysCall.Inline(SysCall.start(commitWorker))
        }
        when(!observer.active && !observer.done) {
          SysCall.Inline(SysCall.start(observer))
        }
        io.done := doneReg
        io.token1 := token1Reg
        io.token2 := token2Reg
        io.x1 := x1Reg
        io.x2 := x2Reg
      }
    }
  }

  Init.build()
}

class RegfileProcessSpec extends AnyFlatSpec {
  "RegfileProcess" should "allocate distinct dynamic write tokens and commit each token to its reserved register" in {
    simulate(new RegfileProcessHarness) { c =>
      c.reset.poke(true.B)
      c.clock.step()
      c.reset.poke(false.B)

      var cycles = 0
      while (c.io.done.peek().litValue == 0 && cycles < 80) {
        c.clock.step()
        cycles += 1
      }

      c.io.done.expect(true.B)
      c.io.token1.expect(1.U)
      c.io.token2.expect(2.U)
      c.io.x1.expect("h11111111".U(XLEN.W))
      c.io.x2.expect("h22222222".U(XLEN.W))
    }
  }
}
