package mycpu.core.drivers

import chisel3._
import chisel3.util._
import mycpu.common._
import mycpu.core.kernel._
import mycpu.utils._

class SmartAXIDriver(bus: AXI4Bundle) extends PhysicalDriver(
  DriverMeta("AXI_BUS", DriverTiming.Sequential, DriverTiming.Sequential)
) {
  val sIdle :: sWaitResp :: Nil = Enum(2)
  
  val rState = RegInit(sIdle)
  val rData  = Reg(UInt(32.W))
  
  val wState = RegInit(sIdle)
  val wDoneAW = RegInit(false.B)
  val wDoneW  = RegInit(false.B)

  private var p_ar_valid: Bool = _
  private var p_aw_valid: Bool = _
  private var p_w_valid:  Bool = _
  private var p_r_ready:  Bool = _
  private var p_b_ready:  Bool = _

  override def setup(agent: HardwareAgent): Unit = {
    p_ar_valid = agent.driveManaged(bus.ar.valid, false.B)
    p_aw_valid = agent.driveManaged(bus.aw.valid, false.B)
    p_w_valid  = agent.driveManaged(bus.w.valid,  false.B)
    p_r_ready  = agent.driveManaged(bus.r.ready,  false.B)
    p_b_ready  = agent.driveManaged(bus.b.ready,  false.B)
    
    bus.ar.bits := DontCare
    bus.aw.bits := DontCare
    bus.w.bits  := DontCare
    bus.ar.bits.len := 0.U; bus.ar.bits.burst := 1.U; bus.ar.bits.id := 0.U
    bus.aw.bits.len := 0.U; bus.aw.bits.burst := 1.U; bus.aw.bits.id := 0.U
    bus.w.bits.last := true.B
  }

  override def seqRead(addr: UInt, size: UInt): (UInt, UInt, Bool) = {
    val done = WireDefault(false.B)
    val err  = WireDefault(Errno.ESUCCESS)
    
    switch(rState) {
      is(sIdle) {
        p_ar_valid := true.B
        bus.ar.bits.addr := addr
        // [修复] size 是 2位，AXI 需要 3位。补0即可。
        bus.ar.bits.size := Cat(0.U(1.W), size) 
        when(bus.ar.ready) { rState := sWaitResp }
      }
      is(sWaitResp) {
        p_r_ready := true.B
        when(bus.r.valid) {
          rData  := bus.r.bits.data
          rState := sIdle
          done   := true.B
        }
      }
    }
    (rData, err, done)
  }

  override def seqWrite(addr: UInt, data: UInt, size: UInt): (UInt, Bool) = {
    val done = WireDefault(false.B)
    val err  = WireDefault(Errno.ESUCCESS)

    switch(wState) {
      is(sIdle) {
        p_aw_valid := !wDoneAW
        bus.aw.bits.addr := addr
        bus.aw.bits.size := Cat(0.U(1.W), size)
        
        p_w_valid := !wDoneW
        
        // [核心修复] 根据地址低位生成 WSTRB 和移位数据
        val offset = addr(1, 0)
        val shift  = Cat(offset, 0.U(3.W)) // offset * 8
        bus.w.bits.data := data(31, 0) << shift
        
        bus.w.bits.strb := MuxLookup(size, "b1111".U)(Seq(
          0.U -> "b0001".U, // Byte
          1.U -> "b0011".U, // Half
          2.U -> "b1111".U  // Word
        )) << offset
        
        when(bus.aw.ready) { wDoneAW := true.B }
        when(bus.w.ready)  { wDoneW  := true.B }
        
        when((wDoneAW || bus.aw.ready) && (wDoneW || bus.w.ready)) {
          wState := sWaitResp
          wDoneAW := false.B
          wDoneW  := false.B
        }
      }
      is(sWaitResp) {
        p_b_ready := true.B
        when(bus.b.valid) {
          wState := sIdle
          done   := true.B
        }
      }
    }
    (err, done)
  }
}