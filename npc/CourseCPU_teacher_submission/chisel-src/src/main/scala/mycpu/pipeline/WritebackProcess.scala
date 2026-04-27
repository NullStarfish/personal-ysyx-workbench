package mycpu.pipeline

import HwOS.kernel._
import chisel3._

final class WritebackProcess(
    fetchApi: FetchApiDecl,
    regApi: RegfileApiDecl,
    traceApi: TraceApiDecl,
    localName: String = "Writeback",
)(implicit kernel: Kernel)
    extends HwProcess(localName) {

  final class WbReq extends Bundle {
    val kind = UInt(3.W)
    val wbToken = UInt(4.W)
    val data = UInt(32.W)
    val nextPc = UInt(32.W)
    val delta = SInt(32.W)
  }

  private val wbReqBuffer = spawn(new PipelineBuffer(new WbReq, "WbReqBuffer"))
  private val wbWorker = createThread("WbWorker")

  private val WB_WRITE_REG = 0.U(3.W)
  private val WB_WRITE_REG_REDIRECT = 1.U(3.W)
  private val WB_REDIRECT = 2.U(3.W)
  private val WB_REDIRECT_REL = 3.U(3.W)

  private val reqReg = RegInit(0.U.asTypeOf(new WbReq))
  private val writeRegPacket = WireDefault(0.U.asTypeOf(new WbReq))
  private val writeRegAndRedirectPacket = WireDefault(0.U.asTypeOf(new WbReq))
  private val redirectPacket = WireDefault(0.U.asTypeOf(new WbReq))
  private val redirectRelativePacket = WireDefault(0.U.asTypeOf(new WbReq))
  override def entry(): Unit = {
    wbWorker.entry {
      wbWorker.Step("WaitReq") {
        wbWorker.waitCondition(wbReqBuffer.valid)
      }
      wbWorker.Step("TakeReq") {
        reqReg := SysCall.Inline(wbReqBuffer.pop())
      }
      wbWorker.Step("Dispatch") {
        when(reqReg.kind === WB_WRITE_REG) {
          wbWorker.jump(wbWorker.stepRef("WriteRegStart"))
        }.elsewhen(reqReg.kind === WB_WRITE_REG_REDIRECT) {
          wbWorker.jump(wbWorker.stepRef("WriteRegRedirectStart"))
        }.elsewhen(reqReg.kind === WB_REDIRECT) {
          wbWorker.jump(wbWorker.stepRef("Redirect"))
        }.elsewhen(reqReg.kind === WB_REDIRECT_REL) {
          wbWorker.jump(wbWorker.stepRef("RedirectRelative"))
        }.otherwise {
          wbWorker.jump(wbWorker.stepRef("WaitReq"))
        }
      }

      wbWorker.Step("WriteRegStart") {}
      SysCall.Inline(regApi.writebackAndClear(reqReg.wbToken, reqReg.data))
      wbWorker.Step("AfterWriteReg") {
        SysCall.Inline(traceApi.commit())
        wbWorker.jump(wbWorker.stepRef("WaitReq"))
      }

      wbWorker.Step("WriteRegRedirectStart") {}
      SysCall.Inline(regApi.writebackAndClear(reqReg.wbToken, reqReg.data))
      wbWorker.Step("AfterWriteRegRedirect") {
        SysCall.Inline(fetchApi.writePC(reqReg.nextPc))
        SysCall.Inline(traceApi.commit())
        wbWorker.jump(wbWorker.stepRef("WaitReq"))
      }

      wbWorker.Step("Redirect") {
        SysCall.Inline(fetchApi.writePC(reqReg.nextPc))
        SysCall.Inline(traceApi.commit())
        wbWorker.jump(wbWorker.stepRef("WaitReq"))
      }

      wbWorker.Step("RedirectRelative") {
        SysCall.Inline(fetchApi.offsetPC(reqReg.delta))
        SysCall.Inline(traceApi.commit())
        wbWorker.jump(wbWorker.stepRef("WaitReq"))
      }
    }

    val daemon = createLogic("Daemon")
    daemon.run {
      when(!wbWorker.active) {
        SysCall.Inline(SysCall.start(wbWorker))
      }
    }
  }

  val api: WritebackApiDecl = new WritebackApiDecl {
    override def wbPath(): HwInline[Unit] = HwInline.thread(s"${name}_wb_path") { t =>
      t.Step(s"${name}_wb_path_idle") {}
    }

    override def writeReg(token: UInt, data: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_write_reg") { _ =>
      printf(p"[WB] writeReg token=${Decimal(token)} data=${Hexadecimal(data)}\n")
      writeRegPacket.kind := WB_WRITE_REG
      writeRegPacket.wbToken := token
      writeRegPacket.data := data
      writeRegPacket.nextPc := 0.U
      writeRegPacket.delta := 0.S
      SysCall.Inline(wbReqBuffer.push(writeRegPacket))
    }

    override def writeRegAndRedirect(token: UInt, data: UInt, nextPc: UInt): HwInline[Unit] =
      HwInline.atomic(s"${name}_write_reg_and_redirect") { _ =>
        printf(p"[WB] writeReg+redirect token=${Decimal(token)} data=${Hexadecimal(data)} nextPc=${Hexadecimal(nextPc)}\n")
        writeRegAndRedirectPacket.kind := WB_WRITE_REG_REDIRECT
        writeRegAndRedirectPacket.wbToken := token
        writeRegAndRedirectPacket.data := data
        writeRegAndRedirectPacket.nextPc := nextPc
        writeRegAndRedirectPacket.delta := 0.S
        SysCall.Inline(wbReqBuffer.push(writeRegAndRedirectPacket))
      }

    override def redirect(nextPc: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_redirect") { _ =>
      printf(p"[WB] redirect nextPc=${Hexadecimal(nextPc)}\n")
      redirectPacket.kind := WB_REDIRECT
      redirectPacket.wbToken := 0.U
      redirectPacket.data := 0.U
      redirectPacket.nextPc := nextPc
      redirectPacket.delta := 0.S
      SysCall.Inline(wbReqBuffer.push(redirectPacket))
    }

    override def redirectRelative(delta: SInt): HwInline[Unit] = HwInline.atomic(s"${name}_redirect_relative") { _ =>
      printf(p"[WB] redirectRelative delta=${Hexadecimal(delta.asUInt)}\n")
      redirectRelativePacket.kind := WB_REDIRECT_REL
      redirectRelativePacket.wbToken := 0.U
      redirectRelativePacket.data := 0.U
      redirectRelativePacket.nextPc := 0.U
      redirectRelativePacket.delta := delta
      SysCall.Inline(wbReqBuffer.push(redirectRelativePacket))
    }
  }

  def RequestWritebackApi(): HwInline[WritebackApiDecl] = HwInline.bindings(s"${name}_writeback_api") { _ =>
    api
  }
}
