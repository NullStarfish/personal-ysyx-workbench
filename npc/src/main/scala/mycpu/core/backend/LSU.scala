package mycpu.core.backend

import chisel3._
import chisel3.util._
import mycpu.common._
import mycpu.core.bundles._
import mycpu.utils._

class LSU extends Module {
  val io = IO(new Bundle {
    val in  = Flipped(Decoupled(new ExecutePacket))
    val out = Decoupled(new MemoryPacket)
    val axi = new AXI4LiteBundle(XLEN, XLEN) // 完整接口
  })

  val readBridge  = Module(new AXI4LiteReadBridge(XLEN, XLEN))
  val writeBridge = Module(new AXI4LiteWriteBridge(XLEN, XLEN))

  // ==============================================================================
  // [魔法] 自动拆分并连接
  // ==============================================================================
  val AXI4Split(rBus, wBus) = io.axi

  // 直接连接，清爽干净！
  readBridge.io.axi  <> rBus
  writeBridge.io.axi <> wBus

  // ==============================================================================
  // 核心业务逻辑
  // ==============================================================================
  object State extends ChiselEnum { val sIdle, sWaitResp = Value }
  val state = RegInit(State.sIdle)
  val reqReg = Reg(new ExecutePacket)

  // 辅助信号生成
  val inAddrOffset = io.in.bits.aluResult(1, 0)
  val inWstrb = WireDefault(0.U(4.W))
  val inWdata = WireDefault(0.U(XLEN.W))

  switch(io.in.bits.ctrl.memFunct3) {
    is(0.U) { inWstrb := "b0001".U << inAddrOffset; inWdata := io.in.bits.memWData(7,0) << (inAddrOffset << 3) } 
    is(1.U) { inWstrb := "b0011".U << inAddrOffset; inWdata := io.in.bits.memWData(15,0) << (inAddrOffset << 3) } 
    is(2.U) { inWstrb := "b1111".U;                  inWdata := io.in.bits.memWData } 
  }

  // Bridge 输入配置
  readBridge.io.req.valid       := false.B
  readBridge.io.req.bits.addr   := io.in.bits.aluResult

  writeBridge.io.req.valid      := false.B
  writeBridge.io.req.bits.addr  := io.in.bits.aluResult
  writeBridge.io.req.bits.wdata := inWdata
  writeBridge.io.req.bits.wstrb := inWstrb


  //DEBUG: Bridge请求debug信息
  when(readBridge.io.req.fire) {
    Debug.log("LSU : read req: addr: %x\n",readBridge.io.req.bits.addr)
  }

  when(writeBridge.io.req.fire) {
    Debug.log("LSU : write req: addr: %x, data: %x, wstrb: %x\n", writeBridge.io.req.bits.addr, writeBridge.io.req.bits.wdata, writeBridge.io.req.bits.wstrb)
  }




  // 握手判断
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

  // 读数据后处理
  val rawReadData = readBridge.io.resp.bits.rdata
  val addrOffset  = reqReg.aluResult(1, 0)
  val shiftAmount = addrOffset << 3 
  val shiftedData = rawReadData >> shiftAmount
  val finalLoadData = Wire(UInt(32.W))



  when(readBridge.io.resp.fire) {
    Debug.log("[DEBUG] [LSU] Read Resp: rdata: %x, isError: %x\n", readBridge.io.resp.bits.rdata, readBridge.io.resp.bits.isError)
  }
  when(writeBridge.io.resp.fire) {
    Debug.log("[DEBUG] [LSU] Write Resp: isError: %x\n", writeBridge.io.resp.bits.isError)
  }


  
  finalLoadData := 0.U 
  switch(reqReg.ctrl.memFunct3) {
    is(0.U) { finalLoadData := Cat(Fill(24, shiftedData(7)), shiftedData(7,0)) }
    is(1.U) { finalLoadData := Cat(Fill(16, shiftedData(15)), shiftedData(15,0)) }
    is(2.U) { finalLoadData := rawReadData }
    is(4.U) { finalLoadData := shiftedData(7,0) }
    is(5.U) { finalLoadData := shiftedData(15,0) }
  }

  // 输出汇聚
  readBridge.io.resp.ready  := false.B
  writeBridge.io.resp.ready := false.B

  io.out.valid := false.B
  io.out.bits.connectDebug(reqReg)
  io.out.bits.rdAddr   := reqReg.rdAddr
  io.out.bits.regWen   := reqReg.ctrl.regWen
  io.out.bits.pcTarget := reqReg.pcTarget
  io.out.bits.wbData   := Mux(reqReg.ctrl.memEn && !reqReg.ctrl.memWen, finalLoadData, reqReg.aluResult)

  when (state === State.sWaitResp) {
    val isMemReadOp  = reqReg.ctrl.memEn && !reqReg.ctrl.memWen
    val isMemWriteOp = reqReg.ctrl.memEn && reqReg.ctrl.memWen
    
    when (isMemReadOp) {
      io.out.valid := readBridge.io.resp.valid
      readBridge.io.resp.ready := io.out.ready
    } .elsewhen (isMemWriteOp) {
      io.out.valid := writeBridge.io.resp.valid
      writeBridge.io.resp.ready := io.out.ready
    } .otherwise {
      io.out.valid := true.B
    }
  }
}