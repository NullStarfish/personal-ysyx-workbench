package ysyx
import chisel3._
import chisel3.util._
import scala.collection.mutable.{ArrayBuffer, HashSet, LinkedHashMap}


trait HardwareAgent {
  val name: String
  // 记录所有受管信号及其默认值
  // Map: Proxy信号 -> (空闲时的值, 运行时的默认值)
  protected val managedSignals = LinkedHashMap[Data, (Data, Data)]()

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




class HardwareLogic(val name: String) extends HardwareAgent {
  def run(block: => Unit): Unit = {
    // 1. 每一拍开始，先给所有代理信号赋默认值 (WireDefault 效果)
    managedSignals.foreach { case (proxy, (_, default)) => proxy := default }
    
    // 2. 执行逻辑块
    block
  }
}




class HardwareThread(val name: String) extends HardwareAgent{

  private val steps = ArrayBuffer[() => Unit]()
  private val globals = ArrayBuffer[() => Unit]()
  

  private var pcEntity: UInt = _


  private val active = RegInit(false.B)

  private var startSignal: Bool = false.B  // 触发启动
  private var abortSignal: Bool = false.B  // 强制复位 (Kill)
  private var pauseSignal: Bool = false.B  // 暂停执行 (Stall)




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
    block
    val totalSteps = steps.length
    if (totalSteps == 0) return

    val width = if (totalSteps > 1) log2Ceil(totalSteps) else 1
    val pcReg = RegInit(0.U(width.W))
    pcEntity = pcReg

    managedSignals.foreach { case (proxy, (_, default)) => proxy := default }


    when (abortSignal) {
      active := false.B
      pcReg  := 0.U
    } .elsewhen (active) {
      when (!pauseSignal) {
        pcReg := pcReg + 1.U
        when (pcReg >= (totalSteps - 1).U) {
          active := false.B
          pcReg  := 0.U
        }
        for ((func, idx) <- steps.zipWithIndex) {
          when (pcReg === idx.U) { func() }
        }
      }
    } .otherwise {
      when (startSignal) {
        active := true.B
        pcReg  := 0.U
        // 注意：如果你希望在 start 这一拍就有输出，
        // 可以在这里显式调用 steps(0)()，但通常硬件习惯是下一拍生效
      }
    }
    globals.foreach(_())
  }


  def Exit(): Unit = {
    active := false.B
    pcEntity := 0.U
  }
  
  // 新增：循环指令
  def Loop(): Unit = {
    pcEntity := 0.U
  }
  

  def Step(block: => Unit): Unit = {
    steps += { () => block }
  }
  
  def Label: UInt = steps.length.U


  def Global(block: => Unit): Unit = {
    globals += { () => block }
  }

  def Call[T <: Data, R <: Data](func: HwFunction[T, R], input: T): R = {

    val resultWire = Wire(chiselTypeOf(func.ret))
    resultWire := DontCare 

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