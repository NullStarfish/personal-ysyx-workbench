package mycpu.utils

import chisel3._
import chisel3.util._
import mycpu.core.os._ // 确保能引用 ContextScope
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
    target := proxy 
    proxy
  }

  def driveManaged[T <: Data](target: T, default: T): T = driveManaged(target, default, default)

  def write[T <: Data](target: T, value: T): Unit = {
    if (!managedSignals.contains(target)) {
       throw new Exception(s"[$name] Error: Driving unmanaged signal: $target")
    }
    target := value
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

// [新增] isMealy 参数：true=零延迟启动 (立即响应 startWhen)
class HardwareThread(val name: String, val debugEnable: Boolean = true, val isMealy: Boolean = false) extends HardwareAgent {

  private val steps = ArrayBuffer[() => Unit]()
  private val stepNames = ArrayBuffer[String]()
  private val globals = ArrayBuffer[() => Unit]()

  private var pcEntity: UInt = _
  
  // 状态机核心寄存器
  private val activeReg = RegInit(false.B)
  
  // 控制信号
  private var startSignal: Bool = false.B
  private var abortSignal: Bool = false.B
  private var pauseSignal: Bool = false.B

  // 状态标记 (供外部检查)
  private var _startCondSet = false
  private var _generated = false

  def hasStartCondition: Boolean = _startCondSet
  def isGenerated: Boolean = _generated

  // 性能计数
  private val sessionCycles = RegInit(0.U(32.W))
  private val sessionStalls = RegInit(0.U(32.W))

  def pc: UInt = {
    require(pcEntity != null, "Error: Accessing pc before generation.")
    pcEntity
  }

  // [关键修改] active 信号逻辑
  // 如果是 Mealy 模式，startSignal 会直接旁路到 active
  // 注意：这可能产生组合逻辑环，如果 startSignal 依赖于 thread 的输出
  def isRunning: Bool = if (isMealy) (activeReg || startSignal) else activeReg

  def startWhen(cond: Bool): Unit = { 
    startSignal = cond
    _startCondSet = true 
  }
  def abortWhen(cond: Bool): Unit = { abortSignal = cond }
  def pauseWhen(cond: Bool): Unit = { pauseSignal = cond }

  override def driveManaged[T <: Data](target: T, idle: T, default: T): T = {
    val proxy = Wire(chiselTypeOf(target))
    managedSignals(proxy) = (idle, default)
    
    target := Mux(isRunning, proxy, idle)
    proxy
  }

  def entry(block: => Unit): Unit = {
    if (_generated) throw new Exception(s"Thread '$name' generated twice")
    _generated = true

    ContextScope.withContext(ThreadCtx(this)) {
      block 
    }

    val totalSteps = steps.length
    if (totalSteps == 0) return

    val width = if (totalSteps > 1) log2Ceil(totalSteps) else 1
    val pcReg = RegInit(0.U(width.W))
    pcEntity = pcReg

    // 默认值注入
    managedSignals.foreach { case (proxy, (_, default)) => proxy := default }

    val active = isRunning // 使用计算后的 active

    // --- 调试与看门狗逻辑 ---
    if (debugEnable) {
      val wasActive = RegNext(active)
      val lastPc    = RegNext(pcReg)
      val pcStuck   = active && (pcReg === lastPc)
      val hangCounter = RegInit(0.U(32.W))

      when (!wasActive && active) {
        agentPrint("--- ONLINE ---")
        sessionCycles := 0.U; sessionStalls := 0.U; hangCounter := 0.U
      }
      when (wasActive && !active) {
        agentPrint("--- OFFLINE (Cycles: %d) ---", sessionCycles)
      }
      
      // 仅在 PC 变化或刚启动时打印 Step
      // 对于 Mealy 模式，启动当拍就会执行 Step 0
      val justStarted = active && !wasActive
      when ((active && pcReg =/= lastPc) || justStarted) {
        for ((name, idx) <- stepNames.zipWithIndex) {
          when (pcReg === idx.U) { agentPrint(s"EXEC [PC $idx] $name") }
        }
      }

      when (pcStuck) {
        hangCounter := hangCounter + 1.U
        when (hangCounter === 1000.U) {
           for ((name, idx) <- stepNames.zipWithIndex) {
             when (pcReg === idx.U) { agentPrint(s"!!! DEADLOCK WARNING [PC=$idx] $name !!!") }
           }
        }
      } .otherwise { hangCounter := 0.U }
    }

    // --- 状态机核心逻辑 ---
    
    // 优先级 1: Abort (Kill)
    when (abortSignal) {
      activeReg := false.B
      pcReg     := 0.U
    }
    // 优先级 2: 正常运行 (Active)
    .elsewhen (active) {
      sessionCycles := sessionCycles + 1.U
      
      when (!pauseSignal) {
        // 默认自增
        pcReg := pcReg + 1.U
        activeReg := true.B
        when (pcReg >= (totalSteps - 1).U) {
          activeReg := false.B
          pcReg     := 0.U
        }

        for ((func, idx) <- steps.zipWithIndex) {
          when (pcReg === idx.U) { func() }
        }
      } .otherwise {
        sessionStalls := sessionStalls + 1.U
      }
    }
    // 优先级 3: 启动 (Spawn)
    // 仅针对非 Mealy 模式，或者是 Mealy 模式下的状态维持
    // 如果是 Mealy 模式，active 已经是 (activeReg || startSignal) 了，所以上面的 .elsewhen (active) 已经覆盖了启动当拍的逻辑
    // 这里处理的是：如果当前 !activeReg 且 startSignal来了，下一拍 activeReg 要变 1
    .otherwise {
       when (startSignal) {
         activeReg := true.B
         pcReg     := 0.U
       }
    }
    
    globals.foreach(_())
  }

  // --- DSL 接口 ---

  def Exit(): Unit = {
    activeReg := false.B
    pcEntity  := 0.U
  }
  
  def Loop(): Unit = { pcEntity := 0.U }
  
  def Step(name: String)(block: => Unit): Unit = {
    stepNames += name
    steps += { () => 
        ContextScope.withContext(AtomicCtx(this)) {
          block
        }
      }
  }
  
  def Step(block: => Unit): Unit = Step(s"Step_${steps.length}")(block)

  def waitCondition(cond: Bool): Unit = { when(!cond) { pcEntity := pcEntity } }
  
  def Label: UInt = steps.length.U

  def Global(block: => Unit): Unit = { globals += { () => block } }

  // [新增] Par: 轻量级并行
  def Par(blocks: (() => Unit)*): Unit = {
    // 1. 创建子线程 (强制使用 Mealy 模式以减少延迟)
    val children = blocks.zipWithIndex.map { case (block, i) =>
      val t = new HardwareThread(s"${name}_fork_$i", debugEnable, isMealy = true)
      t.entry {
        ContextScope.withContext(ThreadCtx(t)) { block() }
      }
      t
    }

    // 2. 当前线程 Step：管理 Fork-Join
    this.Step(s"Par_Fork_${children.length}") {
      val childrenActive = VecInit(children.map(_.isRunning))
      
      // 记录哪些子线程已经启动过了 (防止重复启动)
      // 使用 Reg 记录状态，一旦启动过就置位
      val hasRun = RegInit(VecInit(Seq.fill(children.length)(false.B)))
      
      // 当所有子线程都【已经启动过】且【当前都不再运行】时，才算完成
      // 注意：Mealy 子线程启动当拍 isRunning=true，且 hasRun 下一拍才变 true
      
      children.zipWithIndex.foreach { case (t, i) =>
        // 启动条件：还没运行过
        t.startWhen(!hasRun(i))
        
        // 记录运行状态：只要监测到 running，就标记为已运行
        when(t.isRunning) { hasRun(i) := true.B }
      }
      
      val allStarted = hasRun.asUInt.andR
      val noneRunning = !childrenActive.asUInt.orR
      
      // 只有当所有子线程都跑完后，父线程才继续
      // Corner case: 如果子线程只有 1 步 (Mealy)，它在启动当拍就做完了，下一拍 isRunning=false
      // 所以判断逻辑是：(allStarted || 当前所有都在跑) && (当前没在跑 || 刚启动) 
      // 简化逻辑：父线程必须等到所有子线程 inactive
      
      when (allStarted && noneRunning) {
        // 完成，重置状态供下次使用
        hasRun.foreach(_ := false.B)
        // PC 自动 +1
      } .otherwise {
        this.waitCondition(false.B) // 阻塞
      }
    }
  }
}