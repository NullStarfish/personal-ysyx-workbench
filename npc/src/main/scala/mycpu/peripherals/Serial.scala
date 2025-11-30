package mycpu.peripherals

import chisel3._
import chisel3.util._
import mycpu.utils._
import mycpu.DeviceConfig

class Serial extends Peripheral(mycpu.MemMap.devices(1)) {
  io.bus.setAsSlave() // 初始化所有信号为默认值
  
  // 使用之前定义的 Splitter 拆分读写通道
  val AXI4Split(rBus, wBus) = io.bus

  // ==============================================================================
  // 1. 读通道处理 (必须处理，防止死锁)
  // ==============================================================================
  // 即使是只写外设，如果 CPU 误读，也必须返回一个 0 和 Valid，否则总线会 Hang 住
  rBus.ar.ready := true.B
  rBus.r.valid  := RegNext(rBus.ar.valid, init = false.B) // 简单延迟一拍回数据
  rBus.r.bits.data := 0.U
  rBus.r.bits.resp := 0.U // OKAY

  // ==============================================================================
  // 2. 写通道逻辑
  // ==============================================================================
  object State extends ChiselEnum {
    val sIdle, sIssue, sWaitResp = Value
  }

  val state = RegInit(State.sIdle)
  
  // 暂存地址和数据的寄存器
  val reqAddrReg = Reg(UInt(localAddrWidth.W)) // 使用父类定义的 localAddrWidth
  val printReg   = Reg(UInt(32.W))             // 暂存拼接后的数据

  // 默认输出信号 (防止锁存器推断)
  wBus.aw.ready    := false.B
  wBus.w.ready     := false.B
  wBus.b.valid     := false.B
  wBus.b.bits.resp := 0.U // OKAY



  when(wBus.aw.fire) {
    Debug.log("[DEBUG] [Serial]: aw req: addr: %x, prot: %x\n", wBus.aw.bits.addr, wBus.aw.bits.prot)
  }
  when(wBus.w.fire) {
    Debug.log("[DEBUG] [Serial]: w req: data: %x, strb: %x\n", wBus.w.bits.data, wBus.w.bits.strb)
  }
  when(wBus.b.fire) {
    Debug.log("[DEBUG] [Serial]: write success b: resp : %x\n", wBus.b.bits.resp)
  }

  val lastState = RegNext(state, State.sIdle)
  when(state =/= lastState) {
    Debug.log("[DEBUG] State transition: %x -> %x\n", lastState.asUInt, state.asUInt)
  }
  switch(state) {
    // ------------------------------------------------------------------
    // State: Idle - 等待写地址 (AW)
    // ------------------------------------------------------------------
    is(State.sIdle) {
      wBus.aw.ready := true.B
      
      when(wBus.aw.fire) {
        // 截取需要的地址位 (使用 Peripheral 中的方法或直接截取)
        reqAddrReg := wBus.aw.bits.addr(localAddrWidth - 1, 0)
        state := State.sIssue
      }
    }

    // ------------------------------------------------------------------
    // State: Issue - 等待写数据 (W) 并处理掩码
    // ------------------------------------------------------------------
    is(State.sIssue) {
      wBus.w.ready := true.B

      when(wBus.w.fire) {
        // 只有当地址偏移量为 0 时才处理 (根据 DeviceConfig)
        when(reqAddrReg === 0.U) {
          // ------------------------------------------------------
          // 修正后的 Strobe 掩码逻辑
          // ------------------------------------------------------
          val wData = wBus.w.bits.data
          val wStrb = wBus.w.bits.strb
          
          // 方法：先拆分成字节，根据 strb 决定是否保留，再拼回去
          val maskedBytes = Wire(Vec(4, UInt(8.W)))
          
          for (i <- 0 until 4) {
            // 如果 strb(i) 为 1，则取对应字节，否则置 0
            maskedBytes(i) := Mux(wStrb(i), wData(i * 8 + 7, i * 8), 0.U(8.W))
          }
          

          // 将 Vec 转换回 UInt
          printReg := maskedBytes.asUInt
        }

        
        state := State.sWaitResp
      }
    }

    // ------------------------------------------------------------------
    // State: WaitResp - 执行打印 并 回复写响应 (B)
    // ------------------------------------------------------------------
    is(State.sWaitResp) {
      wBus.b.valid := true.B
      
      // 在握手成功的这一周期执行打印
      when(wBus.b.fire) {
        // 打印低 8 位 (char)
        // 注意：Chisel 的 printf 是在时钟上升沿触发的
        // %c 对应 Scala Char 或 C char
        printf("%c", printReg(7, 0))
        
        state := State.sIdle
      }
    }
  }
}