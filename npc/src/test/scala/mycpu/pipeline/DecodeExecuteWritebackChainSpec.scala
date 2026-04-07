package mycpu.pipeline

import HwOS.kernel._
import chisel3._
import chisel3.simulator.EphemeralSimulator._
import mycpu.common._
import org.scalatest.flatspec.AnyFlatSpec

final class DummyChainFetchProcess(localName: String)(implicit kernel: Kernel) extends HwProcess(localName) {
  private val pcReg = RegInit(START_ADDR.U(XLEN.W))

  val api: FetchApiDecl = new FetchApiDecl {
    override def writePC(nextPc: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_write_pc") { _ =>
      pcReg := nextPc
    }

    override def offsetPC(delta: SInt): HwInline[Unit] = HwInline.atomic(s"${name}_offset_pc") { _ =>
      pcReg := (pcReg.asSInt + delta).asUInt
    }

    override def currentPC(): HwInline[UInt] = HwInline.bindings(s"${name}_current_pc") { _ =>
      pcReg
    }
  }

  def RequestFetchApi(): HwInline[FetchApiDecl] = HwInline.bindings(s"${name}_fetch_api") { _ =>
    api
  }

  def pc: UInt = pcReg
  override def entry(): Unit = {}
}

final class DummyChainLsuProcess(localName: String)(implicit kernel: Kernel) extends HwProcess(localName) {
  val api: LsuApiDecl = new LsuApiDecl {
    override def loadWord(rd: UInt, addr: UInt): HwInline[Unit] = HwInline.thread(s"${name}_load_word") { _ =>
      SysCall.Return()
    }

    override def storeWord(addr: UInt, data: UInt): HwInline[Unit] = HwInline.thread(s"${name}_store_word") { _ =>
      SysCall.Return()
    }

    override def loadByte(rd: UInt, addr: UInt, unsigned: Bool): HwInline[Unit] = HwInline.thread(s"${name}_load_byte") { _ =>
      SysCall.Return()
    }

    override def loadHalf(rd: UInt, addr: UInt, unsigned: Bool): HwInline[Unit] = HwInline.thread(s"${name}_load_half") { _ =>
      SysCall.Return()
    }

    override def storeByte(addr: UInt, data: UInt): HwInline[Unit] = HwInline.thread(s"${name}_store_byte") { _ =>
      SysCall.Return()
    }

    override def storeHalf(addr: UInt, data: UInt): HwInline[Unit] = HwInline.thread(s"${name}_store_half") { _ =>
      SysCall.Return()
    }
  }

  override def entry(): Unit = {}
}

class DecodeExecuteWritebackChainHarness(instValue: UInt) extends Module {
  val io = IO(new Bundle {
    val done = Output(Bool())
    val x1 = Output(UInt(XLEN.W))
    val pc = Output(UInt(XLEN.W))
  })

  implicit val kernel: Kernel = new Kernel()

  object Init extends HwProcess("Init") {
    val links = new PipelineLinks
    val fetch = spawn(new DummyChainFetchProcess("Fetch"))
    val regfile = spawn(new RegfileProcess("Regfile"))
    val lsu = spawn(new DummyChainLsuProcess("Lsu"))
    val writeback = spawn(new WritebackProcess(links.fetch, links.regfile, "Writeback"))
    val execute = adopt(new ExecuteProcess(links.lsu, links.writeback, "Execute"))
    val decode = adopt(new DecodeProcess(links.execute, links.regfile, "Decode"))
    private val worker = createThread("Worker")
    private val daemon = createLogic("Daemon")
    private val doneReg = RegInit(false.B)
    private val x1Reg = RegInit(0.U(XLEN.W))
    private val pcReg = RegInit(0.U(XLEN.W))

    override def entry(): Unit = {
      worker.entry {
        val decodeApi = SysCall.Inline(decode.RequestDecodeApi())
        val regApi = SysCall.Inline(regfile.RequestRegfileApi())
        val fetchApi = SysCall.Inline(fetch.RequestFetchApi())

        SysCall.Call(decodeApi.decodeInst(instValue), "AfterDecode")
        worker.Step("AfterDecode") {
          x1Reg := SysCall.Inline(regApi.read(1.U))
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
        io.pc := pcReg
      }
    }
  }

  Init.links.fetch.bind(Init.fetch.api)
  Init.links.regfile.bind(Init.regfile.api)
  Init.links.lsu.bind(Init.lsu.api)
  Init.links.writeback.bind(Init.writeback.api)
  Init.links.execute.bind(Init.execute.api)
  Init.execute.build()
  Init.links.decode.bind(Init.decode.api)
  Init.decode.build()
  Init.build()
}

class DecodeExecuteWritebackChainSpec extends AnyFlatSpec {
  private def encodeAddi(rd: Int, rs1: Int, imm: Int): BigInt = {
    val imm12 = imm & 0xfff
    BigInt(imm12) << 20 | BigInt(rs1) << 15 | BigInt(0) << 12 | BigInt(rd) << 7 | BigInt(0x13)
  }

  private def encodeBeq(rs1: Int, rs2: Int, imm: Int): BigInt = {
    val v = imm & 0x1fff
    val bit12 = (v >> 12) & 1
    val bit11 = (v >> 11) & 1
    val bits10to5 = (v >> 5) & 0x3f
    val bits4to1 = (v >> 1) & 0xf
    (BigInt(bit12) << 31) |
    (BigInt(bits10to5) << 25) |
    (BigInt(rs2) << 20) |
    (BigInt(rs1) << 15) |
    (BigInt(0) << 12) |
    (BigInt(bits4to1) << 8) |
    (BigInt(bit11) << 7) |
    BigInt(0x63)
  }

  "Decode -> Execute -> Writeback chain" should "write back addi through the real backend chain" in {
    simulate(new DecodeExecuteWritebackChainHarness(encodeAddi(rd = 1, rs1 = 0, imm = 4).U(32.W))) { c =>
      c.reset.poke(true.B)
      c.clock.step()
      c.reset.poke(false.B)

      var cycles = 0
      while (c.io.done.peek().litValue == 0 && cycles < 40) {
        c.clock.step()
        cycles += 1
      }

      c.io.done.expect(true.B)
      c.io.x1.expect(4.U)
      c.io.pc.expect(START_ADDR.U)
    }
  }

  it should "redirect PC through real execute and writeback on a taken beq" in {
    simulate(new DecodeExecuteWritebackChainHarness(encodeBeq(rs1 = 0, rs2 = 0, imm = 16).U(32.W))) { c =>
      c.reset.poke(true.B)
      c.clock.step()
      c.reset.poke(false.B)

      var cycles = 0
      while (c.io.done.peek().litValue == 0 && cycles < 40) {
        c.clock.step()
        cycles += 1
      }

      c.io.done.expect(true.B)
      c.io.pc.expect((START_ADDR + 16).U)
    }
  }
}
