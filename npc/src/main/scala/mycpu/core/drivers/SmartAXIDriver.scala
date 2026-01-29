package mycpu.core.drivers

import chisel3._
import chisel3.util._
import mycpu.common._
import mycpu.core.kernel._
import mycpu.utils._

class SmartAXIDriver(bus: AXI4Bundle) extends PhysicalDriver(
  DriverMeta("AXI_BUS", DriverTiming.Sequential, DriverTiming.Sequential)
) {
  val sIdle :: sAddr :: sData :: sResp :: Nil = Enum(4)
  
  val rState = RegInit(sIdle)
  val rData  = Reg(UInt(32.W))
  
  val wState = RegInit(sIdle)
  val wDoneAW = RegInit(false.B)
  val wDoneW  = RegInit(false.B)

  // === Proxies ===
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
    bus.ar.bits.id := 0.U
    bus.aw.bits := DontCare
    bus.aw.bits.id := 0.U
    bus.w.bits  := DontCare
  }

  // ----------------------------------------------------------------------------
  // 时序读
  // ----------------------------------------------------------------------------
  override def seqRead(addr: UInt, size: UInt): (UInt, UInt, Bool) = {
    val done = WireDefault(false.B)
    val err  = WireDefault(Errno.ESUCCESS)
    val retData = WireDefault(rData) 
    
    switch(rState) {
      is(sIdle) {
        p_ar_valid        := true.B
        bus.ar.bits.addr  := addr
        bus.ar.bits.size  := size
        bus.ar.bits.len   := 0.U
        bus.ar.bits.burst := AXI4Parameters.BURST_INCR
        
        when(bus.ar.ready) { rState := sData }
      }
      is(sData) {
        p_r_ready := true.B
        when(bus.r.valid) {
          rData   := bus.r.bits.data
          // Bypass return
          retData := bus.r.bits.data 
          rState  := sIdle 
          done    := true.B
        }
      }
    }
    
    (retData, err, done)
  }

  // ----------------------------------------------------------------------------
  // 时序写
  // ----------------------------------------------------------------------------
  override def seqWrite(addr: UInt, data: UInt, size: UInt): (UInt, Bool) = {
    val done = WireDefault(false.B)
    val err  = WireDefault(Errno.ESUCCESS)

    switch(wState) {
      is(sIdle) {
        p_aw_valid        := !wDoneAW
        bus.aw.bits.addr  := addr
        bus.aw.bits.size  := size
        bus.aw.bits.len   := 0.U
        
        p_w_valid         := !wDoneW
        
        val shift = addr(1,0) ## 0.U(3.W)
        // [修复] 显式截断 data 到 32位，因为 AXI 总线是 32 位宽
        bus.w.bits.data := data(31,0) << shift
        
        val baseMask = MuxLookup(size, "b1111".U)(Seq(
          0.U -> "b0001".U,
          1.U -> "b0011".U,
          2.U -> "b1111".U
        ))
        bus.w.bits.strb := baseMask << addr(1,0)
        bus.w.bits.last   := true.B
        
        when(bus.aw.ready) { wDoneAW := true.B }
        when(bus.w.ready)  { wDoneW  := true.B }
        
        when((wDoneAW || bus.aw.ready) && (wDoneW || bus.w.ready)) {
          wState := sResp
          wDoneAW := false.B
          wDoneW  := false.B
        }
      }
      is(sResp) {
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