package mycpu.pipeline

import HwOS.kernel._
import chisel3._
import mycpu.common._
import mycpu.mem.DummyMemory

object Rv32eEncoders {
  def encodeLui(rd: Int, imm20: Int): BigInt =
    (BigInt(imm20 & 0xfffff) << 12) | (BigInt(rd) << 7) | BigInt(0x37)

  def encodeAuipc(rd: Int, imm20: Int): BigInt =
    (BigInt(imm20 & 0xfffff) << 12) | (BigInt(rd) << 7) | BigInt(0x17)

  def encodeJal(rd: Int, imm: Int): BigInt = {
    val v = imm & 0x1fffff
    val bit20 = (v >> 20) & 1
    val bits10to1 = (v >> 1) & 0x3ff
    val bit11 = (v >> 11) & 1
    val bits19to12 = (v >> 12) & 0xff
    (BigInt(bit20) << 31) |
    (BigInt(bits19to12) << 12) |
    (BigInt(bit11) << 20) |
    (BigInt(bits10to1) << 21) |
    (BigInt(rd) << 7) |
    BigInt(0x6f)
  }

  def encodeJalr(rd: Int, rs1: Int, imm: Int): BigInt = {
    val imm12 = imm & 0xfff
    (BigInt(imm12) << 20) | (BigInt(rs1) << 15) | (BigInt(0) << 12) | (BigInt(rd) << 7) | BigInt(0x67)
  }

  def encodeBranch(funct3: Int, rs1: Int, rs2: Int, imm: Int): BigInt = {
    val v = imm & 0x1fff
    val bit12 = (v >> 12) & 1
    val bit11 = (v >> 11) & 1
    val bits10to5 = (v >> 5) & 0x3f
    val bits4to1 = (v >> 1) & 0xf
    (BigInt(bit12) << 31) |
    (BigInt(bits10to5) << 25) |
    (BigInt(rs2) << 20) |
    (BigInt(rs1) << 15) |
    (BigInt(funct3) << 12) |
    (BigInt(bits4to1) << 8) |
    (BigInt(bit11) << 7) |
    BigInt(0x63)
  }

  def encodeLoad(funct3: Int, rd: Int, rs1: Int, imm: Int): BigInt = {
    val imm12 = imm & 0xfff
    (BigInt(imm12) << 20) | (BigInt(rs1) << 15) | (BigInt(funct3) << 12) | (BigInt(rd) << 7) | BigInt(0x03)
  }

  def encodeStore(funct3: Int, rs2: Int, rs1: Int, imm: Int): BigInt = {
    val imm12 = imm & 0xfff
    val immHi = (imm12 >> 5) & 0x7f
    val immLo = imm12 & 0x1f
    (BigInt(immHi) << 25) |
    (BigInt(rs2) << 20) |
    (BigInt(rs1) << 15) |
    (BigInt(funct3) << 12) |
    (BigInt(immLo) << 7) |
    BigInt(0x23)
  }

  def encodeOpImm(funct3: Int, rd: Int, rs1: Int, imm: Int): BigInt = {
    val imm12 = imm & 0xfff
    (BigInt(imm12) << 20) | (BigInt(rs1) << 15) | (BigInt(funct3) << 12) | (BigInt(rd) << 7) | BigInt(0x13)
  }

  def encodeShiftImm(funct3: Int, funct7: Int, rd: Int, rs1: Int, shamt: Int): BigInt = {
    val imm12 = ((funct7 & 0x7f) << 5) | (shamt & 0x1f)
    encodeOpImm(funct3, rd, rs1, imm12)
  }

  def encodeOp(funct3: Int, funct7: Int, rd: Int, rs1: Int, rs2: Int): BigInt =
    (BigInt(funct7 & 0x7f) << 25) |
    (BigInt(rs2) << 20) |
    (BigInt(rs1) << 15) |
    (BigInt(funct3) << 12) |
    (BigInt(rd) << 7) |
    BigInt(0x33)

  def nop: BigInt = encodeOpImm(0, 0, 0, 0)
}

