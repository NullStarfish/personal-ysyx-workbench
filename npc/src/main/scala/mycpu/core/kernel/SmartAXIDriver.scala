package mycpu.core.kernel

import chisel3._
import chisel3.util._
import mycpu.core.os._
import mycpu.utils._
import chisel3.util.experimental.BoringUtils

class SmartAXIDriver(bus: AXI4Bundle) extends ResourceHandle {
  override val name = "SmartAXI"

  // --- 1. 物理 IO 代理 (内部 DSL 使用的 Wire) ---
  // 这些 Wire 是我们在 read/write/ioctl 中实际操作的对象
  private val arP = Wire(chiselTypeOf(bus.ar))
  private val rP  = Wire(chiselTypeOf(bus.r))
  private val awP = Wire(chiselTypeOf(bus.aw))
  private val wP  = Wire(chiselTypeOf(bus.w))
  private val bP  = Wire(chiselTypeOf(bus.b))

  // --- [关键修复] ---
  // 给所有本地 Wire 赋默认值。这相当于 HardwareThread 中的 "Idle" 状态输出。
  // 当 Step 逻辑没有覆盖这些值时（即线程阻塞或未启动），这些默认值生效。
  arP.valid := false.B; arP.bits := 0.U.asTypeOf(arP.bits); arP.ready := DontCare
  rP.valid  := DontCare; rP.bits := DontCare; rP.ready := false.B
  awP.valid := false.B; awP.bits := 0.U.asTypeOf(awP.bits); awP.ready := DontCare
  wP.valid  := false.B; wP.bits  := 0.U.asTypeOf(wP.bits); wP.ready := DontCare
  bP.valid  := DontCare; bP.bits := DontCare; bP.ready := false.B

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

  // --- 3. 跨层级连接辅助函数 ---
  
  // 驱动远程信号：Local(Child) -> Remote(Parent)


  override def setup(agent: HardwareAgent): Unit = {
    // 这里我们手动建立连接，不再使用 agent.driveManaged
    // 逻辑：arP (本地逻辑驱动) -> BoringUtils -> bus (远程父模块)

    // --- AR Channel ---
    DriverUtils.driveRemote(arP.valid, bus.ar.valid)
    DriverUtils.driveRemote(arP.bits,  bus.ar.bits)
    
    val arReadyBridge = Wire(Bool())
    DriverUtils.readRemote(bus.ar.ready, arReadyBridge)
    arP.ready := arReadyBridge

    // --- R Channel ---
    DriverUtils.driveRemote(rP.ready, bus.r.ready)

    val rValidBridge = Wire(Bool())
    val rBitsBridge  = Wire(chiselTypeOf(bus.r.bits))
    DriverUtils.readRemote(bus.r.valid, rValidBridge)
    DriverUtils.readRemote(bus.r.bits,  rBitsBridge)
    rP.valid := rValidBridge
    rP.bits  := rBitsBridge

    // --- AW Channel ---
    DriverUtils.driveRemote(awP.valid, bus.aw.valid)
    DriverUtils.driveRemote(awP.bits,  bus.aw.bits)

    val awReadyBridge = Wire(Bool())
    DriverUtils.readRemote(bus.aw.ready, awReadyBridge)
    awP.ready := awReadyBridge

    // --- W Channel ---
    DriverUtils.driveRemote(wP.valid, bus.w.valid)
    DriverUtils.driveRemote(wP.bits,  bus.w.bits)

    val wReadyBridge = Wire(Bool())
    DriverUtils.readRemote(bus.w.ready, wReadyBridge)
    wP.ready := wReadyBridge

    // --- B Channel ---
    DriverUtils.driveRemote(bP.ready, bus.b.ready)

    val bValidBridge = Wire(Bool())
    val bBitsBridge  = Wire(chiselTypeOf(bus.b.bits))
    DriverUtils.readRemote(bus.b.valid, bValidBridge)
    DriverUtils.readRemote(bus.b.bits,  bBitsBridge)
    bP.valid := bValidBridge
    bP.bits  := bBitsBridge

    // --- 寄存器初始化 ---
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
          arP.valid      := true.B
          arP.bits.addr  := addr
          arP.bits.size  := size
          arP.bits.len   := currentLen
          arP.bits.burst := currentBurst
          
          when(arP.ready) { rState := 1.U }
        }
        when(rState === 1.U) {
          rP.ready := true.B
          when(rP.valid) {
            rawData := rP.bits.data
            rState := 2.U
          }
        }
        t.waitCondition(rState === 2.U)
        when(rState === 2.U) { rState := 0.U }
        DriverUtils.alignRead(addr, rawData, size, signed)

      case ThreadCtx(t) =>
        val outData = Reg(UInt(32.W))
        t.Step("AXI_Read_AR") {
          arP.valid      := true.B
          arP.bits.addr  := addr
          arP.bits.len   := currentLen
          arP.bits.size  := size
          arP.bits.burst := currentBurst
          t.waitCondition(arP.ready)
        }
        t.Step("AXI_Read_R") {
          rP.ready := true.B
          t.waitCondition(rP.valid)
          outData := rP.bits.data
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
          awP.valid      := !doneAW
          awP.bits.addr  := addr
          awP.bits.len   := currentLen
          
          wP.valid       := !doneW
          wP.bits.data   := wdata
          wP.bits.strb   := wstrb
          wP.bits.last   := true.B
          
          when (awP.ready) { doneAW := true.B }
          when (wP.ready)  { doneW  := true.B }

          val done = (doneAW && doneW) || (doneAW && wP.ready) || (awP.ready && wP.ready) || (awP.ready && doneW)
          when(done) { wState := 1.U }
        }
        when(wState === 1.U) {
          bP.ready := true.B
          when(bP.valid) { wState := 2.U }
        }
        t.waitCondition(wState === 2.U)
        when(wState === 2.U) { 
          wState := 0.U; doneAW := false.B; doneW := false.B 
        }
        0.U

      case ThreadCtx(t) =>
        t.Par(
          () => t.Step("AXI_AW") { 
            awP.valid     := true.B
            awP.bits.addr := addr
            awP.bits.len  := currentLen 
            t.waitCondition(awP.ready) 
          },
          () => t.Step("AXI_W")  { 
            wP.valid      := true.B
            wP.bits.data  := wdata
            wP.bits.strb  := wstrb
            wP.bits.last  := true.B 
            t.waitCondition(wP.ready) 
          }
        )
        t.Step("AXI_B") { bP.ready := true.B; t.waitCondition(bP.valid) }
        0.U
      case _ => unsupported("write")
    }
  }
}