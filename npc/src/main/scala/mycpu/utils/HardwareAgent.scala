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

  def write[T <: Data](proxy: T, value: T): Unit = {
    if (!managedSignals.contains(proxy)) {
       throw new Exception(s"[$name] [Safety Error] Attempting to write to an unmanaged signal: $proxy.")
    }
    proxy := value
  }
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

  private var pcEntity: UInt = _
  
  private val activeReg = RegInit(false.B)
  
  // [关键修复] 将原本的 var Bool 改为 WireInit
  // 这样无论 entry 和 startWhen 的调用顺序如何，Chisel 都能通过 Wire 连接起来
  private val startWire = WireInit(false.B)
  private val abortWire = WireInit(false.B)
  private val pauseWire = WireInit(false.B)

  private var _startCondSet = false
  private var _generated = false
  private var _isLooping = false

  def hasStartCondition: Boolean = _startCondSet
  def isGenerated: Boolean = _generated

  private val sessionCycles = RegInit(0.U(32.W))
  private val sessionStalls = RegInit(0.U(32.W))

  def pc: UInt = if (pcEntity == null) 0.U else pcEntity

  // [关键修复] 使用 startWire
  def isRunning: Bool = if (isMealy) (activeReg || startWire) else activeReg

  // [关键修复] 驱动 Wire
  def startWhen(cond: Bool): Unit = { 
    startWire := cond
    _startCondSet = true 
  }
  def abortWhen(cond: Bool): Unit = { abortWire := cond }
  def pauseWhen(cond: Bool): Unit = { pauseWire := cond }

  def entry(block: => Unit): Unit = {
    if (_generated) throw new Exception(s"Thread '$name' generated twice")
    _generated = true

    ContextScope.withContext(ThreadCtx(this)) {
      block 
    }

    val totalSteps = steps.length
    if (totalSteps == 0) return

    val width = log2Ceil(totalSteps + 1)
    val pcReg = RegInit(0.U(width.W))
    pcEntity = pcReg

    managedSignals.foreach { case (proxy, (_, default)) => proxy := default }

    val active = isRunning 

    if (debugEnable) {
      val wasActive = RegNext(active)
      val lastPc    = RegNext(pcReg)
      
      when (!wasActive && active) {
        agentPrint("--- ONLINE ---")
        sessionCycles := 0.U; sessionStalls := 0.U
      }
      
      val justStarted = active && !wasActive
      when ((active && pcReg =/= lastPc) || justStarted) {
        for ((name, idx) <- stepNames.zipWithIndex) {
          when (pcReg === idx.U) { agentPrint(s"EXEC [PC $idx] $name") }
        }
      }
    }

    // [关键修复] 在逻辑中使用 Wire
    when (abortWire) {
      activeReg := false.B
      pcReg     := 0.U
    }
    .elsewhen (active) {
      sessionCycles := sessionCycles + 1.U
      
      when (!pauseWire) {
        pcReg := pcReg + 1.U
        activeReg := true.B
        
        when (pcReg >= (totalSteps - 1).U) {
          if (_isLooping) {
            activeReg := true.B
            pcReg     := 0.U
          } else {
            activeReg := false.B
            pcReg     := 0.U
          }
        }

        for ((func, idx) <- steps.zipWithIndex) {
          when (pcReg === idx.U) { func() }
        }
      } .otherwise {
        sessionStalls := sessionStalls + 1.U
      }
    }
    .otherwise {
       // [关键修复] 这里的 startWire 是 Wire，即使在生成这段电路后才连接 true.B 也没问题
       when (startWire) {
         activeReg := true.B
         pcReg     := 0.U
       }
    }
    
    globals.foreach(_())
  }

  def Exit(): Unit = {
    activeReg := false.B
    if (pcEntity != null) pcEntity := 0.U
  }
  
  def Loop(): Unit = { _isLooping = true }
  
  def Step(name: String)(block: => Unit): Unit = {
    stepNames += name
    steps += { () => 
        ContextScope.withContext(AtomicCtx(this)) {
          block
        }
      }
  }
  
  def Step(block: => Unit): Unit = Step(s"Step_${steps.length}")(block)

  def waitCondition(cond: Bool): Unit = { 
      when(!cond) { 
          if(pcEntity != null) pcEntity := pcEntity 
      } 
  }
  
  def Label: UInt = steps.length.U

  def Global(block: => Unit): Unit = { globals += { () => block } }

  def Par(blocks: (() => Unit)*): Unit = {
    val children = blocks.zipWithIndex.map { case (block, i) =>
      val t = new HardwareThread(s"${name}_fork_$i", debugEnable, isMealy = true)
      t.entry {
        ContextScope.withContext(ThreadCtx(t)) { block() }
      }
      t
    }

    this.Step(s"Par_Fork_${children.length}") {
      val childrenActive = VecInit(children.map(_.isRunning))
      val hasRun = RegInit(VecInit(Seq.fill(children.length)(false.B)))
      
      children.zipWithIndex.foreach { case (t, i) =>
        t.startWhen(!hasRun(i))
        when(t.isRunning) { hasRun(i) := true.B }
      }
      
      val allStarted = hasRun.asUInt.andR
      val noneRunning = !childrenActive.asUInt.orR
      
      when (allStarted && noneRunning) {
        hasRun.foreach(_ := false.B)
      } .otherwise {
        this.waitCondition(false.B)
      }
    }
  }
}