package mycpu.pipeline

import HwOS.kernel._
import chisel3._
import chisel3.util._
import mycpu.common._
import org.json4s.native.JsonParser.Token

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
  private val reserveResultReg = RegInit(0.U(TokenWidth.W))
  private val reserveAddrReg = RegInit(0.U(AddrWidth.W))
  private val reserveReqPending = RegInit(false.B)
  private val reserveReqInFlight = RegInit(false.B)
  private val reserveRespValid = RegInit(false.B)
  private val commitTokenReg = RegInit(0.U(TokenWidth.W))
  private val commitDataReg = RegInit(0.U(XLEN.W))
  private val commitIdxReg = RegInit(0.U(log2Ceil(MaxTokens).W))
  private val reserveFreeMask = Wire(Vec(MaxTokens, Bool()))
  private val reserveHasFree = Wire(Bool())
  private val reserveAllocIdx = Wire(UInt(log2Ceil(MaxTokens).W))

  for (i <- 0 until MaxTokens) {
    reserveFreeMask(i) := !tokenValid(i)
  }
  reserveHasFree := reserveFreeMask.asUInt.orR
  reserveAllocIdx := PriorityEncoder(reserveFreeMask)

  val reserveResult = RegInit(0.U(TokenWidth.W))
  val api: RegfileApiDecl = new RegfileApiDecl {
    override def read(addr: UInt): HwInline[UInt] = HwInline.atomic(s"${name}_read") { t =>
      t.waitCondition((addr === 0.U) || !(VecInit((0 until MaxTokens).map(i => tokenValid(i) && tokenAddr(i) === addr)).asUInt.orR))
      Mux(addr === 0.U, 0.U(XLEN.W), regs(addr))
    }

    override def reserve(addr: UInt): HwInline[UInt] = HwInline.thread(s"${name}_reserve") { t =>
      val stepTag = s"${name}_reserve_${System.identityHashCode(new Object())}"
      t.Step(s"${stepTag}_CaptureAddr") {
        t.Prev.edge.add {
          reserveAddrReg := addr
        }
      }
      t.Step(s"${stepTag}_WaitFree") {
        t.waitCondition((reserveAddrReg === 0.U) || reserveHasFree)
      }
      t.Step(s"${stepTag}_Allocate") {
        reserveResult := Mux(reserveAddrReg === 0.U, 0.U(TokenWidth.W), reserveAllocIdx + 1.U)
        when(reserveAddrReg =/= 0.U) {
          tokenValid(reserveAllocIdx) := true.B
          tokenAddr(reserveAllocIdx) := reserveAddrReg
        }
        t.waitCondition(true.B)
      }
      reserveResult
    }

    override def reservePath(addr: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_reserve_path") { t =>
      t.waitCondition(!reserveReqInFlight)
      when(!reserveReqInFlight) {
        reserveAddrReg := addr
        reserveResultReg := 0.U
        reserveReqPending := true.B
        reserveReqInFlight := true.B
        reserveRespValid := false.B
      }
    }

    override def reserveDone(): HwInline[Bool] = HwInline.bindings(s"${name}_reserve_done") { _ =>
      reserveRespValid
    }

    override def reserveToken(): HwInline[UInt] = HwInline.bindings(s"${name}_reserve_token") { _ =>
      reserveResultReg
    }

    override def consumeReserveResp(): HwInline[Unit] = HwInline.atomic(s"${name}_consume_reserve_resp") { _ =>
      reserveRespValid := false.B
      reserveReqInFlight := false.B
      reserveResultReg := 0.U
    }

    override def writebackAndClear(token: UInt, data: UInt): HwInline[Unit] =
      HwInline.thread(s"${name}_writeback_and_clear") { t =>
        val stepTag = s"${name}_writeback_and_clear_${System.identityHashCode(new Object())}"

        t.Step(s"${stepTag}_Capture") {
          t.Prev.edge.add {
            commitTokenReg := token
            commitDataReg := data
            commitIdxReg := Mux(token === 0.U, 0.U, (token - 1.U)(log2Ceil(MaxTokens) - 1, 0))
          }
        }
        t.Step(s"${stepTag}_WaitValid") {
          t.waitCondition((commitTokenReg === 0.U) || tokenValid(commitIdxReg))
        }
        t.Step(s"${stepTag}_Write") {
          when(commitTokenReg =/= 0.U) {
            when(tokenAddr(commitIdxReg) =/= 0.U) {
              regs(tokenAddr(commitIdxReg)) := commitDataReg
            }
          }
        }
        t.Step(s"${stepTag}_Clear") {
          t.Prev.edge.add {
            when(commitTokenReg =/= 0.U) {
              tokenValid(commitIdxReg) := false.B
              tokenAddr(commitIdxReg) := 0.U
            }
            commitTokenReg := 0.U
            commitDataReg := 0.U
            commitIdxReg := 0.U
          }
        }
      }

    override def write(addr: UInt, data: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_write") { _ =>
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

  override def entry(): Unit = {
    val daemon = createLogic("ReserveDaemon")
    daemon.run {
      val freeMask = VecInit((0 until MaxTokens).map(i => !tokenValid(i)))
      val hasFree = freeMask.asUInt.orR
      val allocIdx = PriorityEncoder(freeMask)

      when(reserveReqPending && reserveAddrReg === 0.U) {
        reserveReqPending := false.B
        reserveRespValid := true.B
        reserveResultReg := 0.U
      }.elsewhen(reserveReqPending && hasFree) {
        val tokenValue = allocIdx + 1.U
        tokenValid(allocIdx) := true.B
        tokenAddr(allocIdx) := reserveAddrReg
        reserveReqPending := false.B
        reserveRespValid := true.B
        reserveResultReg := tokenValue
      }
    }
  }
}
