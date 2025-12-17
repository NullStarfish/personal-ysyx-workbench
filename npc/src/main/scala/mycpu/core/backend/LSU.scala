package mycpu.core.backend

import chisel3._
import chisel3.util._
import mycpu.common._
import mycpu.core.bundles._
import mycpu.utils._
import os.read

class LSU extends Module {
  val io = IO(new Bundle {
    val in  = Flipped(Decoupled(new ExecutePacket))
    val out = Decoupled(new MemoryPacket)
    val axi = new AXI4LiteBundle(XLEN, XLEN) // 统一接口
  })

  val readBridge  = Module(new AXI4LiteReadBridge(XLEN, XLEN))
  val writeBridge = Module(new AXI4LiteWriteBridge(XLEN, XLEN))

  // [魔法拆分] 自动生成 rBus 和 wBus (类型为 AXI4ReadOnly / AXI4WriteOnly)
  val AXI4Split(rBus, wBus) = io.axi

  // 一一对应连接
  readBridge.io.axi  <> rBus
  writeBridge.io.axi <> wBus

  // ... 剩余 LSU 逻辑保持不变 ...
  object State extends ChiselEnum { val sIdle, sWaitResp = Value }
  val state = RegInit(State.sIdle)
  val reqReg = Reg(new ExecutePacket)
  
  // (辅助逻辑略: wstrb/wdata生成)
  //temp，移到core外面

  
  val inAddrOffset = io.in.bits.aluResult(1, 0)
  val inWstrb = WireDefault(0.U(4.W))
  val inWdata = WireDefault(0.U(XLEN.W))
  // ... switch case for wstrb ...

  switch(io.in.bits.ctrl.memFunct3) {
    is(0.U) { inWstrb := "b0001".U << inAddrOffset; inWdata := io.in.bits.memWData(7,0) << (inAddrOffset << 3) } 
    is(1.U) { inWstrb := "b0011".U << inAddrOffset; inWdata := io.in.bits.memWData(15,0) << (inAddrOffset << 3) } 
    is(2.U) { inWstrb := "b1111".U;                  inWdata := io.in.bits.memWData } 
  }
  when(writeBridge.io.req.valid) {
    Debug.log("[DEBUG] [LSU] [[[[[[[[[[[CURRENT]]]]]]]]]]]] write addr sent: %x, inWdata: %x, inWstrb: %x. pc: %x, original data: %x\n", io.in.bits.aluResult, inWdata, inWstrb, io.in.bits.pc, io.in.bits.memWData)
  }


  // Bridge Req 连接
  readBridge.io.req.valid       := false.B
  readBridge.io.req.bits.addr   := io.in.bits.aluResult
  //readBridge.io.req.bits.addr   := Cat(io.in.bits.aluResult(XLEN - 1, 2), "b00".U(2.W))

  writeBridge.io.req.valid      := false.B
  //writeBridge.io.req.bits.addr  := Cat(io.in.bits.aluResult(XLEN -1, 2), "b00".U(2.W))
  writeBridge.io.req.bits.addr  := io.in.bits.aluResult
  writeBridge.io.req.bits.wdata := inWdata
  writeBridge.io.req.bits.wstrb := inWstrb

  // 握手逻辑
  val isMemRead  = io.in.bits.ctrl.memEn && !io.in.bits.ctrl.memWen
  val isMemWrite = io.in.bits.ctrl.memEn && io.in.bits.ctrl.memWen
  val isNonMem   = !io.in.bits.ctrl.memEn

  io.in.ready := (state === State.sIdle) && (
    (isNonMem) || 
    (isMemRead && readBridge.io.req.ready) || 
    (isMemWrite && writeBridge.io.req.ready)
  )

  switch(state) {
    is(State.sIdle) {
      when (io.in.valid) {
        when(io.in.ready) {
           reqReg := io.in.bits
           state  := State.sWaitResp
        }
        when (isMemRead) { readBridge.io.req.valid := true.B }
        .elsewhen (isMemWrite) { writeBridge.io.req.valid := true.B }
      }
    }
    is(State.sWaitResp) {
      when (io.out.fire) { state := State.sIdle }
    }
  }
  
  // 输出处理 (读取数据对齐等)
  val rawReadData = readBridge.io.resp.bits.rdata
  val finalLoadData = Wire(UInt(32.W))
  val shiftedData = rawReadData >> (reqReg.aluResult(1, 0) << 3)
  finalLoadData := 0.U
  // ... switch case for load data ...

  when(readBridge.io.resp.valid) {
    Debug.log("[DEBUG] [LSU] [[[[[[[[[[[CURRENT]]]]]]]]]]]] read addr sent: %x, rdata: %x, read processed: %x. pc: %x,", io.in.bits.aluResult, readBridge.io.resp.bits.rdata, finalLoadData, io.in.bits.pc)
  }



  switch(reqReg.ctrl.memFunct3) {
    is(0.U) { finalLoadData := Cat(Fill(24, shiftedData(7)), shiftedData(7,0)) }
    is(1.U) { finalLoadData := Cat(Fill(16, shiftedData(15)), shiftedData(15,0)) }
    is(2.U) { finalLoadData := rawReadData }
    is(4.U) { finalLoadData := shiftedData(7,0) }
    is(5.U) { finalLoadData := shiftedData(15,0) }
  }

  readBridge.io.resp.ready  := false.B
  writeBridge.io.resp.ready := false.B

  io.out.valid := false.B
  io.out.bits.connectDebug(reqReg)
  io.out.bits.rdAddr   := reqReg.rdAddr
  io.out.bits.regWen   := reqReg.ctrl.regWen
  io.out.bits.pcTarget := reqReg.pcTarget
  io.out.bits.wbData   := Mux(reqReg.ctrl.memEn && !reqReg.ctrl.memWen, finalLoadData, reqReg.aluResult)

  when (state === State.sWaitResp) {
    when (reqReg.ctrl.memEn && !reqReg.ctrl.memWen) {
      io.out.valid := readBridge.io.resp.valid
      readBridge.io.resp.ready := io.out.ready
    } .elsewhen (reqReg.ctrl.memEn && reqReg.ctrl.memWen) {
      io.out.valid := writeBridge.io.resp.valid
      writeBridge.io.resp.ready := io.out.ready
    } .otherwise {
      io.out.valid := true.B
    }
  }
}