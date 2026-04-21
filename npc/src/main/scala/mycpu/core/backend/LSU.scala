package mycpu.core.backend

import chisel3._
import chisel3.util._
import mycpu.common._
import mycpu.core.bundles._
import mycpu.MemMap
import mycpu.utils._

class LSU(enableTraceFields: Boolean = ENABLE_TRACE_FIELDS) extends Module {
  val io = IO(new Bundle {
    val in = Flipped(Decoupled(new ExecutePacket(enableTraceFields)))
    val out = Decoupled(new MemoryPacket(enableTraceFields))
    val axi = new AXI4LiteBundle(XLEN, XLEN)
    val status = Output(new LsuStatusBundle)
  })

  val readBridge = Module(new AXI4ReadBridge(XLEN, XLEN))
  val writeBridge = Module(new AXI4WriteBridge(XLEN, XLEN))

  val AXI4Split(rBus, wBus) = io.axi
  readBridge.io.axi <> rBus
  writeBridge.io.axi <> wBus

  object State extends ChiselEnum {
    val Idle, WaitReadResp, WaitWriteResp, EmitPassThrough = Value
  }
  val state = RegInit(State.Idle)
  val reqReg = Reg(new ExecutePacket(enableTraceFields))

  val reqView = Wire(new ExecutePacket(enableTraceFields))
  reqView := reqReg
  when(state === State.Idle) {
    reqView := io.in.bits
  }

  val addr = reqView.result
  val addrOffset = addr(1, 0)
  val writeStrb = WireDefault(0.U(4.W))
  val writeData = WireDefault(0.U(XLEN.W))
  val accessSize = WireDefault(2.U(3.W))

  switch(reqView.mem.subop) {
    is(ExecSubop.Byte) {
      writeStrb := "b0001".U << addrOffset
      writeData := reqView.rhs(7, 0) << (addrOffset << 3)
      accessSize := 0.U
    }
    is(ExecSubop.Half) {
      writeStrb := "b0011".U << addrOffset
      writeData := reqView.rhs(15, 0) << (addrOffset << 3)
      accessSize := 1.U
    }
    is(ExecSubop.Word) {
      writeStrb := "b1111".U
      writeData := reqView.rhs
      accessSize := 2.U
    }
  }

  val memReq = Wire(new AXI4BundleA(AXI_ID_WIDTH, XLEN))
  memReq.id := 1.U
  memReq.addr := addr
  memReq.len := 0.U
  memReq.size := accessSize
  memReq.burst := AXI4Parameters.BURST_FIXED
  memReq.lock := false.B
  memReq.cache := 0.U
  memReq.prot := 0.U
  memReq.qos := 0.U

  val storePack = Wire(new AXI4BundleW(XLEN))
  storePack.data := writeData
  storePack.strb := writeStrb
  storePack.last := true.B

  val shiftedReadData = readBridge.io.rStream.bits.data >> (addrOffset << 3)
  val loadData = WireDefault(readBridge.io.rStream.bits.data)
  switch(reqReg.mem.subop) {
    is(ExecSubop.Byte) {
      loadData := Mux(reqReg.mem.unsigned, shiftedReadData(7, 0), Cat(Fill(24, shiftedReadData(7)), shiftedReadData(7, 0)))
    }
    is(ExecSubop.Half) {
      loadData := Mux(reqReg.mem.unsigned, shiftedReadData(15, 0), Cat(Fill(16, shiftedReadData(15)), shiftedReadData(15, 0)))
    }
    is(ExecSubop.Word) {
      loadData := readBridge.io.rStream.bits.data
    }
  }

  val isInputMem = io.in.bits.mem.valid
  val isInputLoad = isInputMem && !io.in.bits.mem.write
  val isInputStore = isInputMem && io.in.bits.mem.write
  val isInputPassThrough = !isInputMem

  io.in.ready := false.B
  io.out.valid := false.B
  io.out.bits.wbData := Mux(reqReg.mem.valid && !reqReg.mem.write, loadData, reqReg.result)
  io.out.bits.wb := reqReg.wb
  if (enableTraceFields) {
    io.out.bits.trace.get.pc := reqView.trace.get.pc
    io.out.bits.trace.get.inst := reqView.trace.get.inst
    io.out.bits.trace.get.dnpc := reqView.trace.get.dnpc
    io.out.bits.trace.get.ifValid := reqView.trace.get.ifValid
    io.out.bits.trace.get.idValid := reqView.trace.get.idValid
    io.out.bits.trace.get.exValid := reqView.trace.get.exValid
    io.out.bits.trace.get.memValid := true.B
    io.out.bits.trace.get.branchResolved := reqView.trace.get.branchResolved
    io.out.bits.trace.get.branchCorrect := reqView.trace.get.branchCorrect
    io.out.bits.trace.get.redirectValid := reqView.trace.get.redirectValid
    io.out.bits.trace.get.redirectTarget := reqView.trace.get.redirectTarget
    io.out.bits.trace.get.actualTaken := reqView.trace.get.actualTaken
    io.out.bits.trace.get.predictedTaken := reqView.trace.get.predictedTaken
  }

  readBridge.io.rReq.valid := false.B
  readBridge.io.rReq.bits := memReq
  readBridge.io.rStream.ready := false.B
  writeBridge.io.wReq.valid := false.B
  writeBridge.io.wReq.bits := memReq
  writeBridge.io.wStream.valid := false.B
  writeBridge.io.wStream.bits := storePack
  writeBridge.io.bResp.ready := false.B

  io.status.pendingLoad := (state === State.WaitReadResp) && reqReg.mem.valid && !reqReg.mem.write
  io.status.pendingRd := reqReg.wb.rd

  switch(state) {
    is(State.Idle) {
      val acceptPassThrough = isInputPassThrough
      val acceptLoad = isInputLoad && readBridge.io.rReq.ready
      val acceptStore = isInputStore && writeBridge.io.wReq.ready && writeBridge.io.wStream.ready

      io.in.ready := acceptPassThrough || acceptLoad || acceptStore

      when(io.in.fire) {
        reqReg := io.in.bits
        when(isInputPassThrough) {
          state := State.EmitPassThrough
        }.elsewhen(isInputLoad) {
          state := State.WaitReadResp
        }.otherwise {
          state := State.WaitWriteResp
        }
      }

      readBridge.io.rReq.valid := io.in.valid && isInputLoad
      writeBridge.io.wReq.valid := io.in.valid && isInputStore
      writeBridge.io.wStream.valid := io.in.valid && isInputStore
    }

    is(State.WaitReadResp) {
      io.out.valid := readBridge.io.rStream.valid
      readBridge.io.rStream.ready := io.out.ready
      when(io.out.fire) {
        state := State.Idle
      }
    }

    is(State.WaitWriteResp) {
      io.out.valid := writeBridge.io.bResp.valid
      writeBridge.io.bResp.ready := io.out.ready
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
