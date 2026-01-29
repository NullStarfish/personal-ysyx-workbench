
package mycpu.utils

import chisel3._
import chisel3.util._
import mycpu.core.os._ 
import scala.collection.mutable.{ArrayBuffer, LinkedHashMap}

trait HardwareAgent {
  val name: String
  val debugEnable: Boolean

  protected val managedSignals = LinkedHashMap[Data, (Data, Data)]()

  def agentPrint(fmt: String, data: Bits*): Unit = {
    if (debugEnable) {
      printf(s"[$name] " + fmt + "\n", data: _*)
    }
  }

  def driveManaged[T <: Data](target: T, idle: T, default: T): T = {
    val proxy = Wire(chiselTypeOf(target))
    managedSignals(proxy) = (idle, default) 
    
    if (this.isInstanceOf[HardwareThread]) {
        val t = this.asInstanceOf[HardwareThread]
        target := Mux(t.isRunning, proxy, idle)
    } else {
        target := proxy
    }
    proxy
  }

  def driveManaged[T <: Data](target: T, default: T): T = driveManaged(target, default, default)
}

class HardwareLogic(val name: String, val debugEnable: Boolean = true) extends HardwareAgent {
  def run(block: => Unit): Unit = {
    ContextScope.withContext(LogicCtx(this)) {
      managedSignals.foreach { case (proxy, (_, default)) => proxy := default }
      block
    }
  }
}

class HardwareThread(val name: String, val debugEnable: Boolean = true, val isMealy: Boolean = false) extends HardwareAgent {

  private val steps = ArrayBuffer[() => Unit]()
  private val stepNames = ArrayBuffer[String]()
  private val globals = ArrayBuffer[() => Unit]()

  private val activeReg = RegInit(false.B)
  
  // [修复] 提供一个预先定义的稳定 PC 信号
  // 即使 pcReg 还没创建，sys_read 也能引用这个 wire
  private val pcWire = WireInit(0.U(32.W))
  def pc: UInt = pcWire

  // 核心控制线网
  private val startWire  = WireInit(false.B)
  private val abortWire  = WireInit(false.B)
  private val pauseWire  = WireInit(false.B)
  
  // [修复] stepDone 必须定义在 class 作用域，以便全局访问
  private val stepDoneWire = WireInit(true.B) 

  private var _generated = false
  private var _isLooping = false
  private var _startCondSet = false

  def isRunning: Bool = if (isMealy) (activeReg || startWire) else activeReg
  def hasStartCondition: Boolean = _startCondSet

  def startWhen(cond: Bool): Unit = { startWire := cond; _startCondSet = true }
  def abortWhen(cond: Bool): Unit = { abortWire := cond }
  def pauseWhen(cond: Bool): Unit = { pauseWire := cond }
  
  def setStepNotDone(): Unit = { stepDoneWire := false.B }

  def entry(block: => Unit): Unit = {
    if (_generated) throw new Exception(s"Thread '$name' generated twice")
    _generated = true

    // 1. 设置 Context 并执行用户定义的 Steps
    // 此时内部 sys_read 会引用 pcWire
    ContextScope.withContext(ThreadCtx(this)) { block }

    val totalSteps = steps.length
    if (totalSteps == 0) return

    val width = log2Ceil(totalSteps + 1)
    val pcReg = RegInit(0.U(width.W))
    
    // [关键连接] 将物理寄存器连接到稳定的 pcWire
    pcWire := pcReg

    // 默认驱动管理信号
    managedSignals.foreach { case (proxy, (_, default)) => proxy := default }

    // 每一拍默认 stepDone 为 true，除非在 func() 中调用了 waitCondition
    stepDoneWire := true.B

    val active = isRunning 

    // Debug Log
    if (debugEnable) {
      val wasActive = RegNext(active)
      val lastPc    = RegNext(pcReg)
      when (!wasActive && active) { agentPrint("--- ONLINE ---") }
      val justStarted = active && !wasActive
      when ((active && pcReg =/= lastPc) || justStarted) {
        for ((name, idx) <- stepNames.zipWithIndex) {
          when (pcReg === idx.U) { agentPrint(s"EXEC [PC $idx] $name") }
        }
      }
    }

    // 状态机核心逻辑
    when (abortWire) {
      activeReg := false.B
      pcReg     := 0.U
    }
    .elsewhen (active) {
      // 执行当前 PC 对应的 Step 逻辑
      // 如果 Step 内部调用了 waitCondition，会驱动 stepDoneWire 变低
      stepDoneWire := true.B
      for ((func, idx) <- steps.zipWithIndex) {
        when (pcReg === idx.U) { func() }
      }

      if (debugEnable) {
        when (!pauseWire && !stepDoneWire) {
           // 由于 Chisel 打印字符串限制，打印 PC 即可定位
           printf(p"[$name] THREAD STALL AT PC=$pcReg\n")
        }
      }

      // 只有在没被外部暂停且内部逻辑允许（stepDone）时才步进
      when (!pauseWire && stepDoneWire) {
        pcReg := pcReg + 1.U
        activeReg := true.B
        
        when (pcReg >= (totalSteps - 1).U) {
          if (_isLooping) {
            pcReg := 0.U
          } else {
            activeReg := false.B
            pcReg     := 0.U
          }
        }
      } .otherwise {
        // Stall: 保持 PC
        pcReg := pcReg
      }
    }
    .otherwise {
       when (startWire) {
         activeReg := true.B
         pcReg     := 0.U
       }
    }

    globals.foreach(_())
  }

  def Loop(): Unit = { _isLooping = true }
  
  def Step(name: String)(block: => Unit): Unit = {
    stepNames += name
    steps += { () => 
        ContextScope.withContext(AtomicCtx(this)) { block }
      }
  }

  def waitCondition(cond: Bool): Unit = { 
    when(!cond) { 
      this.stepDoneWire := false.B
    } 
  }

  def Global(block: => Unit): Unit = { globals += { () => block } }
}