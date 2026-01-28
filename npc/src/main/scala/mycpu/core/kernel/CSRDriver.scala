package mycpu.core.kernel

import chisel3._
import chisel3.util._
import mycpu.core.os._
import mycpu.utils._
import chisel3.util.experimental.BoringUtils
class CSRDriver(physRegs: Vec[UInt]) extends ResourceHandle {

  val readOnlyRegs = BoringUtils.bore(physRegs)

  // 定义本地驱动信号 (Source)
  private val remoteWen   = WireDefault(false.B)
  private val remoteAddr  = WireDefault(0.U(12.W))
  private val remoteData  = WireDefault(0.U(32.W))

  // 建立“插头” (Source) - 必须在类初始化时完成
  // 注意：这里不需要在 setup 里做，因为 ResourceHandle 初始化时就在 Module 上下文中
  BoringUtils.addSource(remoteWen,   "CSR_Update_En")
  BoringUtils.addSource(remoteAddr,  "CSR_Update_Addr")
  BoringUtils.addSource(remoteData,  "CSR_Update_Data")

  override val name = "CSR"

  
  private var opMode: UInt = _
  private var atomicOpOverride: UInt = _

  override def setup(t: HardwareAgent): Unit = {
    // 默认 RW 模式
    opMode = RegInit(IoctlCmd.CSR_RW)
    // 瞬时覆盖线
    atomicOpOverride = WireDefault(opMode)
  }

  override def ioctl(cmd: UInt, arg: UInt): UInt = {
    ContextScope.current match {
      case AtomicCtx(_) => 
        atomicOpOverride := cmd
      case ThreadCtx(t) => 
        t.Step("CSR_SetMode") { opMode := cmd }
      case LogicCtx(_) => unsupported("ioctl")
    }
    0.U
  }

  override def read(addr: UInt, size: UInt, signed: Bool): UInt = {
    // CSR 读通常是组合逻辑连线
    Mux(addr < readOnlyRegs.length.U, readOnlyRegs(addr), 0.U)
  }

  override def write(addr: UInt, data: UInt, size: UInt): UInt = {
    val ctx = ContextScope.current
    val currentMode = if (ctx.isInstanceOf[AtomicCtx]) atomicOpOverride else opMode
    
    val currentVal = Mux(addr < readOnlyRegs.length.U, readOnlyRegs(addr), 0.U)
    val resOldValue = Reg(UInt(32.W)) // 用于返回

    val newVal = MuxLookup(currentMode, currentVal)(Seq(
      IoctlCmd.CSR_RW -> data,
      IoctlCmd.CSR_RS -> (currentVal | data),
      IoctlCmd.CSR_RC -> (currentVal & ~data)
    ))

    def doWrite(): Unit = {
      resOldValue := currentVal
      when(addr < readOnlyRegs.length.U) { 
        remoteWen  := true.B
        remoteAddr := addr
        remoteData := newVal
      }
    }

    ctx match {
      case AtomicCtx(_) => doWrite() // 当前 Step 直接发生
      case ThreadCtx(t) => t.Step("CSR_Atomic_Write") { doWrite() }
      case LogicCtx(_)  => doWrite() // 组合逻辑写（慎用）
    }
    resOldValue
  }
}