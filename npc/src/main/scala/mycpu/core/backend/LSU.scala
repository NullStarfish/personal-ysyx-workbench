package mycpu.core.backend

import chisel3._
import chisel3.util._
import mycpu.common._
import mycpu.core.bundles._
import mycpu.utils._
import os.read
import os.write

class LSU extends Module {
  val io = IO(new Bundle {
    val in  = Flipped(Decoupled(new ExecutePacket))
    val out = Decoupled(new MemoryPacket)
    val axi = new AXI4LiteBundle(XLEN, XLEN) // 统一接口
  })

//=======================================================
//1. 处理输入，输出的数据
//=======================================================

  val reqReg = Reg(new ExecutePacket)




  val AXI4Split(rBus, wBus) = io.axi
  val lsuThread = new HardwareThread("LSU_Core", debugEnable = true)
  
  // 接管 AXI 关键信号
  val arValid = lsuThread.driveManaged(rBus.ar.valid, false.B)
  val rReady  = lsuThread.driveManaged(rBus.r.ready,  false.B)
  val awValid = lsuThread.driveManaged(wBus.aw.valid, false.B)
  val wValid  = lsuThread.driveManaged(wBus.w.valid,  false.B)
  val bReady  = lsuThread.driveManaged(wBus.b.ready,  false.B)

  val arBits  = lsuThread.driveManaged(rBus.ar.bits, DontCare) //USE DontCare to improve performance 
  val awBits = lsuThread.driveManaged(wBus.aw.bits, DontCare)
  val wBits = lsuThread.driveManaged(wBus.w.bits, DontCare)

  lsuThread.startWhen(io.in.valid)
  
  lsuThread.entry {
    val memData = Reg(UInt(32.W))
    val isRead  = io.in.bits.ctrl.memEn && !io.in.bits.ctrl.memWen
    val isWrite = io.in.bits.ctrl.memEn && io.in.bits.ctrl.memWen

    // 捕捉请求
    lsuThread.Step { reqReg := io.in.bits }

    // 处理 AXI 事务
    lsuThread.Step {
      when (isRead) {
        lsuThread.write(arValid, true.B)
        rBus.ar.bits.addr := reqReg.aluResult
        lsuThread.waitCondition(rBus.ar.ready)
      } .elsewhen (isWrite) {
        lsuThread.write(awValid, true.B)
        wBus.aw.bits.addr := reqReg.aluResult
        lsuThread.waitCondition(wBus.aw.ready)
      }
    }

    lsuThread.Step {
      when (isRead) {
        lsuThread.write(rReady, true.B)
        lsuThread.waitCondition(rBus.r.valid)
        memData := rBus.r.bits.data
      } .elsewhen (isWrite) {
        lsuThread.write(wValid, true.B)
        wBus.w.bits.data := reqReg.memWData // 需根据 wstrb 处理
        lsuThread.waitCondition(wBus.w.ready)
      }
    }

    lsuThread.Step {
      when (isWrite) {
        lsuThread.write(bReady, true.B)
        lsuThread.waitCondition(wBus.b.valid)
      }
    }
  }


  

  //我们同时拥有：
  //非对齐地址，wstrb，size，三种寻址方式，没有问题


  val inAddrOffset = io.in.bits.aluResult(1, 0)
  val inWstrb = WireDefault(0.U(4.W))
  val inWdata = WireDefault(0.U(XLEN.W))
  val size    = WireDefault(0.U(3.W))
  switch(io.in.bits.ctrl.memFunct3) {
    is(0.U) { inWstrb := "b0001".U << inAddrOffset; inWdata := io.in.bits.memWData(7,0) << (inAddrOffset << 3);  size := 0.U } 
    is(1.U) { inWstrb := "b0011".U << inAddrOffset; inWdata := io.in.bits.memWData(15,0) << (inAddrOffset << 3); size := 1.U } 
    is(2.U) { inWstrb := "b1111".U;                  inWdata := io.in.bits.memWData;                             size := 2.U} 
  }








  val memReq = WireDefault(new AXI4BundleA(AXI_ID_WIDTH, XLEN), {
    val w = Wire(new AXI4BundleA(AXI_ID_WIDTH, XLEN))
    // 这里设置通用的“安全”默认值
    w.id    := 1.U
    w.addr  := io.in.bits.aluResult
    //w.addr  := Cat(io.in.bits.aluResult(XLEN -1, 2), "b00".U(2.W)) // 地址对齐
    w.len   := 0.U
    w.size  := size
    w.burst := 0.U
    w.lock  := false.B
    w.cache := 0.U
    w.prot  := 0.U
    w.qos   := 0.U
    w // 返回这个 wire
  })

  
  val storePack = WireDefault(new AXI4BundleW(XLEN), {
    val w = Wire(new AXI4BundleW(XLEN))
    w.data := inWdata
    w.strb := inWstrb
    w.last := 1.B
    w
  })


