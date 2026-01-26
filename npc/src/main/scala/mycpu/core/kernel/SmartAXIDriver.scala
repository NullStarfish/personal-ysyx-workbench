package mycpu.core.kernel

import chisel3._
import chisel3.util._
import mycpu.core.os._
import mycpu.utils._

class SmartAXIDriver(bus: AXI4Bundle) extends ResourceHandle {
  override val name = "SmartAXI"

  // --- 1. 物理 IO 代理 ---
  private var arP: DecoupledIO[AXI4BundleA] = _
  private var rP:  DecoupledIO[AXI4BundleR] = _
  private var awP: DecoupledIO[AXI4BundleA] = _
  private var wP:  DecoupledIO[AXI4BundleW] = _
  private var bP:  DecoupledIO[AXI4BundleB] = _

  // --- 2. 静态配置寄存器与瞬时 Wire ---
  private var lenReg:   UInt = _
  private var burstReg: UInt = _
  private var lenAtomicOverride: UInt = _
  private var burstAtomicOverride: UInt = _

  // --- 3. 静态状态机寄存器 ---
  private var rState: UInt = _
  private var wState: UInt = _ // 0:Idle, 1:AW_W, 2:B_Resp
  private var rawData: UInt = _
  private var doneAW: Bool = _
  private var doneW:  Bool = _




  override def setup(t: HardwareThread): Unit = {
    // 代理所有通道
    arP = t.driveManaged(bus.ar, 0.U.asTypeOf(bus.ar))
    rP  = t.driveManaged(bus.r,  0.U.asTypeOf(bus.r))
    awP = t.driveManaged(bus.aw, 0.U.asTypeOf(bus.aw))
    wP  = t.driveManaged(bus.w,  0.U.asTypeOf(bus.w))
    bP  = t.driveManaged(bus.b,  0.U.asTypeOf(bus.b))
    //必须在收集器就完成注册

    // 配置寄存器
    lenReg   = RegInit(0.U(8.W))
    burstReg = RegInit(1.U(2.W)) // 默认 INCR

    // 瞬时覆盖线：默认为寄存器的值
    // 在 AtomicCtx 下，ioctl 会改写这些 Wire
    lenAtomicOverride   = WireDefault(lenReg)
    burstAtomicOverride = WireDefault(burstReg)

    // 状态寄存器
    rState  = RegInit(0.U(2.W))
    wState  = RegInit(0.U(2.W))
    rawData = Reg(UInt(32.W))
    doneAW  = RegInit(false.B)
    doneW   = RegInit(false.B)
  }

  override def ioctl(cmd: UInt, arg: UInt): UInt = {
    ContextScope.current match {
      case AtomicCtx(_) =>
        // 原子模式：改写瞬时 Wire，影响当前 Step 的后续 AXI 动作
        when(cmd === IoctlCmd.AXI_SET_LEN)   { lenAtomicOverride := arg }
        when(cmd === IoctlCmd.AXI_SET_BURST) { burstAtomicOverride := arg }
        
      case ThreadCtx(t) =>
        // 线程模式：生成一个 Step 修改寄存器，永久生效
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
    //这里，加入多次调用，会生成多个这样的Wire，也就是组合逻辑：包括一个Len的连线，一个burt的连线，


    //注意，ContextScope是收集期object，是真正单例
    //DriverUtil中的函数：可以多次生成组合逻辑

    ctx match {
      case AtomicCtx(t) =>
        // 原子模式：Stall 当前 Step 直到完成
        when(rState === 0.U) {
          t.write(arP.valid, true.B)
          t.write(arP.bits.addr, addr)
          t.write(arP.bits.size, size)
          t.write(arP.bits.len, currentLen)
          t.write(arP.bits.burst, currentBurst)
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
        // 线程模式：多步流水线
        val outData = Reg(UInt(32.W)) //这是需要的，因为每次调用都需要latch
        t.Step("AXI_Read_AR") {
          arP.valid := true.B
          arP.bits.addr := addr
          arP.bits.len  := currentLen
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
          awP.valid := !doneAW
          awP.bits.addr := addr
          awP.bits.len  := currentLen
          
          wP.valid := !doneW
          wP.bits.data := wdata
          wP.bits.strb := wstrb
          wP.bits.last := true.B
          
          when (awP.ready) {doneAW := true.B}
          when (wP.ready)  {doneW  := true.B}

          val done = (doneAW && doneW) || (doneAW && wP.ready) || (awP.ready && wP.ready)
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
          () => t.Step("AXI_AW") { awP.valid := true.B; awP.bits.addr := addr; t.waitCondition(awP.ready) },
          () => t.Step("AXI_W")  { wP.valid := true.B; wP.bits.data := wdata; t.waitCondition(wP.ready) }
        )
        t.Step("AXI_B") { bP.ready := true.B; t.waitCondition(bP.valid) }
        0.U
      case _ => unsupported("write")
    }
  }
}