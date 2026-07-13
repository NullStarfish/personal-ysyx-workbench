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
    val Idle, SendAddr, SendWrite, WaitReply = Value
  }
  val state = RegInit(State.Idle)

  val reqReg = Reg(new ExecutePacket(enableTraceFields))
  val addrDone = RegInit(false.B)
  val writeDone = RegInit(false.B)

  private def formatStore(data: UInt, subop: SizeSubop.Type, addr: UInt): (UInt, UInt, UInt) = {
    val addrOffset = addr(1, 0)
    val writeData = WireDefault(data)
    val writeStrb = WireDefault(0.U(4.W))
    val size = WireDefault(2.U(3.W))

    switch(subop) {
      is(SizeSubop.Byte) {
        writeData := data(7, 0) << (addrOffset << 3)
        writeStrb := "b0001".U << addrOffset
        size := 0.U
      }
      is(SizeSubop.Half) {
        writeData := data(15, 0) << (addrOffset << 3)
        writeStrb := "b0011".U << addrOffset
        size := 1.U
      }
      is(SizeSubop.Word) {
        writeData := data
        writeStrb := "b1111".U
        size := 2.U
      }
    }

    (writeData, writeStrb, size)
  }

  val (inputWriteData, inputWriteStrb, inputSize) = formatStore(
    io.in.bits.memData.data,
    io.in.bits.memCtrl.subop,
    io.in.bits.memData.addr,
  )
  val (reqWriteData, reqWriteStrb, reqSize) = formatStore(
    reqReg.memData.data,
    reqReg.memCtrl.subop,
    reqReg.memData.addr,
  )

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


  val isInputMem = io.in.bits.memCtrl.en && io.in.valid
  val isInputPassThrough = !io.in.bits.memCtrl.en && io.in.valid
  val inputLoad = isInputMem && !io.in.bits.memCtrl.write
  val inputStore = isInputMem && io.in.bits.memCtrl.write

  val reqRegIsLoad = reqReg.memCtrl.en && !reqReg.memCtrl.write
  val reqRegIsStore = reqReg.memCtrl.en && reqReg.memCtrl.write
  val notReset = !reset.asBool
  val isIdle = state === State.Idle
  val isSendAddr = state === State.SendAddr
  val isSendWrite = state === State.SendWrite
  val isWaitReply = state === State.WaitReply

  // State transition logic
  switch(state) {
    is(State.Idle) {
      when (io.in.fire && isInputMem) {
        reqReg := io.in.bits
        when (io.mem.a.fire) {
          state := Mux(inputLoad, State.WaitReply,
            Mux(io.mem.w.fire, State.WaitReply, State.SendWrite))
        }.otherwise {
          state := State.SendAddr
        }
      }
      /*
      when(io.in.fire && inputLoad) {
        reqReg := io.in.bits
        state := State.WaitReply
      }.elsewhen(io.in.fire && inputStore) {
        reqReg := io.in.bits
        addrDone := io.mem.a.fire
        writeDone := io.mem.w.fire
        state := Mux(io.mem.a.fire && io.mem.w.fire, State.WaitReply, State.SendWrite)

        when(io.mem.a.fire && io.mem.w.fire) {
          addrDone := false.B
          writeDone := false.B
        }
      }
      */
    }
    is(State.SendAddr) {
      when (io.mem.a.fire) {
          state := Mux(reqRegIsLoad, State.WaitReply,
            Mux(io.mem.w.fire, State.WaitReply, State.SendWrite))
      }
    }

    is(State.SendWrite) {
      when (io.mem.w.fire) {
        state := State.WaitReply
      }
    }

    is(State.WaitReply) {
      when(io.out.fire) {
        state := State.Idle
      }
    }
  }

  // Output logic
  io.in.ready := notReset && isIdle && Mux(
    isInputPassThrough,
    io.out.ready,
    true.B
  )

  io.mem.a.valid := notReset && (
    (isIdle && isInputMem) ||
    (isSendAddr)
  )

  io.mem.a.bits.addr := Mux(isIdle, io.in.bits.memData.addr, reqReg.memData.addr)
  io.mem.a.bits.size := Mux(isIdle, inputSize, reqSize)
  io.mem.a.bits.len := 0.U
  io.mem.a.bits.write := Mux(isIdle, inputStore, reqRegIsStore)
  io.mem.a.bits.id := 0.U

  io.mem.w.valid := notReset && (
    (isIdle && inputStore) ||
      (isSendAddr && reqRegIsStore) ||
        (isSendWrite)
  )
  io.mem.w.bits.data := Mux(isIdle, inputWriteData, reqWriteData)
  io.mem.w.bits.strb := Mux(isIdle, inputWriteStrb, reqWriteStrb)
  io.mem.w.bits.last := true.B

  io.mem.r.ready := notReset && isWaitReply && reqRegIsLoad && io.out.ready
  io.mem.b.ready := notReset && isWaitReply && reqRegIsStore && io.out.ready

  io.out.valid := notReset && (
    (isIdle && io.in.valid && isInputPassThrough) ||
      (isWaitReply && Mux(reqRegIsLoad, io.mem.r.valid, io.mem.b.valid))
  )
  io.pendingLoad.valid := notReset && (isSendAddr || isWaitReply) && reqRegIsLoad
  io.pendingLoad.addr := reqReg.wbCtrl.rd

  io.out.bits.wbData.wdata := Mux(isIdle, io.in.bits.wbData.wdata,
    Mux(reqRegIsLoad, loadData, 0.U))

  io.out.bits.wbCtrl := Mux(isIdle, io.in.bits.wbCtrl, reqReg.wbCtrl)
  io.out.bits.inst.pc := Mux(isIdle, io.in.bits.inst.pc, reqReg.inst.pc)
  io.out.bits.inst.except.no := Mux(isIdle, io.in.bits.inst.except.no, reqReg.inst.except.no)
  io.out.bits.inst.except.valid := Mux(isIdle, io.in.bits.inst.except.valid, reqReg.inst.except.valid)
  io.out.bits.sys.ebreak := Mux(isIdle, io.in.bits.sys.ebreak, reqReg.sys.ebreak)
  io.out.bits.sys.mret := Mux(isIdle, io.in.bits.sys.mret, reqReg.sys.mret)
  io.out.bits.sys.fencei := Mux(isIdle, io.in.bits.sys.fencei, reqReg.sys.fencei)
  io.out.bits.sys.csr.csrOp := Mux(isIdle, io.in.bits.sys.csr.csrOp, reqReg.sys.csr.csrOp)
  io.out.bits.sys.csr.csrAddr := Mux(isIdle, io.in.bits.sys.csr.csrAddr, reqReg.sys.csr.csrAddr)
  if (enableTraceFields) {
    io.out.bits.retireTrace.get := Mux(isIdle, io.in.bits.retireTrace.get, reqReg.retireTrace.get)
    io.out.bits.retireTrace.get.regWrite.wdata := io.out.bits.wbData.wdata
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
