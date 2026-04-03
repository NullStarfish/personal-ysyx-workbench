package mycpu.pipeline

import HwOS.kernel._
import chisel3._
import chisel3.simulator.EphemeralSimulator._
import mycpu.axi._
import mycpu.common._
import mycpu.mem.Memory
import org.scalatest.flatspec.AnyFlatSpec

final class StubExecuteDecodeProcess(localName: String)(implicit kernel: Kernel)
    extends HwProcess(localName)
{
  val api: DecodeApiDecl = new DecodeApiDecl {
    override def decodeInst(inst: UInt): HwInline[Unit] = HwInline.thread(s"${name}_decode_inst") { _ =>
      SysCall.Return()
    }
  }
  override def entry(): Unit = {}
}

class ExecuteProcessHarness extends Module {
  val io = IO(new Bundle {
    val done = Output(Bool())
    val x1 = Output(UInt(XLEN.W))
    val x2 = Output(UInt(XLEN.W))
    val x3 = Output(UInt(XLEN.W))
    val x4 = Output(UInt(XLEN.W))
    val x5 = Output(UInt(XLEN.W))
    val x6 = Output(UInt(XLEN.W))
    val pc = Output(UInt(XLEN.W))
  })

  implicit val kernel: Kernel = new Kernel()

  val bus = Wire(new AXI4Bundle(AXI_ID_WIDTH, XLEN, XLEN))
  bus.setAsMasterInit()

  io.done := DontCare
  io.x1 := DontCare
  io.x2 := DontCare
  io.x3 := DontCare
  io.x4 := DontCare
  io.x5 := DontCare
  io.x6 := DontCare
  io.pc := DontCare

  object Init extends HwProcess("Init") {
    val links = new PipelineLinks
    val memory = spawn(new Memory(bus, maxClients = 2))
    val regfile = spawn(new RegfileProcess("Regfile"))
    lazy val decode = spawn(new StubExecuteDecodeProcess("Decode"))
    lazy val fetch: FetchProcess = adopt(new FetchProcess(memory, links.decode, "Fetch"))
    val lsu: LsuProcess = adopt(new LsuProcess(links.memory, links.writeback, "Lsu"))
    val writeback = spawn(new WritebackProcess(links.fetch, links.regfile, "Writeback"))
    val execute = spawn(new ExecuteProcess(links.lsu, links.writeback, "Execute"))
    private val worker = createThread("Worker")
    private val daemon = createLogic("Daemon")

    private val doneReg = RegInit(false.B)
    private val x1Reg = RegInit(0.U(XLEN.W))
    private val x2Reg = RegInit(0.U(XLEN.W))
    private val x3Reg = RegInit(0.U(XLEN.W))
    private val x4Reg = RegInit(0.U(XLEN.W))
    private val x5Reg = RegInit(0.U(XLEN.W))
    private val x6Reg = RegInit(0.U(XLEN.W))
    private val pcReg = RegInit(0.U(XLEN.W))

    override def entry(): Unit = {
      worker.entry {
        val exec = SysCall.Inline(execute.RequestExecuteApi())
        val regApi = SysCall.Inline(regfile.RequestRegfileApi())
        val fetchApi = SysCall.Inline(fetch.RequestFetchApi())
        worker.Step("Add") {
          SysCall.Inline(exec.add(1.U, 7.U(XLEN.W), 5.U(XLEN.W)))
        }

        worker.Step("Sub") {
          SysCall.Inline(exec.sub(2.U, 7.U(XLEN.W), 5.U(XLEN.W)))
        }

        worker.Step("Eq") {
          SysCall.Inline(exec.eq(9.U(XLEN.W), 9.U(XLEN.W), 16.S(XLEN.W)))
        }

        worker.Step("CsrRw") {
          SysCall.Inline(exec.csrRw(3.U, "h300".U, "h55".U(XLEN.W)))
        }

        worker.Step("CsrRs") {
          SysCall.Inline(exec.csrRs(4.U, "h300".U, "h0a".U(XLEN.W)))
        }

        worker.Step("CsrRc") {
          SysCall.Inline(exec.csrRc(5.U, "h300".U, "h0f".U(XLEN.W)))
        }

        worker.Step("CsrReadBack") {
          SysCall.Inline(exec.csrRs(6.U, "h300".U, 0.U(XLEN.W)))
        }

        worker.Step("Redirect") {
          SysCall.Inline(exec.redirect("h30000020".U(XLEN.W)))
        }

        worker.Step("Sample") {
          x1Reg := SysCall.Inline(regApi.read(1.U))
          x2Reg := SysCall.Inline(regApi.read(2.U))
          x3Reg := SysCall.Inline(regApi.read(3.U))
          x4Reg := SysCall.Inline(regApi.read(4.U))
          x5Reg := SysCall.Inline(regApi.read(5.U))
          x6Reg := SysCall.Inline(regApi.read(6.U))
          pcReg := SysCall.Inline(fetchApi.currentPC())
          doneReg := true.B
        }
        SysCall.Return()
      }

      daemon.run {
        when(!worker.active && !worker.done) {
          SysCall.Inline(SysCall.start(worker))
        }

        io.done := doneReg
        io.x1 := x1Reg
        io.x2 := x2Reg
        io.x3 := x3Reg
        io.x4 := x4Reg
        io.x5 := x5Reg
        io.x6 := x6Reg
        io.pc := pcReg
      }
    }
  }

  Init.links.decode.bind(Init.decode.api)
  Init.links.fetch.bind(Init.fetch.api)
  Init.links.regfile.bind(Init.regfile.api)
  Init.links.memory.bind(Init.memory.api(1))
  Init.links.lsu.bind(Init.lsu.api)
  Init.links.writeback.bind(Init.writeback.api)
  Init.lsu.build()
  Init.fetch.build()
  Init.build()

  bus.aw.ready := false.B
  bus.w.ready := false.B
  bus.b.valid := false.B
  bus.b.bits := DontCare
  bus.ar.ready := false.B
  bus.r.valid := false.B
  bus.r.bits := DontCare
}

class ExecuteProcessSpec extends AnyFlatSpec {
  "ExecuteProcess" should "compose alu, csr and writeback through real backend processes" in {
    simulate(new ExecuteProcessHarness) { c =>
      c.reset.poke(true.B)
      c.clock.step()
      c.reset.poke(false.B)

      var cycles = 0
      while (c.io.done.peek().litValue == 0 && cycles < 30) {
        c.clock.step()
        cycles += 1
      }

      c.io.done.expect(true.B)
      c.io.x1.expect(12.U)
      c.io.x2.expect(2.U)
      c.io.x3.expect(0.U)
      c.io.x4.expect("h55".U)
      c.io.x5.expect("h5f".U)
      c.io.x6.expect("h50".U)
      c.io.pc.expect("h30000020".U(XLEN.W))
    }
  }
}
