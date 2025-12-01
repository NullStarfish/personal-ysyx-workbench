package mycpu.peripherals

import chisel3._
import chisel3.util._
import mycpu._
import mycpu.utils._
import mycpu.common.XLEN
import mycpu.DeviceConfig

class Serial extends Peripheral(MemMap.devices(1)) {
  // 1. 初始化总线为从机模式
  io.bus.setAsSlave() 
  
  // 2. 拆分 AXI 总线
  val AXI4Split(rBus, wBus) = io.bus

  // 3. 实例化 Slave Bridges
  // 这些 Bridge 负责处理复杂的 AXI 握手、对齐和错误码转换
  val readBridge  = Module(new AXI4LiteReadSlaveBridge(XLEN, XLEN))
  val writeBridge = Module(new AXI4LiteWriteSlaveBridge(XLEN, XLEN))

  // 4. 连接 AXI 侧
  readBridge.io.axi  <> rBus
  writeBridge.io.axi <> wBus

  // ==============================================================================
  // 5. 读通道逻辑 (Read Logic)
  // ==============================================================================
  // 逻辑：永远准备好接收请求，接收到请求的下一拍返回 0
  
  readBridge.io.req.ready := true.B // 串口随时可读（虽然读出来是0）

  // 构建简单的流水线响应：Request Valid -> Delay 1 Cycle -> Response Valid
  val rValid = RegNext(readBridge.io.req.valid, init = false.B)

  readBridge.io.resp.valid        := rValid
  readBridge.io.resp.bits.rdata   := 0.U      // 读串口返回 0
  readBridge.io.resp.bits.isError := false.B  // 没有错误

  // ==============================================================================
  // 6. 写通道逻辑 (Write Logic)
  // ==============================================================================
  // 逻辑：WriteBridge 已经帮我们把 AW (地址) 和 W (数据) 对齐了。
  // 我们只需要处理 simpleWriteReq 即可。

  writeBridge.io.req.ready := true.B // 串口随时可写

  // 提取请求信号
  val reqValid = writeBridge.io.req.valid
  val reqAddr  = writeBridge.io.req.bits.addr
  val reqData  = writeBridge.io.req.bits.wdata
  val reqStrb  = writeBridge.io.req.bits.wstrb
  
  // 计算局部偏移量 (不再使用 io.bus 上的信号，而是使用 Bridge 输出的对齐后的地址)
  val offset = reqAddr(localAddrWidth - 1, 0)

  // 处理写逻辑
  when(reqValid) {
    Debug.log("[DEBUG] [Serial] Write Req: addr=%x data=%x strb=%x\n", reqAddr, reqData, reqStrb)

    // 只有当偏移量为 0 时才处理
    when(offset === 0.U) {
      // ------------------------------------------------------
      // Strobe 掩码逻辑 (保留你原有的逻辑)
      // ------------------------------------------------------
      val maskedBytes = Wire(Vec(4, UInt(8.W)))
      for (i <- 0 until 4) {
        maskedBytes(i) := Mux(reqStrb(i), reqData(i * 8 + 7, i * 8), 0.U(8.W))
      }
      val finalData = maskedBytes.asUInt

      // ------------------------------------------------------
      // 执行打印
      // ------------------------------------------------------
      // Chisel printf 在时钟上升沿且条件为真时触发
      printf("%c", finalData(7, 0))
    }
  }

  // 构建写响应
  // 也是延迟一拍回复 (虽然对于 printf 来说不需要等待，但为了时序更好，习惯性打一拍)
  val wValid = RegNext(reqValid, init = false.B)

  writeBridge.io.resp.valid        := wValid
  writeBridge.io.resp.bits.isError := false.B // 写成功
}