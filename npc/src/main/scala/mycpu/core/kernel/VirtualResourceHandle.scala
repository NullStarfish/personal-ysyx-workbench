package mycpu.core.kernel

import chisel3._
import chisel3.util._
import mycpu.common._
import mycpu.core.os._
import mycpu.utils._

class VirtualResourceHandle(val driverMeta: DriverMeta, channel: ClientChannel) {

  def sys_read(addr: UInt, size: UInt = 2.U): SysResult = {
    val res = Wire(new SysResult)
    res.errno := Errno.ESUCCESS
    res.value := 0.U

    ContextScope.current match {
      case AtomicCtx(t) => 
        val doneReg = RegInit(false.B)
        val dataReg = Reg(UInt(KERNEL_DATA_WIDTH.W))
        val errReg  = RegInit(Errno.ESUCCESS)

        // PC 变动（Step 切换）或 线程刚启动时，重置 done 状态
        val isFirstCycle = RegNext(t.pc) =/= t.pc || (RegNext(t.isRunning) === false.B && t.isRunning === true.B)
        when(isFirstCycle) { doneReg := false.B }

        channel.req.valid := false.B
        channel.req.wen   := false.B

        when(!doneReg) {
          channel.req.valid := true.B
          channel.req.addr  := addr
          channel.req.size  := size
          
          when(channel.ready) {
            doneReg := true.B
            dataReg := channel.respData
            errReg  := channel.error
          }
          if (driverMeta.readTiming == DriverTiming.Sequential) {
            t.waitCondition(channel.ready)
          }
        }

        // [关键修复] isReadyNow 逻辑：组合判定 Ready，消除一拍延迟带来的死锁
        val isReadyNow = channel.ready || doneReg
        res.value := Mux(doneReg, dataReg, channel.respData)
        res.errno := Mux(isReadyNow, Mux(doneReg, errReg, channel.error), Errno.EBUSY)

      case ThreadCtx(t) =>
        val latchData = Reg(UInt(KERNEL_DATA_WIDTH.W))
        t.Step(s"Read_${driverMeta.name}") {
          val sRes = this.sys_read(addr, size)
          t.waitCondition(sRes.errno === Errno.ESUCCESS)
          latchData := sRes.value
        }
        res.value := latchData
        res.errno := Errno.ESUCCESS
      case _ =>
        channel.req.valid := true.B; channel.req.addr := addr; channel.req.size := size
        res.value := channel.respData; res.errno := channel.error
    }
    res
  }

  def sys_write(addr: UInt, data: UInt, size: UInt = 2.U): SysResult = {
    val res = Wire(new SysResult)
    res.errno := Errno.ESUCCESS
    res.value := 0.U

    ContextScope.current match {
      case AtomicCtx(t) =>
        val doneReg = RegInit(false.B)
        val errReg  = RegInit(Errno.ESUCCESS)
        val isFirstCycle = RegNext(t.pc) =/= t.pc || (RegNext(t.isRunning) === false.B && t.isRunning === true.B)
        when(isFirstCycle) { doneReg := false.B }

        channel.req.valid := false.B
        channel.req.wen   := true.B

        when(!doneReg) {
          channel.req.valid := true.B
          channel.req.wen   := true.B
          channel.req.addr  := addr
          channel.req.data  := data
          channel.req.size  := size
          when(channel.ready) {
            doneReg := true.B
            errReg  := channel.error
          }
          t.waitCondition(channel.ready)
        }
        val isReadyNow = channel.ready || doneReg
        res.errno := Mux(isReadyNow, Mux(doneReg, errReg, channel.error), Errno.EBUSY)
      case ThreadCtx(t) =>
        t.Step(s"Write_${driverMeta.name}") {
          val sRes = this.sys_write(addr, data, size)
          t.waitCondition(sRes.errno === Errno.ESUCCESS)
        }
        res.errno := Errno.ESUCCESS
      case _ => throw new Exception("Write denied")
    }
    res
  }
}