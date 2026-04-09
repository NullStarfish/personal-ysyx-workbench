package mycpu.pipeline

import HwOS.kernel._
import chisel3._
import chisel3.simulator.EphemeralSimulator._
import mycpu.common._
import org.scalatest.flatspec.AnyFlatSpec

final class DummyDecodeRegfileProcess(localName: String)(implicit kernel: Kernel) extends HwProcess(localName) {
  private val regs = RegInit(VecInit(Seq.fill(32)(0.U(XLEN.W))))
  private val reserveResult = RegInit(0.U(4.W))

  regs(1) := 10.U
  regs(2) := 20.U
  regs(3) := 30.U

  val api: RegfileApiDecl = new RegfileApiDecl {
    override def read(addr: UInt): HwInline[UInt] = HwInline.bindings(s"${name}_read") { _ =>
      Mux(addr === 0.U, 0.U(XLEN.W), regs(addr))
    }

    override def reserve(addr: UInt): HwInline[UInt] = HwInline.thread(s"${name}_reserve") { t =>
      val stepTag = s"${name}_reserve_${System.identityHashCode(new Object())}"
      t.Step(s"${stepTag}_Capture") {
        reserveResult := addr
        t.waitCondition(true.B)
      }
      reserveResult
    }

    override def reservePath(addr: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_reserve_path") { _ => () }

    override def reserveDone(): HwInline[Bool] = HwInline.bindings(s"${name}_reserve_done") { _ => true.B }

    override def reserveToken(): HwInline[UInt] = HwInline.bindings(s"${name}_reserve_token") { _ => 0.U(4.W) }

    override def consumeReserveResp(): HwInline[Unit] = HwInline.atomic(s"${name}_consume_reserve_resp") { _ => () }

    override def writebackAndClear(token: UInt, data: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_writeback_and_clear") { _ =>
      when(token =/= 0.U) {
        regs(token) := data
      }
    }

    override def write(addr: UInt, data: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_write") { _ =>
      when(addr =/= 0.U) {
        regs(addr) := data
      }
    }
  }

  def RequestRegfileApi(): HwInline[RegfileApiDecl] = HwInline.bindings(s"${name}_regfile_api") { _ =>
    api
  }

  override def entry(): Unit = {}
}

final class DummyDecodeExecuteProcess(localName: String)(implicit kernel: Kernel) extends HwProcess(localName) {
  val opKind = RegInit(0.U(8.W))
  val rdReg = RegInit(0.U(5.W))
  val lhsReg = RegInit(0.U(XLEN.W))
  val rhsReg = RegInit(0.U(XLEN.W))
  val targetReg = RegInit(0.U(XLEN.W))
  val pcReg = RegInit(0.U(XLEN.W))
  val unsignedReg = RegInit(false.B)

  private def record(
      kind: UInt,
      rd: UInt = 0.U,
      lhs: UInt = 0.U,
      rhs: UInt = 0.U,
      target: UInt = 0.U,
      pc: UInt = 0.U,
      unsigned: Bool = false.B,
  ): Unit = {
    opKind := kind
    rdReg := rd
    lhsReg := lhs
    rhsReg := rhs
    targetReg := target
    pcReg := pc
    unsignedReg := unsigned
  }

