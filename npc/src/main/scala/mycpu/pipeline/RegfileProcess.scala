package mycpu.pipeline

import HwOS.kernel._
import chisel3._
import chisel3.util._
import mycpu.common._

final class RegfileProcess(
    localName: String = "Regfile",
)(implicit kernel: Kernel)
    extends HwProcess(localName) {

  private val Depth = 32
  private val AddrWidth = log2Ceil(Depth)
  private val MaxTokens = 8
  private val TokenWidth = log2Ceil(MaxTokens + 1)

  private val regs = RegInit(VecInit(Seq.fill(Depth)(0.U(XLEN.W))))
  private val tokenValid = RegInit(VecInit(Seq.fill(MaxTokens)(false.B)))
  private val tokenAddr = RegInit(VecInit(Seq.fill(MaxTokens)(0.U(AddrWidth.W))))
  val api: RegfileApiDecl = new RegfileApiDecl {
    override def read(addr: UInt): HwInline[UInt] = HwInline.atomic(s"${name}_read") { t =>
      val isZero = addr === 0.U
      val matchingPending = VecInit((0 until MaxTokens).map(i => tokenValid(i) && tokenAddr(i) === addr && !isZero))
      t.waitCondition(isZero || !matchingPending.asUInt.orR)
      Mux(isZero, 0.U(XLEN.W), regs(addr))
    }

    override def reserve(addr: UInt): HwInline[UInt] = HwInline.thread(s"${name}_reserve") { t =>
      val stepTag = s"${name}_reserve_${System.identityHashCode(new Object())}"
      val token = WireInit(0.U(TokenWidth.W))
      val freeMask = VecInit((0 until MaxTokens).map(i => !tokenValid(i)))
      val hasFree = freeMask.asUInt.orR
      val allocIdx = PriorityEncoder(freeMask)

      t.Step(s"${stepTag}_WaitFree") {
        t.waitCondition((addr === 0.U) || hasFree)
      }
      t.Step(s"${stepTag}_Allocate") {
        when(addr =/= 0.U) {
          token := allocIdx + 1.U
          printf(p"[REGFILE] reserve token=${Decimal(allocIdx + 1.U)} addr=${Decimal(addr)}\n")
          t.Prev.edge.add {
            tokenValid(allocIdx) := true.B
            tokenAddr(allocIdx) := addr
          }
        }
      }
      token
    }

    override def writebackAndClear(token: UInt, data: UInt): HwInline[Unit] =
      HwInline.atomic(s"${name}_writeback_and_clear") { t =>
        val tokenIdx = (token - 1.U)(log2Ceil(MaxTokens) - 1, 0)

        t.waitCondition((token === 0.U) || tokenValid(tokenIdx))
        when(token =/= 0.U) {
          val rd = tokenAddr(tokenIdx)
          printf(p"[REGFILE] commit token=${Decimal(token)} addr=${Decimal(rd)} data=${Hexadecimal(data)}\n")
          when(rd =/= 0.U) {
            regs(rd) := data
          }
          t.Prev.edge.add {
            tokenValid(tokenIdx) := false.B
            tokenAddr(tokenIdx) := 0.U
          }
        }
      }

    override def write(addr: UInt, data: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_write") { _ =>
      printf(p"[REGFILE] direct write addr=${Decimal(addr)} data=${Hexadecimal(data)}\n")
      when(addr =/= 0.U) {
        regs(addr) := data
      }
    }
  }

  val probeApi: RegfileProbeApiDecl = new RegfileProbeApiDecl {
    override def read(addr: UInt): HwInline[UInt] = HwInline.bindings(s"${name}_probe_read") { _ =>
      Mux(addr === 0.U, 0.U(XLEN.W), regs(addr))
    }

    override def readAllFlat(): HwInline[UInt] = HwInline.bindings(s"${name}_probe_flat") { _ =>
      regs.asUInt
    }
  }

  def RequestRegfileApi(): HwInline[RegfileApiDecl] = HwInline.bindings(s"${name}_regfile_api") { _ =>
    api
  }

  def RequestRegfileProbeApi(): HwInline[RegfileProbeApiDecl] = HwInline.bindings(s"${name}_regfile_probe_api") { _ =>
    probeApi
  }

  override def entry(): Unit = {}
}