  val loadPack = Wire(new AXI4BundleR(AXI_ID_WIDTH, XLEN))

  val bPack = Wire(new AXI4BundleB(AXI_ID_WIDTH))


  readBridge.io.rReq.bits        := memReq
  loadPack                       := readBridge.io.rStream.bits
  //readBridge.io.req.bits.addr   := Cat(io.in.bits.aluResult(XLEN - 1, 2), "b00".U(2.W))

  //writeBridge.io.req.bits.addr  := Cat(io.in.bits.aluResult(XLEN -1, 2), "b00".U(2.W))
  writeBridge.io.wReq.bits       := memReq
  writeBridge.io.wStream.bits    := storePack
  bPack                          := writeBridge.io.bResp.bits



  //Bridge接口
  val isMemRead  = io.in.bits.ctrl.memEn && !io.in.bits.ctrl.memWen
  val isMemWrite = io.in.bits.ctrl.memEn && io.in.bits.ctrl.memWen
  val isNonMem   = !io.in.bits.ctrl.memEn


  val rawReadData = loadPack.data
  val finalLoadData = Wire(UInt(32.W))
  val shiftedData = rawReadData >> (reqReg.aluResult(1, 0) << 3)
  finalLoadData := 0.U




  switch(reqReg.ctrl.memFunct3) {
    is(0.U) { finalLoadData := Cat(Fill(24, shiftedData(7)), shiftedData(7,0)) }
    is(1.U) { finalLoadData := Cat(Fill(16, shiftedData(15)), shiftedData(15,0)) }
    is(2.U) { finalLoadData := rawReadData }
    is(4.U) { finalLoadData := shiftedData(7,0) }
    is(5.U) { finalLoadData := shiftedData(15,0) }
  }



  io.out.bits.connectDebug(reqReg)
  io.out.bits.rdAddr   := reqReg.rdAddr
  io.out.bits.regWen   := reqReg.ctrl.regWen
  io.out.bits.pcTarget := reqReg.pcTarget
  io.out.bits.wbData   := Mux(reqReg.ctrl.memEn && !reqReg.ctrl.memWen, finalLoadData, reqReg.aluResult)




  // 输出处理 (读取数据对齐等)


  //io.in.ready
  io.in.ready  := false.B
  io.out.valid := false.B
  readBridge.io.rReq.valid       := false.B
  writeBridge.io.wReq.valid      := false.B
  readBridge.io.rStream.ready    := false.B
  writeBridge.io.wStream.valid   := false.B
  writeBridge.io.bResp.ready     := false.B

  object State extends ChiselEnum { val sIdle, sWaitResp = Value }
  val state = RegInit(State.sIdle)

  io.in.ready := (state === State.sIdle) && (
    (isNonMem) || 
    (isMemRead && readBridge.io.rReq.ready) || 
    (isMemWrite && writeBridge.io.wReq.ready)
  )

  // read和write的Req valid
  // read 和write 的Stream ready


  switch(state) {
    is(State.sIdle) {
      when (io.in.valid) {
        when(io.in.ready) {
           reqReg := io.in.bits
           state  := State.sWaitResp
        }
        when (isMemRead) { readBridge.io.rReq.valid := true.B }
        .elsewhen (isMemWrite) { writeBridge.io.wReq.valid := true.B; writeBridge.io.wStream.valid := true.B}
      }
    }
    is(State.sWaitResp) {
      when (io.out.fire) { state := State.sIdle }
    }
  }

  when (state === State.sWaitResp) {
    when (reqReg.ctrl.memEn && !reqReg.ctrl.memWen) {
      io.out.valid := readBridge.io.rStream.valid
      readBridge.io.rStream.ready := io.out.ready
    } .elsewhen (reqReg.ctrl.memEn && reqReg.ctrl.memWen) {
      io.out.valid := writeBridge.io.bResp.valid
      writeBridge.io.bResp.ready := io.out.ready
    } .otherwise {
      io.out.valid := true.B
    }
  }



  // 握手逻辑


  when(readBridge.io.rReq.fire) {
    Debug.log("[DEBUG] [LSU]  read req: addr: %x, size: %x\n", memReq.addr, memReq.size)
  }

  when(readBridge.io.rStream.fire) {
    Debug.log("[DEBUG] [LSU]  read stream: rdata: %x, resp: %x\n", loadPack.data, loadPack.resp)
  }
  when(writeBridge.io.wReq.fire) {
    Debug.log("[DEBUG] [LSU]  write req: addr: %x, size: %x\n", memReq.addr, memReq.size)
  }
  when(writeBridge.io.wStream.fire) {
    Debug.log("[DEBUG] [LSU]: write stream: data: %x, wstrb: %x\n", storePack.data, storePack.strb)
  }








}