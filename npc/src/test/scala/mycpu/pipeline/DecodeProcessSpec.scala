package mycpu.pipeline

import HwOS.kernel._
import chisel3._
import chisel3.simulator.EphemeralSimulator._
import chisel3.util._
import mycpu.axi._
import mycpu.common._
import mycpu.mem.Memory
import org.scalatest.flatspec.AnyFlatSpec

class DecodeProcessHarness extends Module {
  val io = IO(new Bundle {
    val inst = Input(UInt(32.W))
    val done = Output(Bool())
    val beforePc = Output(UInt(XLEN.W))
    val afterPc = Output(UInt(XLEN.W))
  })

  implicit val kernel: Kernel = new Kernel()

  val bus = Wire(new AXI4Bundle(AXI_ID_WIDTH, XLEN, XLEN))
  bus.setAsMasterInit()

  object Init extends HwProcess("Init") {
    val links = new PipelineLinks
    private val memory = spawn(new Memory(bus, maxClients = 1))
    val regfile = spawn(new RegfileProcess("Regfile"))
    val fetch: FetchProcess = adopt(new FetchProcess(memory, links.decode, "Fetch"))
    val execute: ExecuteProcess = spawn(new ExecuteProcess(links.fetch, links.regfile, "Execute"))
    val decode: DecodeProcess = spawn(new DecodeProcess(links.execute, links.regfile, "Decode"))
    private val worker = createThread("Worker")
    private val daemon = createLogic("Daemon")

    private val doneReg = RegInit(false.B)
    private val beforePcReg = RegInit(0.U(XLEN.W))
    private val afterPcReg = RegInit(0.U(XLEN.W))

    override def entry(): Unit = {
      worker.entry {
        val fetchApi = SysCall.Inline(fetch.RequestFetchApi())
        val decodeApi = SysCall.Inline(decode.RequestDecodeApi())
        val beforePcWire = WireInit(0.U(XLEN.W))
        val afterPcWire = WireInit(0.U(XLEN.W))

        worker.Step("ReadBeforePc") {
          beforePcWire := SysCall.Inline(fetchApi.currentPC())
        }
        worker.Prev.edge.add {
          beforePcReg := beforePcWire
        }

        worker.Step("Decode") {
          SysCall.Inline(decodeApi.decodeInst(io.inst))
        }

        worker.Step("ReadAfterPc") {
          afterPcWire := SysCall.Inline(fetchApi.currentPC())
        }
        worker.Prev.edge.add {
          afterPcReg := afterPcWire
        }

        worker.Step("Finish") {
          doneReg := true.B
        }
        SysCall.Return()
      }

      daemon.run {
        when(!worker.active && !worker.done) {
          SysCall.Inline(SysCall.start(worker))
        }

        io.done := doneReg
        io.beforePc := beforePcReg
        io.afterPc := afterPcReg
      }
    }
  }

  Init.links.decode.bind(Init.decode.api)
  Init.links.fetch.bind(Init.fetch.api)
  Init.links.execute.bind(Init.execute.api)
  Init.links.regfile.bind(Init.regfile.api)
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

class DecodeProcessSpec extends AnyFlatSpec {
  private def encodeAddi(rd: Int, rs1: Int, imm: Int): BigInt = {
    val imm12 = imm & 0xfff
    BigInt(imm12) << 20 | BigInt(rs1) << 15 | BigInt(0) << 12 | BigInt(rd) << 7 | BigInt(0x13)
  }

  private def encodeJal(rd: Int, imm: Int): BigInt = {
    val imm21 = imm & 0x1fffff
    val bit20 = (imm21 >> 20) & 0x1
    val bits10To1 = (imm21 >> 1) & 0x3ff
    val bit11 = (imm21 >> 11) & 0x1
    val bits19To12 = (imm21 >> 12) & 0xff

    BigInt(bit20) << 31 |
    BigInt(bits19To12) << 12 |
    BigInt(bit11) << 20 |
    BigInt(bits10To1) << 21 |
    BigInt(rd) << 7 |
    BigInt(0x6f)
  }

  "DecodeProcess" should "leave pc unchanged for a non-control ALU instruction" in {
    simulate(new DecodeProcessHarness) { c =>
      c.reset.poke(true.B)
      c.io.inst.poke(encodeAddi(rd = 1, rs1 = 0, imm = 4).U(32.W))
      c.clock.step()
      c.reset.poke(false.B)

      var cycles = 0
      while (c.io.done.peek().litValue == 0 && cycles < 30) {
        c.clock.step()
        cycles += 1
      }

      c.io.done.expect(true.B)
      c.io.beforePc.expect(START_ADDR.U)
      c.io.afterPc.expect(START_ADDR.U)
    }
  }

  it should "redirect fetch pc for a jal instruction" in {
    simulate(new DecodeProcessHarness) { c =>
      val offset = 32

      c.reset.poke(true.B)
      c.io.inst.poke(encodeJal(rd = 1, imm = offset).U(32.W))
      c.clock.step()
      c.reset.poke(false.B)

      var cycles = 0
      while (c.io.done.peek().litValue == 0 && cycles < 30) {
        c.clock.step()
        cycles += 1
      }

      c.io.done.expect(true.B)
      c.io.beforePc.expect(START_ADDR.U)
      c.io.afterPc.expect((START_ADDR + offset).U)
    }
  }
}
