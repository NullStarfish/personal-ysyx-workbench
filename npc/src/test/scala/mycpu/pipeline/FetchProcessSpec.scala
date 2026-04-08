package mycpu.pipeline

import HwOS.kernel._
import chisel3._
import chisel3.simulator.EphemeralSimulator._
import mycpu.common._
import mycpu.mem.DummyMemory
import org.scalatest.flatspec.AnyFlatSpec

final class StubFetchDecodeProcess(localName: String)(implicit kernel: Kernel)
    extends HwProcess(localName)
{
  private val decodeCountReg = RegInit(0.U(8.W))
  private val lastPcReg = RegInit(0.U(XLEN.W))
  private val lastInstReg = RegInit(0.U(32.W))

  val api: DecodeApiDecl = new DecodeApiDecl {
    override def decodeInst(pc: UInt, inst: UInt): HwInline[Unit] = HwInline.thread(s"${name}_decode_inst") { _ =>
      decodeCountReg := decodeCountReg + 1.U
      lastPcReg := pc
      lastInstReg := inst
      SysCall.Return()
    }
  }

  def count: UInt = decodeCountReg
  def lastPc: UInt = lastPcReg
  def lastInst: UInt = lastInstReg
  override def entry(): Unit = {}
}

class FetchProcessHarness extends Module {
  val io = IO(new Bundle {
    val jump = Input(Bool())
    val jumpTarget = Input(UInt(XLEN.W))
    val decodeCount = Output(UInt(8.W))
    val lastInst = Output(UInt(32.W))
  })

  implicit val kernel: Kernel = new Kernel()

  object Init extends HwProcess("Init") {
    val links = new PipelineLinks
    private val memory = spawn(new DummyMemory(
      readonlyWords = Seq(
        BigInt(START_ADDR) -> BigInt("00112233", 16),
        BigInt(START_ADDR + 0x20L) -> BigInt("89abcdef", 16),
      ),
      mutableWords = Seq(BigInt(0) -> BigInt(0)),
      maxClients = 1,
      localName = "DummyMemory",
    ))
    val trace = spawn(new NoopTracerProcess("Tracer"))
    lazy val decode = spawn(new StubFetchDecodeProcess("Decode"))
    lazy val fetch: FetchProcess = adopt(new FetchProcess(memory, links.decode, links.trace, "Fetch"))
    private val pcWriter = createThread("PcWriter")
    private val daemon = createLogic("Daemon")

    override def entry(): Unit = {
      pcWriter.entry {
        val api = SysCall.Inline(fetch.RequestFetchApi())
        pcWriter.Step("WritePC") {
          SysCall.Inline(api.writePC(io.jumpTarget))
        }
        SysCall.Return()
      }

      daemon.run {
        when(io.jump && !pcWriter.active) {
          SysCall.Inline(SysCall.start(pcWriter))
        }

        io.decodeCount := decode.count
        io.lastInst := decode.lastInst
      }
    }
  }

  Init.links.decode.bind(Init.decode.api)
  Init.links.trace.bind(Init.trace.api)
  Init.fetch.build()
  Init.build()
}

class FetchProcessSpec extends AnyFlatSpec {
  "FetchProcess" should "fetch from the current pc and then refetch after writePC" in {
    simulate(new FetchProcessHarness) { c =>
      c.reset.poke(true.B)
      c.io.jump.poke(false.B)
      c.io.jumpTarget.poke(0.U)
      c.clock.step()
      c.reset.poke(false.B)

      var cycles = 0
      while (c.io.lastInst.peek().litValue != BigInt("00112233", 16) && cycles < 30) {
        c.clock.step()
        cycles += 1
      }

      c.io.lastInst.expect("h00112233".U)
      assert(c.io.decodeCount.peek().litValue >= 1)

      c.clock.step()

      c.io.jumpTarget.poke((START_ADDR + 0x20).U)
      c.io.jump.poke(true.B)
      c.clock.step()
      c.io.jump.poke(false.B)

      cycles = 0
      while (c.io.lastInst.peek().litValue != BigInt("89abcdef", 16) && cycles < 30) {
        c.clock.step()
        cycles += 1
      }

      c.io.lastInst.expect("h89abcdef".U)
      assert(c.io.decodeCount.peek().litValue >= 2)
    }
  }
}