class PipelineProgramHarness(
    programWords: Seq[BigInt],
    mutableWords: Seq[(BigInt, BigInt)],
    targetCommits: Int,
    extraReadonlyWords: Seq[(BigInt, BigInt)] = Seq.empty,
) extends Module {
  val io = IO(new Bundle {
    val done = Output(Bool())
    val regs = Output(Vec(16, UInt(XLEN.W)))
    val pc = Output(UInt(XLEN.W))
    val commitCount = Output(UInt(32.W))
    val lastCommitPc = Output(UInt(XLEN.W))
    val lastCommitInst = Output(UInt(32.W))
    val memWords = Output(Vec(mutableWords.length, UInt(XLEN.W)))
  })

  implicit val kernel: Kernel = new Kernel()

  private val readonlyWords =
    programWords.zipWithIndex.map { case (inst, idx) => BigInt(START_ADDR + idx * 4L) -> inst } ++ extraReadonlyWords

  object Init extends HwProcess("Init") {
    val links = new PipelineLinks
    val memory = spawn(new DummyMemory(
      readonlyWords = readonlyWords,
      mutableWords = mutableWords,
      maxClients = 2,
      localName = "DummyMemory",
    ))
    val regfile = spawn(new RegfileProcess("Regfile"))
    val trace = spawn(new DummyTracerProcess(localName = "Tracer"))
    val fetch: FetchProcess = adopt(new FetchProcess(memory, links.decode, links.trace, "Fetch"))
    val lsu: LsuProcess = adopt(new LsuProcess(links.memory, links.writeback, "Lsu"))
    val writeback = spawn(new WritebackProcess(fetch.api, regfile.api, trace.api, "Writeback"))
    val execute: ExecuteProcess = adopt(new ExecuteProcess(links.lsu, links.writeback, links.hazard, "Execute"))
    val hazard = spawn(new ControlHazardProcess(fetch.api, trace.api, Seq(() => execute.clearExecuteReqBuffer()), "ControlHazard"))
    val decode: DecodeProcess = adopt(new DecodeProcess(links.execute, links.regfile, "Decode"))
    private val observer = createThread("Observer")
    private val daemon = createLogic("Daemon")

    private val doneReg = RegInit(false.B)
    private val regsReg = RegInit(VecInit(Seq.fill(16)(0.U(XLEN.W))))
    private val pcReg = RegInit(0.U(XLEN.W))
    private val commitCountReg = RegInit(0.U(32.W))
    private val lastCommitPcReg = RegInit(0.U(XLEN.W))
    private val lastCommitInstReg = RegInit(0.U(32.W))

    override def entry(): Unit = {
      observer.entry {
        val fetchApi = SysCall.Inline(fetch.RequestFetchApi())
        val regProbeApi = SysCall.Inline(regfile.RequestRegfileProbeApi())

        observer.Step("WaitRetire") {
          observer.waitCondition(trace.commitCount >= targetCommits.U)
        }
        observer.Step("Sample") {
          val regsFlat = SysCall.Inline(regProbeApi.readAllFlat())
          for (idx <- 0 until 16) {
            regsReg(idx) := regsFlat((idx + 1) * XLEN - 1, idx * XLEN)
          }
          printf(
            p"[HARNESS] sample regs x1=${Hexadecimal(regsFlat(2 * XLEN - 1, 1 * XLEN))} x4=${Hexadecimal(regsFlat(5 * XLEN - 1, 4 * XLEN))} x5=${Hexadecimal(regsFlat(6 * XLEN - 1, 5 * XLEN))} x7=${Hexadecimal(regsFlat(8 * XLEN - 1, 7 * XLEN))}\n"
          )
          pcReg := SysCall.Inline(fetchApi.currentPC())
          commitCountReg := trace.commitCount
          lastCommitPcReg := trace.lastCommittedPc
          lastCommitInstReg := trace.lastCommittedInst
        }
        observer.Step("PublishDone") {
          doneReg := true.B
        }
        SysCall.Return()
      }

      daemon.run {
        when(!observer.active && !observer.done) {
          SysCall.Inline(SysCall.start(observer))
        }
        io.done := doneReg
        io.regs := regsReg
        io.pc := pcReg
        io.commitCount := commitCountReg
        io.lastCommitPc := lastCommitPcReg
        io.lastCommitInst := lastCommitInstReg
      }
    }
  }

  Init.links.decode.bind(Init.decode.api)
  Init.links.fetch.bind(Init.fetch.api)
  Init.links.trace.bind(Init.trace.api)
  Init.links.execute.bind(Init.execute.api)
  Init.links.regfile.bind(Init.regfile.api)
  Init.links.memory.bind(Init.memory.api(1))
  Init.links.lsu.bind(Init.lsu.api)
  Init.links.writeback.bind(Init.writeback.api)
  Init.links.hazard.bind(Init.hazard.api)
  Init.lsu.build()
  Init.execute.build()
  Init.decode.build()
  Init.fetch.build()
  Init.build()

  for (idx <- mutableWords.indices) {
    io.memWords(idx) := Init.memory.mutableData(idx)
  }
}
