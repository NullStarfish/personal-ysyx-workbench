package mycpu.core.backend

import chisel3._
import chisel3.util._
import mycpu.common._
import mycpu.core.bundles._
import mycpu.memory._

class LSU(
    enableTraceFields: Boolean = ENABLE_TRACE_FIELDS,
    enableDpi: Boolean = false,
) extends Module {
  val io = IO(new Bundle {
    val in = Flipped(Decoupled(new ExecutePacket(enableTraceFields)))
    val mem = new MemIO
    val out = Decoupled(new MemoryPacket)
    val pendingLoad = Output(new RAWRdPacket)
  })

  object State extends ChiselEnum {
    val Idle, SendWrite, WaitReply = Value
  }

  val state = RegInit(State.Idle)
  val reqReg = Reg(new ExecutePacket(enableTraceFields))
  val addrDone = RegInit(false.B)
  val writeDone = RegInit(false.B)

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

  val shiftedReadData = io.mem.r.bits.data >> (reqReg.memData.addr(1, 0) << 3)
  val loadData = WireDefault(io.mem.r.bits.data)
  switch(reqReg.memCtrl.subop) {
    is(SizeSubop.Byte) {
      loadData := Mux(reqReg.memCtrl.unsigned, shiftedReadData(7, 0), Cat(Fill(24, shiftedReadData(7)), shiftedReadData(7, 0)))
    }
    is(SizeSubop.Half) {
      loadData := Mux(reqReg.memCtrl.unsigned, shiftedReadData(15, 0), Cat(Fill(16, shiftedReadData(15)), shiftedReadData(15, 0)))
    }
    is(SizeSubop.Word) {
      loadData := io.mem.r.bits.data
    }
  }

  val isInputMem = io.in.bits.memCtrl.en
  val isInputPassThrough = !isInputMem
  val inputLoad = isInputMem && !io.in.bits.memCtrl.write
  val inputStore = isInputMem && io.in.bits.memCtrl.write
  val reqViewIsLoad = reqView.memCtrl.en && !reqView.memCtrl.write
  val reqRegIsLoad = reqReg.memCtrl.en && !reqReg.memCtrl.write
  val notReset = !reset.asBool

  io.in.ready := false.B
  io.mem.a.valid := false.B
  io.mem.a.bits.addr := addr
  io.mem.a.bits.size := reqSize
  io.mem.a.bits.len := 0.U
  io.mem.a.bits.write := reqView.memCtrl.write
  io.mem.a.bits.id := 0.U
  io.mem.w.valid := false.B
  io.mem.w.bits.data := writeData
  io.mem.w.bits.strb := writeStrb
  io.mem.w.bits.last := true.B
  io.mem.r.ready := false.B
  io.mem.b.ready := false.B
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
      io.mem.a.valid := notReset && io.in.valid && isInputMem
      io.mem.w.valid := notReset && io.in.valid && inputStore
      io.in.ready := notReset && Mux(
        isInputPassThrough,
        io.out.ready,
        Mux(inputLoad, io.mem.a.ready, io.mem.a.ready || io.mem.w.ready),
      )
      io.out.valid := notReset && io.in.valid && isInputPassThrough

      when(io.in.fire && inputLoad) {
        reqReg := io.in.bits
        state := State.WaitReply
      }.elsewhen(io.in.fire && inputStore) {
        reqReg := io.in.bits
        addrDone := io.mem.a.fire
        writeDone := io.mem.w.fire
        when(io.mem.a.fire && io.mem.w.fire) {
          addrDone := false.B
          writeDone := false.B
          state := State.WaitReply
        }.otherwise {
          state := State.SendWrite
        }
      }
    }

    is(State.SendWrite) {
      io.mem.a.valid := notReset && !addrDone
      io.mem.w.valid := notReset && !writeDone
      when(io.mem.a.fire) {
        addrDone := true.B
      }
      when(io.mem.w.fire) {
        writeDone := true.B
      }
      when((addrDone || io.mem.a.fire) && (writeDone || io.mem.w.fire)) {
        addrDone := false.B
        writeDone := false.B
        state := State.WaitReply
      }
    }

    is(State.WaitReply) {
      io.out.valid := notReset && Mux(reqRegIsLoad, io.mem.r.valid, io.mem.b.valid)
      io.mem.r.ready := notReset && reqRegIsLoad && io.out.ready
      io.mem.b.ready := notReset && !reqRegIsLoad && io.out.ready
      when(io.out.fire) {
        state := State.Idle
      }
    }
  }

  val enableTrace = false
  if (enableTrace) {
    when(io.mem.a.fire) {
      printf("[LSU] req addr: %x\n", io.mem.a.bits.addr)
      when(io.mem.a.bits.write) {
        printf("[LSU] req write data: %x\n", io.mem.w.bits.data)
      }.otherwise {
        printf("[LSU] req read\n")
      }
    }

    when(io.mem.r.fire || io.mem.b.fire) {
      printf("[LSU] reply\n")
      printf("[LSU] reply data: %x\n", io.mem.r.bits.data)
    }
  }

  if (enableDpi) {
    val lsuTrace = Module(new LSUTrace)
    lsuTrace.io.clk := clock
    lsuTrace.io.reset := reset.asBool
    lsuTrace.io.reqReadData := io.mem.a.fire && !io.mem.a.bits.write
    lsuTrace.io.reqWriteData := io.mem.w.fire
    lsuTrace.io.gotData := (io.mem.r.fire || io.mem.b.fire) && reqReg.memCtrl.en
    lsuTrace.io.blocked := io.in.valid && !io.in.ready
  }
}
