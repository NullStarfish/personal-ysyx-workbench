package mycpu.core.backend

import chisel3._
import chisel3.util._
import mycpu.common._
import mycpu.core.bundles._

class LSU(enableTraceFields: Boolean = ENABLE_TRACE_FIELDS) extends Module {
  val io = IO(new Bundle {
    val in = Flipped(Decoupled(new ExecutePacket(enableTraceFields)))
    val req = Decoupled(new LsuReq)
    val reply = Flipped(Decoupled(UInt(XLEN.W)))
    val out = Decoupled(new MemoryPacket)
  })

  object State extends ChiselEnum {
    val Idle, WaitReply, EmitPassThrough = Value
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
  val byteLane = Mux(addrOffset === 3.U, 2.U, addrOffset)
  val writeData = WireDefault(reqView.memData.data)
  val writeStrb = WireDefault(0.U(4.W))
  val reqSize = WireDefault(0.U(2.W))

  switch(reqView.memCtrl.subop) {
    is(SizeSubop.Byte) {
      writeData := reqView.memData.data(7, 0) << (byteLane << 3)
      writeStrb := "b0001".U << byteLane
      reqSize := 2.U
    }
    is(SizeSubop.Half) {
      writeData := reqView.memData.data(15, 0) << (addrOffset << 3)
      writeStrb := "b0011".U << addrOffset
      reqSize := 1.U
    }
    is(SizeSubop.Word) {
      writeData := reqView.memData.data
      writeStrb := "b1111".U
      reqSize := 0.U
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

  io.in.ready := false.B
  io.req.valid := false.B
  io.req.bits.addr := addr
  io.req.bits.data := writeData
  io.req.bits.strb := writeStrb
  io.req.bits.write := reqView.memCtrl.write
  io.req.bits.size := reqSize
  io.reply.ready := false.B
  io.out.valid := false.B

  io.out.bits.wbData.wdata := Mux(reqReg.memCtrl.en && !reqReg.memCtrl.write, loadData, reqReg.wbData.wdata)
  io.out.bits.wbCtrl := reqReg.wbCtrl
  if (enableTraceFields) {
    io.out.bits.retireTrace.get := reqReg.retireTrace.get
    io.out.bits.retireTrace.get.regWrite.wdata := io.out.bits.wbData.wdata
  }

  switch(state) {
    is(State.Idle) {
      io.in.ready := isInputPassThrough || io.req.ready
      io.req.valid := io.in.valid && isInputMem

      when(io.in.fire) {
        reqReg := io.in.bits
        when(isInputPassThrough) {
          state := State.EmitPassThrough
        }.otherwise {
          state := State.WaitReply
        }
      }
    }

    is(State.WaitReply) {
      io.out.valid := io.reply.valid
      io.reply.ready := io.out.ready
      when(io.out.fire) {
        state := State.Idle
      }
    }

    is(State.EmitPassThrough) {
      io.out.valid := true.B
      when(io.out.fire) {
        state := State.Idle
      }
    }
  }
}
