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
    val axi = new AXI4Bundle(AXI_ID_WIDTH, XLEN, XLEN)
  })

  // =======================================================
  // 1. 数据结构与队列定义 (使用 HwQueue)
  // =======================================================

  val AXI4Split(rBus, wBus) = io.axi
  
  class LsuRespPacket extends Bundle {
    val req   = new ExecutePacket
    val rdata = UInt(XLEN.W) 
  }

  val reqPipe  = new HwQueue(new ExecutePacket, 3, "LSU_Req_Pipe")
  val respPipe = new HwQueue(new LsuRespPacket, 3, "LSU_Resp_Pipe")

  // 连接外部输入到 Input Pipe
  reqPipe.enq <> io.in


  val currReq = reqPipe.peek()
  
  val addrOffset = currReq.aluResult(1, 0)
  val calcWStrb  = WireDefault(0.U(4.W))
  val calcWData  = WireDefault(0.U(XLEN.W))
  val calcSize   = WireDefault(0.U(3.W))

  switch(currReq.ctrl.memFunct3) {
    is(0.U) { // Byte
      calcWStrb := "b0001".U << addrOffset
      calcWData := currReq.memWData(7,0) << (addrOffset << 3)
      calcSize  := 0.U 
    } 
    is(1.U) { // Half
      calcWStrb := "b0011".U << addrOffset
      calcWData := currReq.memWData(15,0) << (addrOffset << 3)
      calcSize  := 1.U 
    } 
    is(2.U) { // Word
      calcWStrb := "b1111".U
      calcWData := currReq.memWData
      calcSize  := 2.U
    } 
  }

  // AXI 包构建
  val axiAddrPacket = Wire(chiselTypeOf(rBus.ar.bits))
  axiAddrPacket     := 0.U.asTypeOf(axiAddrPacket) // Init defaults
  axiAddrPacket.addr  := currReq.aluResult
  axiAddrPacket.size  := calcSize
  axiAddrPacket.burst := AXI4Parameters.BURST_FIXED

  val axiDataPacket = Wire(chiselTypeOf(wBus.w.bits))
  axiDataPacket.data := calcWData
  axiDataPacket.strb := calcWStrb
  axiDataPacket.last := true.B

  // 操作类型判定
  val isRead   = currReq.ctrl.memEn && !currReq.ctrl.memWen
  val isWrite  = currReq.ctrl.memEn && currReq.ctrl.memWen
  
  // =======================================================
  // 3. Hardware Thread (AXI 事务管理器)
  // =======================================================

  val lsuThread = new HardwareThread("LSU_Core", debugEnable = true)


  val arValidProxy = lsuThread.driveManaged(rBus.ar.valid, false.B)
  val arBitsProxy  = lsuThread.driveManaged(rBus.ar.bits,  DontCare)
  val rReadyProxy  = lsuThread.driveManaged(rBus.r.ready,  false.B)

  val awValidProxy = lsuThread.driveManaged(wBus.aw.valid, false.B)
  val awBitsProxy  = lsuThread.driveManaged(wBus.aw.bits,  DontCare)
  val wValidProxy  = lsuThread.driveManaged(wBus.w.valid,  false.B)
  val wBitsProxy   = lsuThread.driveManaged(wBus.w.bits,   DontCare)
  val bReadyProxy  = lsuThread.driveManaged(wBus.b.ready,  false.B)
  

  lsuThread.startWhen(reqPipe.canPop && respPipe.canPush)

  lsuThread.entry {
    val readDataLatch = RegInit(0.U(XLEN.W))

    // 跳转标签定义
    val L_Read   = Wire(UInt(32.W))
    val L_Write  = Wire(UInt(32.W))
    val L_Skip   = Wire(UInt(32.W))
    val L_Commit = Wire(UInt(32.W))

    // Step 0: Dispatch (分发)
    lsuThread.Step("Dispatch") {
      when (isRead) {
        lsuThread.pc := L_Read
      } .elsewhen (isWrite) {
        lsuThread.pc := L_Write
      } .otherwise {
        lsuThread.pc := L_Skip // 非内存指令直接跳过
      }
    }

    // --- Read Flow ---
    L_Read := lsuThread.Label
    lsuThread.Step("AXI_READ_ADDR") {
      lsuThread.write(arValidProxy, true.B)
      lsuThread.write(arBitsProxy, axiAddrPacket)
      lsuThread.waitCondition(rBus.ar.ready)
    }

    lsuThread.Step("AXI_READ_DATA") {
      lsuThread.write(rReadyProxy, true.B)
      lsuThread.waitCondition(rBus.r.valid)
      readDataLatch := rBus.r.bits.data
    }
    
    lsuThread.Step("JUMP_COMMIT_R") { lsuThread.pc := L_Commit }

    // --- Write Flow ---
    L_Write := lsuThread.Label
    lsuThread.Step("AXI_WRITE_ADDR") {
      lsuThread.write(awValidProxy, true.B)
      lsuThread.write(awBitsProxy, axiAddrPacket)
      lsuThread.waitCondition(wBus.aw.ready)
    }

    lsuThread.Step("AXI_WRITE_DATA") {
      lsuThread.write(wValidProxy, true.B)
      lsuThread.write(wBitsProxy, axiDataPacket)
      lsuThread.waitCondition(wBus.w.ready)
    }

    lsuThread.Step("AXI_WRITE_RESP") {
      lsuThread.write(bReadyProxy, true.B)
      lsuThread.waitCondition(wBus.b.valid)
    }
    
    lsuThread.Step("JUMP_COMMIT_W") { lsuThread.pc := L_Commit }

    // --- Skip Flow ---
    L_Skip := lsuThread.Label
    lsuThread.Step("PASS_THROUGH") {
       // 空操作，下一拍自动进入 Commit
    }

    // --- Commit Flow ---
    L_Commit := lsuThread.Label
    lsuThread.Step("COMMIT") {
       val resp = Wire(new LsuRespPacket)
       resp.req := currReq
       // 如果是读，使用 latch 数据；否则使用 0 (ALU结果已在 req 中)
       resp.rdata := Mux(isRead, readDataLatch, 0.U)
       
       // [修改] 使用 HwQueue API 进行原子提交
       // 尝试推入结果
       when (respPipe.tryPush(resp)) {
         // 推入成功后，立刻消耗掉请求
         reqPipe.tryPop()
         // 线程结束，自动 active := false
       } .otherwise {
         // 如果 RespQueue 满了（虽然 startWhen 检查过，但可能被 logic 堵住），等待
         lsuThread.waitCondition(false.B)
       }
    }
  }

  // =======================================================
  // 4. WriteBack 转码逻辑 (消费者)
  // =======================================================
  val wbLogic = new HardwareLogic("LSU_Format_Logic")
  val outValidProxy = wbLogic.driveManaged(io.out.valid, false.B)
  
  wbLogic.run {
    io.out.bits := DontCare // 默认
    
    // [修改] 使用 HwQueue API 检查是否有数据
    when (respPipe.canPop) {
      val resp     = respPipe.peek()
      val original = resp.req
      val rawData  = resp.rdata
      val addrLow  = original.aluResult(1, 0)
      
      // 数据处理 (Shift & Extend)
      val shiftedData = rawData >> (addrLow << 3)
      val finalLoadData = WireDefault(0.U(XLEN.W))

      switch(original.ctrl.memFunct3) {
        is(0.U) { finalLoadData := Cat(Fill(24, shiftedData(7)),  shiftedData(7,0)) }
        is(1.U) { finalLoadData := Cat(Fill(16, shiftedData(15)), shiftedData(15,0)) }
        is(2.U) { finalLoadData := rawData }
        is(4.U) { finalLoadData := shiftedData(7,0) }
        is(5.U) { finalLoadData := shiftedData(15,0) }
        is(6.U) { finalLoadData := rawData }
      }

      val wbData = Mux(original.ctrl.memEn && !original.ctrl.memWen, 
                       finalLoadData,      
                       original.aluResult)

      // 驱动输出
      wbLogic.write(outValidProxy, true.B)
      io.out.bits.connectDebug(original)
      io.out.bits.wbData   := wbData
      io.out.bits.rdAddr   := original.rdAddr
      io.out.bits.regWen   := original.ctrl.regWen
      io.out.bits.pcTarget := original.pcTarget

      // 握手：下游 Ready 后，弹出 RespQueue
      when (io.out.ready) {
        respPipe.tryPop()
      }
    }
  }
}