package mycpu.core.backend

import chisel3._
import chisel3.util._
import mycpu.common._
import mycpu.core.bundles._
import mycpu.memory.AXI4LiteMasterIO

class LSU extends Module {
  val io = IO(new Bundle {
    val in  = Flipped(Decoupled(new ExecutePacket))
    val out = Decoupled(new MemoryPacket)
    val axi = new AXI4LiteMasterIO()
  })

  object State extends ChiselEnum {
    val sIdle, sReadAddr, sReadData, sWriteAddr, sWriteData, sWriteResp, sWaitWB = Value
  }
  val state = RegInit(State.sIdle)
  val reqReg = Reg(new ExecutePacket)
  val readDataReg = Reg(UInt(32.W))

  io.in.ready := (state === State.sIdle)
  io.out.valid := false.B
  io.out.bits  := DontCare
  
  // AXI Defaults
  io.axi.ar.valid := false.B; io.axi.ar.bits.addr := reqReg.aluResult; io.axi.ar.bits.prot := 0.U
  io.axi.aw.valid := false.B; io.axi.aw.bits.addr := reqReg.aluResult; io.axi.aw.bits.prot := 0.U
  io.axi.w.valid  := false.B; io.axi.w.bits.data  := DontCare;         io.axi.w.bits.strb := 0.U
  io.axi.r.ready  := false.B; io.axi.b.ready      := false.B

  val addrOffset = reqReg.aluResult(1, 0)
  val wstrb = WireDefault(0.U(4.W))
  val wdata = WireDefault(0.U(XLEN.W))
  switch(reqReg.ctrl.memFunct3) {
    is(0.U) { wstrb := "b0001".U << addrOffset; wdata := reqReg.memWData(7,0) << (addrOffset << 3) } 
    is(1.U) { wstrb := "b0011".U << addrOffset; wdata := reqReg.memWData(15,0) << (addrOffset << 3) } 
    is(2.U) { wstrb := "b1111".U; wdata := reqReg.memWData } 
  }

  val rawReadData = readDataReg
  val shiftAmount = addrOffset << 3 
  val shiftedData = rawReadData >> shiftAmount
  val finalLoadData = Wire(UInt(32.W))
  finalLoadData := 0.U 
  switch(reqReg.ctrl.memFunct3) {
    is(0.U) { finalLoadData := Cat(Fill(24, shiftedData(7)), shiftedData(7,0)) } 
    is(1.U) { finalLoadData := Cat(Fill(16, shiftedData(15)), shiftedData(15,0)) } 
    is(2.U) { finalLoadData := rawReadData } 
    is(4.U) { finalLoadData := shiftedData(7,0) } 
    is(5.U) { finalLoadData := shiftedData(15,0) } 
  }

  switch (state) {
    is (State.sIdle) {
      when (io.in.valid) {
        reqReg := io.in.bits
        when (io.in.bits.ctrl.memEn) {
           when (io.in.bits.ctrl.memWen) { state := State.sWriteAddr }
           .otherwise { state := State.sReadAddr }
        } .otherwise { state := State.sWaitWB }
      }
    }
    is (State.sReadAddr) {
      io.axi.ar.valid := true.B
      when (io.axi.ar.fire) { state := State.sReadData }
    }
    is (State.sReadData) {
      io.axi.r.ready := true.B
      when (io.axi.r.fire) {
        readDataReg := io.axi.r.bits.data 
        state := State.sWaitWB
      }
    }
    is (State.sWriteAddr) {
      io.axi.aw.valid := true.B
      when (io.axi.aw.fire) { state := State.sWriteData }
    }
    is (State.sWriteData) {
      io.axi.w.valid := true.B
      io.axi.w.bits.data := wdata
      io.axi.w.bits.strb := wstrb
      when (io.axi.w.fire) { state := State.sWriteResp }
    }
    is (State.sWriteResp) {
      io.axi.b.ready := true.B
      when (io.axi.b.fire) { state := State.sWaitWB }
    }
    is (State.sWaitWB) {
      io.out.valid := true.B
      
      // [修改] 输出时，从 reqReg 恢复调试信息
      io.out.bits.connectDebug(reqReg)

      io.out.bits.wbData := Mux(reqReg.ctrl.memEn && !reqReg.ctrl.memWen, 
                                finalLoadData, 
                                reqReg.aluResult)
      io.out.bits.rdAddr := reqReg.rdAddr
      io.out.bits.regWen := reqReg.ctrl.regWen
      when (io.out.ready) { state := State.sIdle }
    }
  }
}