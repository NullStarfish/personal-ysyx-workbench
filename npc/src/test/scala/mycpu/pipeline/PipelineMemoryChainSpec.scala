package mycpu.pipeline

import HwOS.kernel._
import chisel3._
import chisel3.simulator.EphemeralSimulator._
import chisel3.util._
import mycpu.axi._
import mycpu.common._
import mycpu.mem.Memory
import org.scalatest.flatspec.AnyFlatSpec

class PipelineMemoryChainHarness extends Module {
  val io = IO(new Bundle {
    val x1 = Output(UInt(XLEN.W))
    val memWord0 = Output(UInt(XLEN.W))
    val memWord1 = Output(UInt(XLEN.W))
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
        observer.Step("Wait7") {}
        observer.Step("Wait8") {}
        observer.Step("Wait9") {}
        observer.Step("Wait10") {}
        observer.Step("Wait11") {}
        observer.Step("Wait12") {}
        observer.Step("Wait13") {}
        observer.Step("Wait14") {}
        observer.Step("Wait15") {}
        observer.Step("Wait16") {}
        observer.Step("Wait17") {}
        observer.Step("Wait18") {}
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

  private def encodeLw(rd: Int, rs1: Int, imm: Int): UInt = {
    val imm12 = imm & 0xfff
    (BigInt(imm12) << 20 | BigInt(rs1) << 15 | BigInt(2) << 12 | BigInt(rd) << 7 | BigInt(0x03)).U(32.W)
  }

  private def encodeSw(rs2: Int, rs1: Int, imm: Int): UInt = {
    val imm12 = imm & 0xfff
    val immHi = (imm12 >> 5) & 0x7f
    val immLo = imm12 & 0x1f
    (BigInt(immHi) << 25 | BigInt(rs2) << 20 | BigInt(rs1) << 15 | BigInt(2) << 12 | BigInt(immLo) << 7 | BigInt(0x23)).U(32.W)
  }

  private val instr0 = encodeLw(rd = 1, rs1 = 0, imm = 0)
  private val instr1 = encodeSw(rs2 = 1, rs1 = 0, imm = 4)
  private val instr2 = "h00000013".U(32.W) // nop

  private val dataWord0 = RegInit("h11223344".U(XLEN.W))
  private val dataWord1 = RegInit(0.U(XLEN.W))

  io.memWord0 := dataWord0
  io.memWord1 := dataWord1

  private val readPending = RegInit(false.B)
  private val readAddr = RegInit(0.U(XLEN.W))
  private val writePending = RegInit(false.B)

  private val readData = WireDefault(0.U(XLEN.W))
  when(readAddr === START_ADDR.U) {
    readData := instr0
  }.elsewhen(readAddr === (START_ADDR + 4).U) {
    readData := instr1
  }.elsewhen(readAddr === (START_ADDR + 8).U) {
    readData := instr2
  }.elsewhen(readAddr === 0.U) {
    readData := dataWord0
  }.elsewhen(readAddr === 4.U) {
    readData := dataWord1
  }

  bus.aw.ready := true.B
  bus.w.ready := true.B
  bus.b.valid := writePending
  bus.b.bits.id := 0.U
  bus.b.bits.resp := 0.U

  bus.ar.ready := !readPending
  bus.r.valid := readPending
  bus.r.bits.id := 0.U
  bus.r.bits.data := readData
  bus.r.bits.resp := 0.U
  bus.r.bits.last := true.B

  when(bus.ar.valid && bus.ar.ready) {
    readPending := true.B
    readAddr := bus.ar.bits.addr
  }

  when(bus.r.valid && bus.r.ready) {
    readPending := false.B
  }

  when(bus.aw.valid && bus.aw.ready && bus.w.valid && bus.w.ready) {
    val oldValue = Mux(bus.aw.bits.addr === 0.U, dataWord0, dataWord1)
    val mergedBytes = Wire(Vec(XLEN / 8, UInt(8.W)))
    for (byteIdx <- 0 until (XLEN / 8)) {
      mergedBytes(byteIdx) := Mux(
        bus.w.bits.strb(byteIdx),
        bus.w.bits.data(byteIdx * 8 + 7, byteIdx * 8),
        oldValue(byteIdx * 8 + 7, byteIdx * 8),
      )
    }

    when(bus.aw.bits.addr === 0.U) {
      dataWord0 := mergedBytes.asUInt
    }.elsewhen(bus.aw.bits.addr === 4.U) {
      dataWord1 := mergedBytes.asUInt
    }
    writePending := true.B
  }

  when(bus.b.valid && bus.b.ready) {
    writePending := false.B
  }
}

class PipelineMemoryChainSpec extends AnyFlatSpec {
  "Pipeline memory chain" should "run lw then sw through the real fetch/decode/execute/lsu/memory chain" in {
    simulate(new PipelineMemoryChainHarness) { c =>
      c.reset.poke(true.B)
      c.clock.step()
      c.reset.poke(false.B)

      c.clock.step(40)

      c.io.x1.expect("h11223344".U)
      c.io.memWord0.expect("h11223344".U)
      c.io.memWord1.expect("h11223344".U)
    }
  }
}
