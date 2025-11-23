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

  // 默认值
  io.in.ready := (state === State.sIdle)
  io.out.valid := false.B
  io.out.bits  := DontCare
  
  // AXI 默认
  io.axi.ar.valid := false.B; io.axi.ar.bits.addr := reqReg.aluResult; io.axi.ar.bits.prot := 0.U
  io.axi.aw.valid := false.B; io.axi.aw.bits.addr := reqReg.aluResult; io.axi.aw.bits.prot := 0.U
  io.axi.w.valid  := false.B; io.axi.w.bits.data  := DontCare;         io.axi.w.bits.strb := 0.U
  io.axi.r.ready  := false.B; io.axi.b.ready      := false.B

  // --- Store 逻辑 (保持不变) ---
  val addrOffset = reqReg.aluResult(1, 0)
  val wstrb = WireDefault(0.U(4.W))
  val wdata = WireDefault(0.U(XLEN.W))
  switch(reqReg.ctrl.memFunct3) {
    is(0.U) { wstrb := "b0001".U << addrOffset; wdata := reqReg.memWData(7,0) << (addrOffset << 3) } // SB
    is(1.U) { wstrb := "b0011".U << addrOffset; wdata := reqReg.memWData(15,0) << (addrOffset << 3) } // SH
    is(2.U) { wstrb := "b1111".U; wdata := reqReg.memWData } // SW
  }

  // --- Load 逻辑 (修复对齐) ---
  // DPI 返回的是 4字节对齐的 Word，我们需要根据地址偏移提取字节
  val rawReadData = readDataReg
  val shiftAmount = addrOffset << 3 // * 8
  val shiftedData = rawReadData >> shiftAmount
  val finalLoadData = Wire(UInt(32.W))
  
  finalLoadData := 0.U // 默认
  switch(reqReg.ctrl.memFunct3) {
    is(0.U) { finalLoadData := Cat(Fill(24, shiftedData(7)), shiftedData(7,0)) } // LB
    is(1.U) { finalLoadData := Cat(Fill(16, shiftedData(15)), shiftedData(15,0)) } // LH
    is(2.U) { finalLoadData := rawReadData } // LW
    is(4.U) { finalLoadData := shiftedData(7,0) } // LBU
    is(5.U) { finalLoadData := shiftedData(15,0) } // LHU
  }

  // --- 状态机 ---
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
      // 关键：DPI 读取需要对齐地址，但 AXI 协议通常传原始地址，Slave 处理对齐。
      // 既然你的 SRAM.sv 用的是 AXI，我们传原始地址即可。
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
      io.out.bits.wbData := Mux(reqReg.ctrl.memEn && !reqReg.ctrl.memWen, 
                                finalLoadData, 
                                reqReg.aluResult)
      io.out.bits.rdAddr := reqReg.rdAddr
      io.out.bits.regWen := reqReg.ctrl.regWen
      when (io.out.ready) { state := State.sIdle }
    }
  }
}