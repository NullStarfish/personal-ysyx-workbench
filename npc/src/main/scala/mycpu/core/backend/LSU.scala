package mycpu.core.backend

import chisel3._
import chisel3.util._
import mycpu.common._
import mycpu.core.bundles._

class LSU(
    enableTraceFields: Boolean = ENABLE_TRACE_FIELDS,
    enableDpi: Boolean = false,
) extends Module {
  val io = IO(new Bundle {
    val in = Flipped(Decoupled(new ExecutePacket(enableTraceFields)))
    val req = Decoupled(new LsuReq)
    val reply = Flipped(Decoupled(UInt(XLEN.W)))
    val out = Decoupled(new MemoryPacket)
    val pendingLoad = Output(new RAWRdPacket)
  })

  object State extends ChiselEnum {
    val Idle, WaitReply = Value
  }

  val state = RegInit(State.Idle)
  val reqReg = Reg(new ExecutePacket(enableTraceFields))

  val reqView = Wire(new ExecutePacket(enableTraceFields))
  reqView := reqReg
  when(state === State.Idle) {
    reqView := io.in.bits
  }

  val addr = reqView.memData.addr
  val addrOffset = addr(1, 0)
  val writeData = WireDefault(reqView.memData.data)
  val writeStrb = WireDefault(0.U(4.W))
  val reqSize = WireDefault(2.U(3.W))

  switch(reqView.memCtrl.subop) {
    is(SizeSubop.Byte) {
      writeData := reqView.memData.data(7, 0) << (addrOffset << 3)
      writeStrb := "b0001".U << addrOffset
      reqSize := 0.U
    }
    is(SizeSubop.Half) {
      writeData := reqView.memData.data(15, 0) << (addrOffset << 3)
      writeStrb := "b0011".U << addrOffset
      reqSize := 1.U
    }
    is(SizeSubop.Word) {
      writeData := reqView.memData.data
      writeStrb := "b1111".U
      reqSize := 2.U
    }
  }

  val shiftedReadData = io.reply.bits >> (reqReg.memData.addr(1, 0) << 3)
  val loadData = WireDefault(io.reply.bits)
  switch(reqReg.memCtrl.subop) {
    is(SizeSubop.Byte) {
      loadData := Mux(reqReg.memCtrl.unsigned, shiftedReadData(7, 0), Cat(Fill(24, shiftedReadData(7)), shiftedReadData(7, 0)))
    }
    is(SizeSubop.Half) {
      loadData := Mux(reqReg.memCtrl.unsigned, shiftedReadData(15, 0), Cat(Fill(16, shiftedReadData(15)), shiftedReadData(15, 0)))
    }
    is(SizeSubop.Word) {
      loadData := io.reply.bits
    }
  }

  val isInputMem = io.in.bits.memCtrl.en
  val isInputPassThrough = !isInputMem
  val reqViewIsMem = reqView.memCtrl.en
  val reqViewIsLoad = reqView.memCtrl.en && !reqView.memCtrl.write
  val reqRegIsLoad = reqReg.memCtrl.en && !reqReg.memCtrl.write
  val notReset = !reset.asBool

  io.in.ready := false.B
  io.req.valid := false.B
  io.req.bits.addr := addr
  io.req.bits.data := writeData
  io.req.bits.strb := writeStrb
  io.req.bits.write := reqView.memCtrl.write
  io.req.bits.size := reqSize
  io.reply.ready := false.B
  io.out.valid := false.B
  io.pendingLoad.valid := notReset && state === State.WaitReply && reqRegIsLoad
  io.pendingLoad.addr := reqReg.wbCtrl.rd

  io.out.bits.wbData.wdata := Mux(reqViewIsLoad, loadData, reqView.wbData.wdata)
  io.out.bits.wbCtrl := reqView.wbCtrl
  if (enableTraceFields) {
    io.out.bits.retireTrace.get := reqView.retireTrace.get
    io.out.bits.retireTrace.get.regWrite.wdata := io.out.bits.wbData.wdata
  }

  switch(state) {
    is(State.Idle) {
      io.in.ready := notReset && Mux(isInputPassThrough, io.out.ready, io.req.ready)
      io.req.valid := notReset && io.in.valid && isInputMem
      io.out.valid := notReset && io.in.valid && isInputPassThrough

      when(io.in.fire && isInputMem) {
        reqReg := io.in.bits
        state := State.WaitReply
      }
    }

    is(State.WaitReply) {
      io.out.valid := notReset && io.reply.valid
      io.reply.ready := notReset && io.out.ready
      when(io.out.fire) {
        state := State.Idle
      }
    }
  }

  if (enableDpi) {
    val lsuTrace = Module(new LSUTrace)
    lsuTrace.io.clk := clock
    lsuTrace.io.reset := reset.asBool
    lsuTrace.io.reqReadData := io.req.fire && !reqView.memCtrl.write
    lsuTrace.io.reqWriteData := io.req.fire && reqView.memCtrl.write
    lsuTrace.io.gotData := io.reply.fire && reqReg.memCtrl.en
    lsuTrace.io.blocked := io.in.valid && !io.in.ready

    val dcacheTrace = Module(new DCacheTrace)
    dcacheTrace.io.clk := clock
    dcacheTrace.io.reset := reset.asBool
    dcacheTrace.io.hit := false.B
    dcacheTrace.io.miss := io.req.fire
  }
}
