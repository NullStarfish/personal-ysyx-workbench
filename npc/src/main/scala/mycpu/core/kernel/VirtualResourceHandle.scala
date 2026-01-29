package mycpu.core.kernel

import chisel3._
import chisel3.util._
import mycpu.core.os._
import mycpu.utils._

class VirtualResourceHandle(val driverMeta: DriverMeta, channel: ClientChannel) {

  def sys_read(addr: UInt, size: UInt = 2.U): SysResult = {
    val res = Wire(new SysResult)
    // 默认值
    res.errno := Errno.ESUCCESS
    res.value := 0.U

    ContextScope.current match {
      case LogicCtx(_) =>
        // 纯逻辑上下文中，只允许组合逻辑读取
        if (driverMeta.readTiming == DriverTiming.Combinational) {
          channel.req.valid := true.B 
          channel.req.wen   := false.B
          channel.req.addr  := addr
          channel.req.size  := size
          channel.req.data  := 0.U
          
          res.value := channel.respData
          res.errno := channel.error
        } else {
           throw new Exception(s"[Error] Cannot perform Combinational Read on Sequential device '${driverMeta.name}' inside LogicCtx")
        }

      case AtomicCtx(_) => 
        // [修复] 在 Step 内部 (AtomicCtx)，允许对时序设备发起请求
        // 这是一个 "Fire-and-Wait" 模式
        channel.req.valid := true.B
        channel.req.wen   := false.B
        channel.req.addr  := addr
        channel.req.size  := size
        channel.req.data  := 0.U
        
        // [关键] 映射结果
        if (driverMeta.readTiming == DriverTiming.Combinational) {
            // 组合设备：立即完成
            res.value := channel.respData
            res.errno := channel.error
        } else {
            // 时序设备：如果未 Ready，则返回 EBUSY，迫使 waitCondition 阻塞
            res.value := channel.respData
            res.errno := Mux(channel.ready, channel.error, Errno.EBUSY)
        }

      case ThreadCtx(t) =>
        // 这种模式是 sys_read 自己生成 Step，通常用于 entry 代码块顶层
        if (driverMeta.readTiming == DriverTiming.Combinational) {
          channel.req.valid := true.B
          channel.req.wen   := false.B
          channel.req.addr  := addr
          channel.req.size  := size
          channel.req.data  := 0.U
          
          res.value := channel.respData
          res.errno := channel.error
        } else {
          val latchData = Reg(UInt(32.W))
          val latchErr  = Reg(UInt(8.W))
          
          t.Step(s"Read_${driverMeta.name}") {
            channel.req.valid := true.B
            channel.req.wen   := false.B
            channel.req.addr  := addr
            channel.req.size  := size
            channel.req.data  := 0.U
            
            t.waitCondition(channel.ready)
            latchData := channel.respData
            latchErr  := channel.error
          }
          res.value := latchData
          res.errno := latchErr
        }
    }
    res
  }

  def sys_write(addr: UInt, data: UInt, size: UInt = 2.U): SysResult = {
    val res = Wire(new SysResult)
    res.errno := Errno.ESUCCESS
    res.value := 0.U

    ContextScope.current match {
      case LogicCtx(_) =>
        throw new Exception("Cannot perform sys_write inside LogicCtx (Side-effects forbidden in pure logic)")
        
      case AtomicCtx(_) =>
        // [修复] Step 内部写操作
        channel.req.valid := true.B
        channel.req.wen   := true.B
        channel.req.addr  := addr
        channel.req.data  := data
        channel.req.size  := size
        
        // [关键] 返回 EBUSY 直到 Ready
        // 组合写（如 RegFile）通常立即 Ready，时序写（如 RAM）需要等待
        res.errno := Mux(channel.ready, channel.error, Errno.EBUSY)
        
      case ThreadCtx(t) =>
        val latchErr = Reg(UInt(8.W))
        t.Step(s"Write_${driverMeta.name}") {
          channel.req.valid := true.B
          channel.req.wen   := true.B
          channel.req.addr  := addr
          channel.req.data  := data
          channel.req.size  := size
          
          t.waitCondition(channel.ready)
          latchErr := channel.error
        }
        res.errno := latchErr
    }
    res
  }
}