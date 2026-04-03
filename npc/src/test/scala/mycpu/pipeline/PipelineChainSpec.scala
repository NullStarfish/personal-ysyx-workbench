package mycpu.pipeline

import HwOS.kernel._
import chisel3._
import chisel3.simulator.EphemeralSimulator._
import chisel3.util._
import mycpu.axi._
import mycpu.common._
import mycpu.mem.Memory
import org.scalatest.flatspec.AnyFlatSpec

class PipelineChainHarness extends Module {
  val io = IO(new Bundle {
    val x1 = Output(UInt(XLEN.W))
    val pc = Output(UInt(XLEN.W))
  })

  implicit val kernel: Kernel = new Kernel()

  val bus = Wire(new AXI4Bundle(AXI_ID_WIDTH, XLEN, XLEN))
  bus.setAsMasterInit()

  object Init extends HwProcess("Init") {
    val links = new PipelineLinks
    val memory = spawn(new Memory(bus, maxClients = 2))
    val regfile = spawn(new RegfileProcess("Regfile"))
    val fetch: FetchProcess = adopt(new FetchProcess(memory, links.decode, "Fetch"))
    val lsu: LsuProcess = adopt(new LsuProcess(links.memory, links.writeback, "Lsu"))
    val writeback = spawn(new WritebackProcess(links.fetch, links.regfile, "Writeback"))
    val execute: ExecuteProcess = adopt(new ExecuteProcess(links.lsu, links.writeback, "Execute"))
    val decode: DecodeProcess = adopt(new DecodeProcess(links.execute, links.regfile, "Decode"))
    private val observer = createThread("Observer")
    private val daemon = createLogic("Daemon")
    private val x1Reg = RegInit(0.U(XLEN.W))
    private val pcReg = RegInit(0.U(XLEN.W))

    override def entry(): Unit = {
      observer.entry {
        val fetchApi = SysCall.Inline(fetch.RequestFetchApi())
        val regApi = SysCall.Inline(regfile.RequestRegfileApi())

        observer.Step("Wait1") {}
        observer.Step("Wait2") {}
        observer.Step("Wait3") {}
        observer.Step("Wait4") {}
        observer.Step("Wait5") {}
        observer.Step("Wait6") {}
        observer.Step("Sample") {
          x1Reg := SysCall.Inline(regApi.read(1.U))
          pcReg := SysCall.Inline(fetchApi.currentPC())
        }
        SysCall.Return()
      }

      daemon.run {
        when(!observer.active && !observer.done) {
          SysCall.Inline(SysCall.start(observer))
        }
        io.x1 := x1Reg
        io.pc := pcReg
      }
    }
  }

  Init.links.decode.bind(Init.decode.api)
  Init.links.fetch.bind(Init.fetch.api)
  Init.links.execute.bind(Init.execute.api)
  Init.links.regfile.bind(Init.regfile.api)
  Init.links.memory.bind(Init.memory.api(1))
  Init.links.lsu.bind(Init.lsu.api)
  Init.links.writeback.bind(Init.writeback.api)
  Init.lsu.build()
  Init.execute.build()
  Init.decode.build()
  Init.fetch.build()
  Init.build()

  private val readPending = RegInit(false.B)
  private val readAddr = RegInit(0.U(XLEN.W))
  private val readData = MuxLookup(readAddr, 0.U(XLEN.W))(
    Seq(
      START_ADDR.U -> "h00400093".U(XLEN.W), // addi x1, x0, 4
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

class PipelineChainSpec extends AnyFlatSpec {
  "Pipeline chain" should "run fetch -> decode -> execute -> regfile for a simple addi" in {
    simulate(new PipelineChainHarness) { c =>
      c.reset.poke(true.B)
      c.clock.step()
      c.reset.poke(false.B)

      c.clock.step(20)

      c.io.x1.expect(4.U)
      c.io.pc.expect((START_ADDR + 4).U)
    }
  }
}
