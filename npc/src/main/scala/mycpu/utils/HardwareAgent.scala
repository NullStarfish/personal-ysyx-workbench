package mycpu.utils
import chisel3._
import chisel3.util._
import mycpu.core.os._
import scala.collection.mutable.{ArrayBuffer, HashSet, LinkedHashMap}

trait HardwareAgent {
  val name: String
  // 记录所有受管信号及其默认值
  // Map: Proxy信号 -> (空闲时的值, 运行时的默认值)
  protected val managedSignals = LinkedHashMap[Data, (Data, Data)]()

  val debugEnable: Boolean // 实例级开关

  // 内部辅助打印工具
  def agentPrint(fmt: String, data: Bits*): Unit = {
    if (debugEnable) {
      // 这里的 printf 是硬件电路，只在仿真运行时输出
      printf(s"[$name] " + fmt + "\n", data: _*)
    }
  }

  /**
   * 统一的信号接管接口
   * @param target 外部 IO
   * @param idle 空闲状态（Logic永远运行，Thread未启动时）的值
   * @param default 运行状态下，每一拍开始时的默认值 (类似 WireDefault)
   */
  def driveManaged[T <: Data](target: T, idle: T, default: T): T = {
    val proxy = Wire(chiselTypeOf(target))
    managedSignals(proxy) = (idle, default)
    
    // 默认连接，子类会覆盖此逻辑（如 Thread 需要 Mux active）
    target := proxy 
    proxy
  }

  // 简化版：idle 和 default 相同
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
    // [修改] 使用 withContext 包裹用户代码
    ContextScope.withContext(LogicCtx(this)) {
      managedSignals.foreach { case (proxy, (_, default)) => proxy := default }
      block
    }
  }
}

class HardwareThread(val name: String, val debugEnable: Boolean = true) extends HardwareAgent {

  private val steps = ArrayBuffer[() => Unit]()
  private val stepNames = ArrayBuffer[String]() // [新增] 存储步骤名称
  private val globals = ArrayBuffer[() => Unit]()

  private var pcEntity: UInt = _

  private val active = RegInit(false.B)

  private var startSignal: Bool = false.B  // 触发启动
  private var abortSignal: Bool = false.B  // 强制复位 (Kill)
  private var pauseSignal: Bool = false.B  // 暂停执行 (Stall)

  private val sessionCycles = RegInit(0.U(32.W))
  private val sessionStalls = RegInit(0.U(32.W))

  def pc: UInt = {
    require(pcEntity != null, "Error: Cannot access 'thread.pc' outside of Step/Call/Global logic! Hardware not generated yet.")
    pcEntity
  }

  def startWhen(cond: Bool): Unit = { startSignal = cond }
  def abortWhen(cond: Bool): Unit = { abortSignal = cond }
  def pauseWhen(cond: Bool): Unit = { pauseSignal = cond }
  def isRunning: Bool = active

  override def driveManaged[T <: Data](target: T, idle: T, default: T): T = {
    val proxy = Wire(chiselTypeOf(target))
    managedSignals(proxy) = (idle, default)
    
    target := Mux(active, proxy, idle)
    proxy
  }

