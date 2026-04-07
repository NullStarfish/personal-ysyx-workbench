package mycpu.pipeline

import HwOS.kernel._
import chisel3._
import chisel3.simulator.EphemeralSimulator._
import mycpu.common._
import mycpu.mem.DummyMemory
import org.scalatest.flatspec.AnyFlatSpec

class PipelineMemoryChainHarness extends Module {
  val io = IO(new Bundle {
    val x1 = Output(UInt(XLEN.W))
    val x2 = Output(UInt(XLEN.W))
    val x3 = Output(UInt(XLEN.W))
    val x4 = Output(UInt(XLEN.W))
    val memWord0 = Output(UInt(XLEN.W))
    val memWord1 = Output(UInt(XLEN.W))
    val pc = Output(UInt(XLEN.W))
  })

  implicit val kernel: Kernel = new Kernel()

  private val instr0 = encodeLw(rd = 1, rs1 = 0, imm = 0)
  private val instr1 = encodeAddi(rd = 2, rs1 = 1, imm = 1)
  private val instr2 = encodeSw(rs2 = 2, rs1 = 0, imm = 4)
  private val instr3 = encodeLw(rd = 3, rs1 = 0, imm = 4)
  private val instr4 = encodeAddi(rd = 4, rs1 = 3, imm = 2)
  private val instr5 = "h00000013".U(32.W) // nop

  object Init extends HwProcess("Init") {
    val links = new PipelineLinks
    val memory = spawn(new DummyMemory(
      readonlyWords = Seq(
        BigInt(START_ADDR) -> instr0.litValue,
        BigInt(START_ADDR + 4L) -> instr1.litValue,
        BigInt(START_ADDR + 8L) -> instr2.litValue,
        BigInt(START_ADDR + 12L) -> instr3.litValue,
        BigInt(START_ADDR + 16L) -> instr4.litValue,
        BigInt(START_ADDR + 20L) -> instr5.litValue,
      ),
      mutableWords = Seq(
        BigInt(0) -> "h11223344".U(XLEN.W).litValue,
        BigInt(4) -> BigInt(0),
      ),
      maxClients = 2,
      localName = "DummyMemory",
    ))
    val regfile = spawn(new RegfileProcess("Regfile"))
    val fetch: FetchProcess = adopt(new FetchProcess(memory, links.decode, "Fetch"))
    val lsu: LsuProcess = adopt(new LsuProcess(links.memory, links.writeback, "Lsu"))
    val writeback = spawn(new WritebackProcess(links.fetch, links.regfile, "Writeback"))
    val execute: ExecuteProcess = adopt(new ExecuteProcess(links.lsu, links.writeback, "Execute"))
    val decode: DecodeProcess = adopt(new DecodeProcess(links.execute, links.regfile, "Decode"))
    private val observer = createThread("Observer")
    private val daemon = createLogic("Daemon")
    private val x1Reg = RegInit(0.U(XLEN.W))
    private val x2Reg = RegInit(0.U(XLEN.W))
    private val x3Reg = RegInit(0.U(XLEN.W))
    private val x4Reg = RegInit(0.U(XLEN.W))
    private val pcReg = RegInit(0.U(XLEN.W))

    override def entry(): Unit = {
      observer.entry {
        val fetchApi = SysCall.Inline(fetch.RequestFetchApi())
        val regApi = SysCall.Inline(regfile.RequestRegfileApi())

        observer.Step("WaitRetire") {
          val currentPc = SysCall.Inline(fetchApi.currentPC())
          observer.waitCondition(
            memory.mutableData(1) === "h11223345".U(XLEN.W) &&
            currentPc >= (START_ADDR + 24).U(XLEN.W),
          )
        }
        observer.Step("Sample") {
          x1Reg := SysCall.Inline(regApi.read(1.U))
          x2Reg := SysCall.Inline(regApi.read(2.U))
          x3Reg := SysCall.Inline(regApi.read(3.U))
          x4Reg := SysCall.Inline(regApi.read(4.U))
          pcReg := SysCall.Inline(fetchApi.currentPC())
        }
        SysCall.Return()
      }

      daemon.run {
        when(!observer.active && !observer.done) {
          SysCall.Inline(SysCall.start(observer))
        }
        io.x1 := x1Reg
        io.x2 := x2Reg
        io.x3 := x3Reg
        io.x4 := x4Reg
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
  io.memWord0 := Init.memory.mutableData(0)
  io.memWord1 := Init.memory.mutableData(1)

  private def encodeLw(rd: Int, rs1: Int, imm: Int): UInt = {
    val imm12 = imm & 0xfff
    (BigInt(imm12) << 20 | BigInt(rs1) << 15 | BigInt(2) << 12 | BigInt(rd) << 7 | BigInt(0x03)).U(32.W)
  }

  private def encodeAddi(rd: Int, rs1: Int, imm: Int): UInt = {
    val imm12 = imm & 0xfff
    (BigInt(imm12) << 20 | BigInt(rs1) << 15 | BigInt(0) << 12 | BigInt(rd) << 7 | BigInt(0x13)).U(32.W)
  }

  private def encodeSw(rs2: Int, rs1: Int, imm: Int): UInt = {
    val imm12 = imm & 0xfff
    val immHi = (imm12 >> 5) & 0x7f
    val immLo = imm12 & 0x1f
    (BigInt(immHi) << 25 | BigInt(rs2) << 20 | BigInt(rs1) << 15 | BigInt(2) << 12 | BigInt(immLo) << 7 | BigInt(0x23)).U(32.W)
  }

}

class PipelineMemoryChainSpec extends AnyFlatSpec {
  "Pipeline memory chain" should "run arithmetic and memory instructions through the real fetch/decode/execute/lsu/memory chain" in {
    simulate(new PipelineMemoryChainHarness) { c =>
      c.reset.poke(true.B)
      c.clock.step()
      c.reset.poke(false.B)

      var cycles = 0
      while ((c.io.memWord1.peek().litValue != BigInt("11223345", 16) || c.io.x4.peek().litValue != BigInt("11223347", 16)) && cycles < 160) {
        c.clock.step()
        cycles += 1
      }

      c.io.x1.expect("h11223344".U)
      c.io.x2.expect("h11223345".U)
      c.io.x3.expect("h11223345".U)
      c.io.x4.expect("h11223347".U)
      c.io.memWord0.expect("h11223344".U)
      c.io.memWord1.expect("h11223345".U)
      c.io.pc.expect((START_ADDR + 24).U)
    }
  }
}