  val api: ExecuteApiDecl = new ExecuteApiDecl {
    override def execPath(): HwInline[Unit] = HwInline.thread(s"${name}_exec_path") { t =>
      t.Step(s"${name}_Exec_Idle") {}
    }
    override def add(rd: UInt, wbToken: UInt, lhs: UInt, rhs: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_add") { _ => record(1.U, rd, lhs, rhs) }
    override def sub(rd: UInt, wbToken: UInt, lhs: UInt, rhs: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_sub") { _ => record(2.U, rd, lhs, rhs) }
    override def and(rd: UInt, wbToken: UInt, lhs: UInt, rhs: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_and") { _ => record(3.U, rd, lhs, rhs) }
    override def or(rd: UInt, wbToken: UInt, lhs: UInt, rhs: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_or") { _ => record(4.U, rd, lhs, rhs) }
    override def xor(rd: UInt, wbToken: UInt, lhs: UInt, rhs: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_xor") { _ => record(5.U, rd, lhs, rhs) }
    override def sll(rd: UInt, wbToken: UInt, lhs: UInt, rhs: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_sll") { _ => record(6.U, rd, lhs, rhs) }
    override def srl(rd: UInt, wbToken: UInt, lhs: UInt, rhs: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_srl") { _ => record(7.U, rd, lhs, rhs) }
    override def sra(rd: UInt, wbToken: UInt, lhs: UInt, rhs: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_sra") { _ => record(8.U, rd, lhs, rhs) }
    override def slt(rd: UInt, wbToken: UInt, lhs: UInt, rhs: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_slt") { _ => record(9.U, rd, lhs, rhs) }
    override def sltu(rd: UInt, wbToken: UInt, lhs: UInt, rhs: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_sltu") { _ => record(10.U, rd, lhs, rhs) }
    override def writeReg(rd: UInt, wbToken: UInt, data: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_write_reg") { _ => record(11.U, rd, data) }
    override def redirect(nextPc: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_redirect") { _ => record(12.U, target = nextPc) }
    override def redirectRelative(delta: SInt): HwInline[Unit] = HwInline.atomic(s"${name}_redirect_rel") { _ => record(13.U, target = delta.asUInt) }
    override def eq(lhs: UInt, rhs: UInt, pc: UInt, offset: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_eq") { _ => record(14.U, lhs = lhs, rhs = rhs, target = offset, pc = pc) }
    override def ne(lhs: UInt, rhs: UInt, pc: UInt, offset: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_ne") { _ => record(15.U, lhs = lhs, rhs = rhs, target = offset, pc = pc) }
    override def lt(lhs: UInt, rhs: UInt, pc: UInt, offset: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_lt") { _ => record(16.U, lhs = lhs, rhs = rhs, target = offset, pc = pc) }
    override def ltu(lhs: UInt, rhs: UInt, pc: UInt, offset: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_ltu") { _ => record(17.U, lhs = lhs, rhs = rhs, target = offset, pc = pc) }
    override def ge(lhs: UInt, rhs: UInt, pc: UInt, offset: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_ge") { _ => record(30.U, lhs = lhs, rhs = rhs, target = offset, pc = pc) }
    override def geu(lhs: UInt, rhs: UInt, pc: UInt, offset: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_geu") { _ => record(31.U, lhs = lhs, rhs = rhs, target = offset, pc = pc) }
    override def memPath(): HwInline[Unit] = execPath()
    override def loadWord(rd: UInt, wbToken: UInt, base: UInt, offset: UInt): HwInline[Unit] =
      HwInline.atomic(s"${name}_load_word") { _ =>
        record(18.U, rd, base, offset)
      }
    override def storeWord(base: UInt, offset: UInt, data: UInt): HwInline[Unit] =
      HwInline.atomic(s"${name}_store_word") { _ =>
        record(19.U, lhs = base, rhs = offset, target = data)
      }
    override def load(rd: UInt, wbToken: UInt, base: UInt, offset: UInt, kind: UInt, unsigned: Bool): HwInline[Unit] =
      HwInline.atomic(s"${name}_load") { _ =>
        record(32.U, rd, base, offset, target = kind, unsigned = unsigned)
      }
    override def store(base: UInt, offset: UInt, data: UInt, kind: UInt): HwInline[Unit] =
      HwInline.atomic(s"${name}_store") { _ =>
        record(33.U, lhs = base, rhs = offset, target = data, pc = kind)
      }
    override def mem(isLoad: Bool, rd: UInt, wbToken: UInt, base: UInt, offset: UInt, data: UInt, kind: UInt, unsigned: Bool): HwInline[Unit] =
      HwInline.atomic(s"${name}_mem") { _ =>
        record(
          Mux(isLoad, 32.U, 33.U),
          rd,
          base,
          offset,
          target = Mux(isLoad, kind, data),
          pc = Mux(isLoad, 0.U, kind),
          unsigned = unsigned,
        )
      }
    override def loadByte(rd: UInt, wbToken: UInt, base: UInt, offset: UInt, unsigned: Bool): HwInline[Unit] =
      HwInline.atomic(s"${name}_load_byte") { _ =>
        record(20.U, rd, base, offset, unsigned = unsigned)
      }
    override def loadHalf(rd: UInt, wbToken: UInt, base: UInt, offset: UInt, unsigned: Bool): HwInline[Unit] =
      HwInline.atomic(s"${name}_load_half") { _ =>
        record(21.U, rd, base, offset, unsigned = unsigned)
      }
    override def storeByte(base: UInt, offset: UInt, data: UInt): HwInline[Unit] =
      HwInline.atomic(s"${name}_store_byte") { _ =>
        record(22.U, lhs = base, rhs = offset, target = data)
      }
    override def storeHalf(base: UInt, offset: UInt, data: UInt): HwInline[Unit] =
      HwInline.atomic(s"${name}_store_half") { _ =>
        record(23.U, lhs = base, rhs = offset, target = data)
      }
    override def auipc(rd: UInt, wbToken: UInt, pc: UInt, imm: UInt): HwInline[Unit] =
      HwInline.atomic(s"${name}_auipc") { _ => record(27.U, rd, pc, imm) }
    override def jal(rd: UInt, wbToken: UInt, pc: UInt, offset: SInt): HwInline[Unit] =
      HwInline.atomic(s"${name}_jal") { _ => record(28.U, rd, pc, target = offset.asUInt) }
    override def jalr(rd: UInt, wbToken: UInt, pc: UInt, base: UInt, offset: UInt): HwInline[Unit] =
      HwInline.atomic(s"${name}_jalr") { _ => record(29.U, rd, pc, base, offset) }
    override def csrRw(rd: UInt, wbToken: UInt, addr: UInt, src: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_csr_rw") { _ => record(24.U, rd, addr, src) }
    override def csrRs(rd: UInt, wbToken: UInt, addr: UInt, src: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_csr_rs") { _ => record(25.U, rd, addr, src) }
    override def csrRc(rd: UInt, wbToken: UInt, addr: UInt, src: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_csr_rc") { _ => record(26.U, rd, addr, src) }
  }

  def RequestExecuteApi(): HwInline[ExecuteApiDecl] = HwInline.bindings(s"${name}_execute_api") { _ =>
    api
  }

  override def entry(): Unit = {}
}

class DecodeProcessHarness extends Module {
  val io = IO(new Bundle {
    val inst = Input(UInt(32.W))
    val done = Output(Bool())
    val opKind = Output(UInt(8.W))
    val rd = Output(UInt(5.W))
    val lhs = Output(UInt(XLEN.W))
    val rhs = Output(UInt(XLEN.W))
    val target = Output(UInt(XLEN.W))
    val pc = Output(UInt(XLEN.W))
    val unsigned = Output(Bool())
  })

  implicit val kernel: Kernel = new Kernel()

  object Init extends HwProcess("Init") {
    val links = new PipelineLinks
    val regfile = spawn(new DummyDecodeRegfileProcess("Regfile"))
    val execute = spawn(new DummyDecodeExecuteProcess("Execute"))
    val decode: DecodeProcess = adopt(new DecodeProcess(links.execute, links.regfile, "Decode"))
    private val worker = createThread("Worker")
    private val daemon = createLogic("Daemon")

    private val doneReg = RegInit(false.B)

    override def entry(): Unit = {
      worker.entry {
        val decodeApi = SysCall.Inline(decode.RequestDecodeApi())
        SysCall.Call(decodeApi.decodeInst(START_ADDR.U(XLEN.W), io.inst), "AfterDecode")
        worker.Step("AfterDecode") {
          doneReg := true.B
        }
        SysCall.Return()
      }

      daemon.run {
        when(!worker.active && !worker.done) {
          SysCall.Inline(SysCall.start(worker))
        }

        io.done := doneReg
        io.opKind := execute.opKind
        io.rd := execute.rdReg
        io.lhs := execute.lhsReg
        io.rhs := execute.rhsReg
        io.target := execute.targetReg
        io.pc := execute.pcReg
        io.unsigned := execute.unsignedReg
      }
    }
  }

  Init.links.execute.bind(Init.execute.api)
  Init.links.regfile.bind(Init.regfile.api)
  Init.decode.build()
  Init.build()
}

class DecodeProcessSpec extends AnyFlatSpec {
  private def encodeAddi(rd: Int, rs1: Int, imm: Int): BigInt = {
    val imm12 = imm & 0xfff
    BigInt(imm12) << 20 | BigInt(rs1) << 15 | BigInt(0) << 12 | BigInt(rd) << 7 | BigInt(0x13)
  }

  private def encodeLw(rd: Int, rs1: Int, imm: Int): BigInt = {
    val imm12 = imm & 0xfff
    BigInt(imm12) << 20 | BigInt(rs1) << 15 | BigInt(2) << 12 | BigInt(rd) << 7 | BigInt(0x03)
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

  "DecodeProcess" should "dispatch addi using decoded register value and immediate" in {
    simulate(new DecodeProcessHarness) { c =>
      c.reset.poke(true.B)
      c.io.inst.poke(encodeAddi(rd = 5, rs1 = 1, imm = 4).U(32.W))
      c.clock.step()
      c.reset.poke(false.B)

      var cycles = 0
      while (c.io.done.peek().litValue == 0 && cycles < 30) {
        c.clock.step()
        cycles += 1
      }

      c.io.done.expect(true.B)
      c.io.opKind.expect(1.U)
      c.io.rd.expect(5.U)
      c.io.lhs.expect(10.U)
      c.io.rhs.expect(4.U)
    }
  }

  it should "dispatch lw with base register, offset and destination register" in {
    simulate(new DecodeProcessHarness) { c =>
      c.reset.poke(true.B)
      c.io.inst.poke(encodeLw(rd = 6, rs1 = 2, imm = 12).U(32.W))
      c.clock.step()
      c.reset.poke(false.B)

      var cycles = 0
      while (c.io.done.peek().litValue == 0 && cycles < 30) {
        c.clock.step()
        cycles += 1
      }

      c.io.done.expect(true.B)
      c.io.opKind.expect(18.U)
      c.io.rd.expect(6.U)
      c.io.lhs.expect(20.U)
      c.io.rhs.expect(12.U)
      c.io.target.expect(0.U)
    }
  }

  it should "dispatch beq with decoded operands and branch target" in {
    simulate(new DecodeProcessHarness) { c =>
      c.reset.poke(true.B)
      c.io.inst.poke(encodeBeq(rs1 = 1, rs2 = 2, imm = 16).U(32.W))
      c.clock.step()
      c.reset.poke(false.B)

      var cycles = 0
      while (c.io.done.peek().litValue == 0 && cycles < 30) {
        c.clock.step()
        cycles += 1
      }

      c.io.done.expect(true.B)
      c.io.opKind.expect(14.U)
      c.io.lhs.expect(10.U)
      c.io.rhs.expect(20.U)
      c.io.pc.expect(START_ADDR.U)
      c.io.target.expect(16.U)
    }
  }
}
