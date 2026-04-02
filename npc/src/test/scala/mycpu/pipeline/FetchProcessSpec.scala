package mycpu.pipeline

import HwOS.kernel._
import chisel3._
import chisel3.simulator.EphemeralSimulator._
import chisel3.util._
import mycpu.axi._
import mycpu.common._
import mycpu.mem.Memory
import org.scalatest.flatspec.AnyFlatSpec

final class StubFetchDecodeProcess(localName: String)(implicit kernel: Kernel)
    extends HwProcess(localName)
{
  private val decodeCountReg = RegInit(0.U(8.W))
  private val lastInstReg = RegInit(0.U(32.W))

  val api: DecodeApiDecl = new DecodeApiDecl {
    override def decodeInst(inst: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_decode_inst") { _ =>
      decodeCountReg := decodeCountReg + 1.U
      lastInstReg := inst
    }
  }

  def count: UInt = decodeCountReg
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

  val bus = Wire(new AXI4Bundle(AXI_ID_WIDTH, XLEN, XLEN))
  bus.setAsMasterInit()

  object Init extends HwProcess("Init") {
    val links = new PipelineLinks
    private val memory = spawn(new Memory(bus, maxClients = 1))
    lazy val decode = spawn(new StubFetchDecodeProcess("Decode"))
    lazy val fetch: FetchProcess = adopt(new FetchProcess(memory, links.decode, "Fetch"))
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
  Init.fetch.build()
  Init.build()

  private val readPending = RegInit(false.B)
  private val readAddr = RegInit(0.U(XLEN.W))
  private val readData = MuxLookup(readAddr, 0.U(XLEN.W))(
    Seq(
      START_ADDR.U -> "h00112233".U(XLEN.W),
      (START_ADDR + 0x20).U -> "h89abcdef".U(XLEN.W),
    ),
  )

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
  }

  when(readPending) {
    bus.r.valid := true.B
    bus.r.bits.id := 0.U
    bus.r.bits.data := readData
    bus.r.bits.resp := 0.U
    bus.r.bits.last := true.B
    when(bus.r.ready) {
      readPending := false.B
    }
  }
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
      while (c.io.decodeCount.peek().litValue == 0 && cycles < 30) {
        c.clock.step()
        cycles += 1
      }

      c.io.decodeCount.expect(1.U)
      c.io.lastInst.expect("h00112233".U)

      c.clock.step()

      c.io.jumpTarget.poke((START_ADDR + 0x20).U)
      c.io.jump.poke(true.B)
      c.clock.step()
      c.io.jump.poke(false.B)

      cycles = 0
      while (c.io.decodeCount.peek().litValue < 2 && cycles < 30) {
        c.clock.step()
        cycles += 1
      }

      c.io.decodeCount.expect(2.U)
      c.io.lastInst.expect("h89abcdef".U)
    }
  }
}
