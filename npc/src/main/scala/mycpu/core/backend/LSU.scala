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
    val axi = new AXI4LiteBundle(XLEN, XLEN)
  })

  // =======================================================
  // 1. 数据结构定义
  // =======================================================
  
  // 响应队列的 Payload：包含原始指令信息 + AXI 读回来的原始数据
  class LsuRespPacket extends Bundle {
    val req  = new ExecutePacket
    val rdata = UInt(XLEN.W) // 还没有移位/符号扩展的原始数据
  }

  // 1.1 输入队列 (Request Queue)
  val reqQueue = Module(new Queue(new ExecutePacket, entries = 3))
  reqQueue.io.enq <> io.in

  // 1.2 输出缓冲队列 (Response Queue)
  val respQueue = Module(new Queue(new LsuRespPacket, entries = 3))

  QueueProbe(reqQueue.io.deq, "LSU_Dispatch")
  QueueProbe(respQueue.io.enq, "LSU_Commit")
  
  // 默认不 Pop/Push，由 Thread 控制
  reqQueue.io.deq.ready  := false.B
  respQueue.io.enq.valid := false.B
  respQueue.io.enq.bits  := DontCare


  // =======================================================
  // 2. 辅助逻辑 (Combinational Logic)
  // =======================================================
  // 预先计算写操作所需的 Address, Data, Strb
  // 这些逻辑直接基于 reqQueue 的队头，供 Thread 使用
  
  val currReq = reqQueue.io.deq.bits
  val addrOffset = currReq.aluResult(1, 0)
  
  // WStrb 和 WData 计算
  val calcWStrb = WireDefault(0.U(4.W))
  val calcWData = WireDefault(0.U(XLEN.W))
  val calcSize  = WireDefault(0.U(3.W))

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

  // 构建 AXI 请求包的模版
  val axiAddrPacket = Wire(new AXI4BundleA(AXI_ID_WIDTH, XLEN))
  axiAddrPacket.id    := 0.U
  axiAddrPacket.addr  := currReq.aluResult // 对齐与否交给 AXI 互联或在 Logic 处理，这里发原始地址
  axiAddrPacket.len   := 0.U
  axiAddrPacket.size  := calcSize
  axiAddrPacket.burst := AXI4Parameters.BURST_FIXED // Lite 只有 Fixed 或 Incr 均可，通常 Lite 不看 Burst
  axiAddrPacket.lock  := false.B
  axiAddrPacket.cache := 0.U
  axiAddrPacket.prot  := 0.U
  axiAddrPacket.qos   := 0.U

  val axiDataPacket = Wire(new AXI4BundleW(XLEN))
  axiDataPacket.data := calcWData
  axiDataPacket.strb := calcWStrb
  axiDataPacket.last := true.B

  // 状态判定
  val isRead  = currReq.ctrl.memEn && !currReq.ctrl.memWen
  val isWrite = currReq.ctrl.memEn && currReq.ctrl.memWen
  val isNonMem = !currReq.ctrl.memEn

  // =======================================================
  // 3. Hardware Thread (AXI Transaction Manager)
  // =======================================================

  val AXI4Split(rBus, wBus) = io.axi
  val lsuThread = new HardwareThread("LSU_Core", debugEnable = true)

  // 接管 AXI 信号
  val arValidProxy = lsuThread.driveManaged(rBus.ar.valid, false.B)
  val arBitsProxy  = lsuThread.driveManaged(rBus.ar.bits,  DontCare)
  val rReadyProxy  = lsuThread.driveManaged(rBus.r.ready,  false.B)

  val awValidProxy = lsuThread.driveManaged(wBus.aw.valid, false.B)
  val awBitsProxy  = lsuThread.driveManaged(wBus.aw.bits,  DontCare)
  val wValidProxy  = lsuThread.driveManaged(wBus.w.valid,  false.B)
  val wBitsProxy   = lsuThread.driveManaged(wBus.w.bits,   DontCare)
  val bReadyProxy  = lsuThread.driveManaged(wBus.b.ready,  false.B)
  
  
  // 启动条件：有请求 且 下游能接收
  lsuThread.startWhen(reqQueue.io.deq.valid && respQueue.io.enq.ready)

   lsuThread.entry {
    val readDataLatch = RegInit(0.U(XLEN.W))

    // =========================================================
    // 定义跳转标签 (Wire)
    // =========================================================
    val LabelRead   = Wire(UInt(32.W))
    val LabelWrite  = Wire(UInt(32.W))
    val LabelSkip   = Wire(UInt(32.W))
    val LabelCommit = Wire(UInt(32.W))

    // Step 0: Dispatch (分发器)
    // 类似于 CPU 的 ID 阶段，决定下一跳去哪里
    lsuThread.Step("Dispatch") {
      when (isRead) {
        lsuThread.pc := LabelRead // 只有 isRead 时，pc 才会跳到 Read 流程
      } .elsewhen (isWrite) {
        lsuThread.pc := LabelWrite
      } .otherwise {
        lsuThread.pc := LabelSkip // MV指令会走这里，跳过所有 AXI 操作
      }
    }

    LabelRead := lsuThread.Label // 标记当前 Label 为 Step 1
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
    
    // 读完后，直接跳去提交
    lsuThread.Step("JUMP_TO_COMMIT_R") {
       lsuThread.pc := LabelCommit 
    }

    // =========================================================
    // Write Flow
    // =========================================================
    LabelWrite := lsuThread.Label
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
    
    // 写完后，跳去提交
    lsuThread.Step("JUMP_TO_COMMIT_W") {
       lsuThread.pc := LabelCommit 
    }

    // =========================================================
    // Skip Flow (非内存指令)
    // =========================================================
    LabelSkip := lsuThread.Label
    lsuThread.Step("PASS_THROUGH") {
       // 什么也不做，ALU结果直接透传
       // 这里不需要wait，一拍就过
       // 也不需要显式 jump，因为下面紧接着就是 Commit
    }

    // =========================================================
    // Commit (Unified Exit)
    // =========================================================
    LabelCommit := lsuThread.Label
    lsuThread.Step("COMMIT") {
       respQueue.io.enq.valid    := true.B
       respQueue.io.enq.bits.req := currReq
       
       // 如果是读，用 latch 数据；否则用 0 (ALU结果已在 req 中)
       respQueue.io.enq.bits.rdata := Mux(isRead, readDataLatch, 0.U)
       
       reqQueue.io.deq.ready     := true.B // Pop Request
       // 执行完最后一步，Thread 自动 active := false
    }
  }


  // =======================================================
  // 4. WriteBack 转码逻辑 (Combinational Logic)
  // =======================================================
  val wbLogic = new HardwareLogic("LSU_Format_Logic")
  val outValidProxy = wbLogic.driveManaged(io.out.valid, false.B)
  // out.bits 我们直接赋值，因为它是由数据流驱动的
  
  wbLogic.run {
    // 默认连接
    io.out.bits := DontCare
    
    val resp     = respQueue.io.deq.bits
    val original = resp.req
    val rawData  = resp.rdata
    val addrLow  = original.aluResult(1, 0)
    
    // 数据移位 (Alignment)
    val shiftedData = rawData >> (addrLow << 3) // addrLow * 8
    val finalLoadData = WireDefault(0.U(XLEN.W))

    // Load 数据格式化 (Masking & Sign Extension)
    // 根据 funct3: LB, LH, LW, LBU, LHU
    switch(original.ctrl.memFunct3) {
      is(0.U)  { finalLoadData := Cat(Fill(24, shiftedData(7)),  shiftedData(7,0)) }
      is(1.U)  { finalLoadData := Cat(Fill(16, shiftedData(15)), shiftedData(15,0)) }
      is(2.U)  { finalLoadData := rawData } // Word 假设对齐
      is(4.U) { finalLoadData := shiftedData(7,0) }
      is(5.U) { finalLoadData := shiftedData(15,0) }
      is(6.U)                     { finalLoadData := rawData } // LWU
    }

    // 最终 WB 数据选择
    val wbData = Mux(original.ctrl.memEn && !original.ctrl.memWen, 
                     finalLoadData,      // Load 指令用内存数据
                     original.aluResult) // ALU/Store 指令用 ALU 结果 (Store 不需要写回寄存器，但 WB 阶段可能需要 ALU 结果做其他用途，或置0)

    // 驱动输出
    when (respQueue.io.deq.valid) {
      wbLogic.write(outValidProxy, true.B)
      
      io.out.bits.connectDebug(original) // 复制 PC, Inst, DNPC
      io.out.bits.wbData   := wbData
      io.out.bits.rdAddr   := original.rdAddr
      io.out.bits.regWen   := original.ctrl.regWen
      io.out.bits.pcTarget := original.pcTarget // Branch/Jump 目标
      
      // 只有下游接受了，才弹出 RespQueue
      respQueue.io.deq.ready := io.out.ready
    } .otherwise {
      respQueue.io.deq.ready := false.B
    }
  }
  
  // 补充 Instruction 定义里的 funct3，如果 common 包里没有，这里可能需要手动写 0.U, 1.U 等
  // 假设 Instructions.LB.funct3 不可用，直接用数字:
  // LB=0, LH=1, LW=2, LBU=4, LHU=5
}