package mycpu.core.kernel

import chisel3._
import chisel3.util._
import mycpu.common._
import mycpu.core.os._
import mycpu.utils._
import chisel3.util.experimental.BoringUtils
import mycpu.common.AXI_ID_WIDTH

class SmartAXIDriver(bus: AXI4Bundle) extends ResourceHandle {
  override val name = "SmartAXI"

  // --- 1. 物理 IO 代理 (内部 DSL 使用的 Wire) ---
  // 这些 Wire 是我们在 read/write/ioctl 中实际操作的对象
  private var arP_valid : Bool = _
  private var arP_bits  : AXI4BundleA = _
  private val arP_ready = bus.ar.ready

  private val rP_valid = bus.r.valid
  private var rP_ready : Bool  = _
  private val rP_bits  = bus.r.bits

  private var awP_valid : Bool = _
  private var awP_bits  : AXI4BundleA = _
  private val awP_ready = bus.aw.ready 
  
  private var wP_valid : Bool = _
  private var wP_bits  : AXI4BundleW = _
  private val wP_ready = bus.w.ready 

  private val bP_valid = bus.b.valid
  private var bP_ready : Bool  = _
  private val bP_bits  = bus.b.bits



  // --- 2. 状态寄存器 ---
  private var lenReg:   UInt = _
  private var burstReg: UInt = _
  private var lenAtomicOverride: UInt = _
  private var burstAtomicOverride: UInt = _

  private var rState: UInt = _
  private var wState: UInt = _ 
  private var rawData: UInt = _
  private var doneAW: Bool = _
  private var doneW:  Bool = _




  override def setup(agent: HardwareAgent): Unit = {

    arP_valid = agent.driveManaged(bus.ar.valid, false.B) 
    arP_bits  = agent.driveManaged(bus.ar.bits, 0.U.asTypeOf(new AXI4BundleA(AXI_ID_WIDTH, XLEN)))

    rP_ready = agent.driveManaged(bus.r.ready, false.B)
    
    
    awP_valid = agent.driveManaged(bus.aw.valid, false.B) 
    awP_bits  = agent.driveManaged(bus.aw.bits, 0.U.asTypeOf(new AXI4BundleA(AXI_ID_WIDTH, XLEN)))

    wP_valid = agent.driveManaged(bus.w.valid, false.B) 
    wP_bits  = agent.driveManaged(bus.w.bits, 0.U.asTypeOf(new AXI4BundleW(XLEN)))

    bP_ready = agent.driveManaged(bus.b.ready, false.B)


    // --- 寄存器初始化 ---``
    lenReg   = RegInit(0.U(8.W))
    burstReg = RegInit(1.U(2.W))
    lenAtomicOverride   = WireDefault(lenReg)
    burstAtomicOverride = WireDefault(burstReg)

    rState  = RegInit(0.U(2.W))
    wState  = RegInit(0.U(2.W))
    rawData = Reg(UInt(32.W))
    doneAW  = RegInit(false.B)
    doneW   = RegInit(false.B)
  }

  override def ioctl(cmd: UInt, arg: UInt): UInt = {
    ContextScope.current match {
      case AtomicCtx(_) =>
        when(cmd === IoctlCmd.AXI_SET_LEN)   { lenAtomicOverride := arg }
        when(cmd === IoctlCmd.AXI_SET_BURST) { burstAtomicOverride := arg }
        
      case ThreadCtx(t) =>
        t.Step("AXI_Config_Update") {
          when(cmd === IoctlCmd.AXI_SET_LEN)   { lenReg := arg }
          when(cmd === IoctlCmd.AXI_SET_BURST) { burstReg := arg }
        }
      case LogicCtx(_) => unsupported("ioctl")
    }
    0.U
  }

  override def read(addr: UInt, size: UInt, signed: Bool): UInt = {
    val ctx = ContextScope.current
    val currentLen   = if (ctx.isInstanceOf[AtomicCtx]) lenAtomicOverride else lenReg
    val currentBurst = if (ctx.isInstanceOf[AtomicCtx]) burstAtomicOverride else burstReg

    ctx match {
      case AtomicCtx(t) =>
        when(rState === 0.U) {
          arP_valid      := true.B
          arP_bits.addr  := addr
          arP_bits.size  := size
          arP_bits.len   := currentLen
          arP_bits.burst := currentBurst
          
          when(arP_ready) { rState := 1.U }
        }
        when(rState === 1.U) {
          rP_ready := true.B
          when(rP_valid) {
            rawData := rP_bits.data
            rState := 2.U
          }
        }
        t.waitCondition(rState === 2.U)
        when(rState === 2.U) { rState := 0.U }
        DriverUtils.alignRead(addr, rawData, size, signed)

      case ThreadCtx(t) =>
        val outData = Reg(UInt(32.W))
        t.Step("AXI_Read_AR") {
          arP_valid      := true.B
          arP_bits.addr  := addr
          arP_bits.len   := currentLen
          arP_bits.size  := size
          arP_bits.burst := currentBurst
          t.waitCondition(arP_ready)
        }
        t.Step("AXI_Read_R") {
          rP_ready := true.B
          t.waitCondition(rP_valid)
          outData := rP_bits.data
        }
        DriverUtils.alignRead(addr, outData, size, signed)

      case _ => unsupported("read")
    }
  }

  override def write(addr: UInt, data: UInt, size: UInt): UInt = {
    val ctx = ContextScope.current
    val currentLen = if (ctx.isInstanceOf[AtomicCtx]) lenAtomicOverride else lenReg
    val (wdata, wstrb) = DriverUtils.alignWrite(addr, data, size)

    ctx match {
      case AtomicCtx(t) =>
        when(wState === 0.U) {
          awP_valid      := !doneAW
          awP_bits.addr  := addr
          awP_bits.len   := currentLen
          
          wP_valid       := !doneW
          wP_bits.data   := wdata
          wP_bits.strb   := wstrb
          wP_bits.last   := true.B
          
          when (awP_ready) { doneAW := true.B }
          when (wP_ready)  { doneW  := true.B }

          val done = (doneAW && doneW) || (doneAW && wP_ready) || (awP_ready && wP_ready) || (awP_ready && doneW)
          when(done) { wState := 1.U }
        }
        when(wState === 1.U) {
          bP_ready := true.B
          when(bP_valid) { wState := 2.U }
        }
        t.waitCondition(wState === 2.U)
        when(wState === 2.U) { 
          wState := 0.U; doneAW := false.B; doneW := false.B 
        }
        0.U

      case ThreadCtx(t) =>
        t.Par(
          () => t.Step("AXI_AW") { 
            awP_valid     := true.B
            awP_bits.addr := addr
            awP_bits.len  := currentLen 
            t.waitCondition(awP_ready) 
          },
          () => t.Step("AXI_W")  { 
            wP_valid      := true.B
            wP_bits.data  := wdata
            wP_bits.strb  := wstrb
            wP_bits.last  := true.B 
            t.waitCondition(wP_ready) 
          }
        )
        t.Step("AXI_B") { bP_ready := true.B; t.waitCondition(bP_valid) }
        0.U
      case _ => unsupported("write")
    }
  }
}