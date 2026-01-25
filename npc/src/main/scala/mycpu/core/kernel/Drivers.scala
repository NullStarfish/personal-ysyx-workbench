package mycpu.core.kernel

import chisel3._
import chisel3.util._
import mycpu.core.os._
import mycpu.common._
import mycpu.utils._

object Drivers {

  // =========================================================
  // 1. RegFile 驱动：支持真正的组合逻辑读
  // =========================================================
  class RegFileDriver(physRegs: Vec[UInt]) extends ResourceHandle {
    
    // 组合逻辑读 (用于 Logic 或 Thread 的非阻塞查看)
    override def read(addr: UInt, size: UInt, signed: Bool): UInt = {
      val res = Mux(addr === 0.U, 0.U, physRegs(addr))
      
      // [修改] 上下文自动感知
      ContextScope.current match {
        case LogicCtx(_) => 
          res // 组合逻辑直接返回 Wire
        case ThreadCtx(t) =>
          val latch = Reg(UInt(32.W))
          t.Step("Reg_Seq_Read") { latch := res }
          latch
      }
    }

    override def write(addr: UInt, data: UInt, size: UInt): UInt = {
      ContextScope.current match {
        case LogicCtx(l) => 
          when(addr =/= 0.U) { physRegs(addr) := data}
        
        case ThreadCtx(t) =>
          t.Step("Reg_Write") {
            when(addr =/= 0.U) {physRegs(addr) := data}
          }
      }
      0.U
    }
  }

  // =========================================================
  // 2. AXI 驱动：只支持时序操作
  // =========================================================
  private object BitOps {
    // 写：计算移位后的 Data 和 Strobe
    def alignWrite(addr: UInt, data: UInt, size: UInt): (UInt, UInt) = {
      val offset = addr(1, 0)
      val shiftedData = data << (offset << 3)
      val baseStrb = MuxLookup(size, "b1111".U)(Seq(
        1.U -> "b0001".U,
        2.U -> "b0011".U,
        4.U -> "b1111".U
      ))
      val shiftedStrb = baseStrb << offset
      (shiftedData, shiftedStrb)
    }

    // 读：计算移位回来的 Data 并做扩展
    def alignRead(addr: UInt, rawData: UInt, size: UInt, signed: Bool): UInt = {
      val offset = addr(1, 0)
      val shifted = rawData >> (offset << 3)
      MuxLookup(size, shifted)(Seq(
        1.U -> Mux(signed, (shifted(7,0).asSInt).asUInt, shifted(7,0)),
        2.U -> Mux(signed, (shifted(15,0).asSInt).asUInt, shifted(15,0)),
        4.U -> shifted
      ))
    }
  }

  class AXIDriver(bus: AXI4Bundle) extends ResourceHandle {
    // 必须接管所有信号
    // 注意：这里需要一个全局仲裁器，或者假设每个进程有独立的 Virtual Bus
    // 为演示，我们假设这是独占的

    override def read(addr: UInt, size: UInt, signed: Bool): UInt = {
      ContextScope.current match {
        case LogicCtx(_) =>
          // AXI 是时序总线，不支持组合逻辑直接读
          throw new Exception(s"[Error] Cannot read AXI bus combinationally in Logic '${ContextScope.current}'! AXI requires a Thread.")
        
        case ThreadCtx(t) =>
          // === 时序读逻辑 ===
          val rawData = Reg(UInt(XLEN.W))
          
          // 1. 接管信号
          val arValid = t.driveManaged(bus.ar.valid, false.B)
          val arBits  = t.driveManaged(bus.ar.bits,  0.U.asTypeOf(bus.ar.bits))
          val rReady  = t.driveManaged(bus.r.ready,  false.B)

          // 2. 发送地址
          t.Step("AXI_AR") {
            t.write(arValid, true.B)
            val p = Wire(chiselTypeOf(bus.ar.bits))
            p := 0.U.asTypeOf(p); p.addr := addr; p.size := size; p.burst := 1.U
            t.write(arBits, p)
            t.waitCondition(bus.ar.ready)
          }

          // 3. 接收数据
          t.Step("AXI_R") {
            t.write(rReady, true.B)
            t.waitCondition(bus.r.valid)
            rawData := bus.r.bits.data
          }

          // 4. 后处理 (组合逻辑)
          // 既然是 Thread，我们可以在返回时直接应用组合逻辑处理 Reg 中的值
          // 这样 LSU 拿到的是已经处理好的 Wire
          BitOps.alignRead(addr, rawData, size, signed)
      }
    }

    override def write(addr: UInt, data: UInt, size: UInt): UInt = {
      ContextScope.current match {
        case LogicCtx(_) =>
          throw new Exception("[Error] Cannot write AXI bus from Logic! Use a Thread.")
        
        case ThreadCtx(t) =>
          val (wdata, wstrb) = BitOps.alignWrite(addr, data, size)
          val resp = Reg(UInt(2.W))

          val awValid = t.driveManaged(bus.aw.valid, false.B)
          val awBits  = t.driveManaged(bus.aw.bits,  0.U.asTypeOf(bus.aw.bits))
          val wValid  = t.driveManaged(bus.w.valid,  false.B)
          val wBits   = t.driveManaged(bus.w.bits,   0.U.asTypeOf(bus.w.bits))
          val bReady  = t.driveManaged(bus.b.ready,  false.B)

          // 1. 地址
          t.Step("AXI_AW") {
            t.write(awValid, true.B)
            val p = Wire(chiselTypeOf(bus.aw.bits))
            p := 0.U.asTypeOf(p); p.addr := addr; p.size := size
            t.write(awBits, p)
            t.waitCondition(bus.aw.ready)
          }

          // 2. 数据 (使用计算好的 wdata/wstrb)
          t.Step("AXI_W") {
            t.write(wValid, true.B)
            val p = Wire(chiselTypeOf(bus.w.bits))
            p := 0.U.asTypeOf(p); p.data := wdata; p.strb := wstrb; p.last := true.B
            t.write(wBits, p)
            t.waitCondition(bus.w.ready)
          }

          // 3. 响应
          t.Step("AXI_B") {
            t.write(bReady, true.B)
            t.waitCondition(bus.b.valid)
            resp := bus.b.bits.resp
          }
          
          resp // 返回错误码
      }
    }
  }
}