  def entry(block: => Unit): Unit = {

    ContextScope.withContext(ThreadCtx(this)) {
      block // 执行用户逻辑，此时 ContextScope.current 就是这个线程
    }

    val totalSteps = steps.length
    if (totalSteps == 0) return

    val width = if (totalSteps > 1) log2Ceil(totalSteps) else 1
    val pcReg = RegInit(0.U(width.W))
    pcEntity = pcReg

    // 默认值注入
    managedSignals.foreach { case (proxy, (_, default)) => proxy := default }

    // --- 调试与看门狗逻辑 ---
    if (debugEnable) {
      val wasActive = RegNext(active)
      val lastPc    = RegNext(pcReg)
      
      // 判断 PC 是否停滞 (WaitCondition 或 Pause 都会导致 PC 不变)
      val pcStuck   = active && (pcReg === lastPc)
      val hangCounter = RegInit(0.U(32.W))

      // A. 上线提醒
      when (!wasActive && active) {
        agentPrint("--- ONLINE ---")
        sessionCycles := 0.U
        sessionStalls := 0.U
        hangCounter   := 0.U
      }

      // B. 下线提醒
      when (wasActive && !active) {
        agentPrint("--- OFFLINE (Duration: %d, Stalls: %d) ---", sessionCycles, sessionStalls)
      }

      // C. 执行步骤追踪 (带名称)
      // 当 active 且 PC 发生改变时，打印新进入的 Step 名称
      when (active && pcReg =/= lastPc) {
        // 遍历查找当前 PC 对应的名称
        for ((name, idx) <- stepNames.zipWithIndex) {
          when (pcReg === idx.U) {
            // 注意：这里使用 Scala 插值把 name 编译进 Verilog 字符串
            agentPrint(s"EXEC [PC $idx] $name") 
          }
        }
      }

      // D. 死锁检测 (Watchdog)
      // 如果 active 且 PC 保持不变超过 1000 周期
      when (pcStuck) {
        hangCounter := hangCounter + 1.U
        when (hangCounter === 1000.U) {
           for ((name, idx) <- stepNames.zipWithIndex) {
             when (pcReg === idx.U) {
               agentPrint(s"!!! DEADLOCK WARNING !!! Stuck at Step '$name' (PC=$idx) for 1000+ cycles")
             }
           }
        }
      } .otherwise {
        hangCounter := 0.U
      }
    }

    // --- 状态机逻辑 ---
    when (abortSignal) {
      active := false.B
      pcReg  := 0.U
    } .elsewhen (active) {
      // 统计运行周期
      sessionCycles := sessionCycles + 1.U
      
      when (!pauseSignal) {
        // 默认行为：PC 自增
        pcReg := pcReg + 1.U
        
        // 边界检查：运行完最后一步自动退出
        when (pcReg >= (totalSteps - 1).U) {
          active := false.B
          pcReg  := 0.U
        }

        // 执行当前 Step 的逻辑
        for ((func, idx) <- steps.zipWithIndex) {
          when (pcReg === idx.U) { 
            func() 
            // 注意：func() 内部如果有 waitCondition，会生成覆盖 pcReg 的逻辑
            // Chisel 后写的赋值会覆盖先写的，所以 waitCondition 生效
          }
        }
      } .otherwise {
        sessionStalls := sessionStalls + 1.U
      }
    } .otherwise {
      when (startSignal) {
        active := true.B
        pcReg  := 0.U
      }
    }
    
    globals.foreach(_())
  }

  def Exit(): Unit = {
    active := false.B
    pcEntity := 0.U
  }
  
  def Loop(): Unit = {
    pcEntity := 0.U
  }
  
  // [修改] 支持命名的 Step
  def Step(name: String)(block: => Unit): Unit = {
    stepNames += name
    steps += { () => block }
  }

  // [修改] 兼容旧接口，自动生成名称
  def Step(block: => Unit): Unit = {
    Step(s"Step_${steps.length}")(block)
  }

  // 保持 PC 不变 (阻塞)
  def waitCondition(cond: Bool): Unit = { 
    when(!cond) { 
      pcEntity := pcEntity 
    } 
  }
  
  def Label: UInt = steps.length.U

  def Global(block: => Unit): Unit = {
    globals += { () => block }
  }

  def Call[T <: Data, R <: Data](func: HwFunction[T, R], input: T): R = {
    val resultWire = Wire(chiselTypeOf(func.ret))
    resultWire := DontCare 
    
    // Call 作为一个单独的隐式步骤
    val callStepName = s"Call_Func_${steps.length}"
    stepNames += callStepName

    steps += { () => 
      val latch = Reg(chiselTypeOf(func.ret))
      func.args   := input
      func.enable := true.B

      pcEntity := pcEntity 
      when (func.done) {
        latch := func.ret
        pcEntity := pcEntity + 1.U
      }
      resultWire := latch
    }
    resultWire
  }
}