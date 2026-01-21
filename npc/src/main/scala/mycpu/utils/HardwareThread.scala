package mycpu.utils
import chisel3._
import chisel3.util._
import scala.collection.mutable.{ArrayBuffer, HashSet}


class HardwareThread(val name: String) {

  private val steps = ArrayBuffer[() => Unit]()
  private val globals = ArrayBuffer[() => Unit]()
  

  private var pcEntity: UInt = _



  private val active = RegInit(false.B)

    // --- 控制信号 (Lazy Binding) ---
  private var startSignal: Bool = false.B  // 触发启动
  private var abortSignal: Bool = false.B  // 强制复位 (Kill)
  private var pauseSignal: Bool = false.B  // 暂停执行 (Stall)


  private val ownedSignals = HashSet[Data]()


  def pc: UInt = {
    require(pcEntity != null, "Error: Cannot access 'thread.pc' outside of Step/Call/Global logic! Hardware not generated yet.")
    pcEntity
  }


  // 设置启动条件 (Spawn)
  def startWhen(cond: Bool): Unit = { startSignal = cond }
  
  // 设置强杀条件 (Kill -9)
  // 优先级最高，一旦满足，下一拍 PC 归零，active 变 false
  def abortWhen(cond: Bool): Unit = { abortSignal = cond }
  
  // 设置暂停条件 (Context Switch / Wait)
  // 满足时，保持 PC 不变，保持输出不变
  def pauseWhen(cond: Bool): Unit = { pauseSignal = cond }

  // 状态查询
  def isRunning: Bool = active



  def driveManaged[T <: Data](signal: T, idleValue: T, activeDefault: T): T = {
    // 只有当线程 Active 时，才使用 activeDefault 或线程内部的值
    // 否则回退到 idleValue
    val threadDriver = WireInit(activeDefault)

    ownedSignals.add(threadDriver)
    signal := Mux(active, threadDriver, idleValue)
    threadDriver
  }

  def write[T <: Data](target: T, value: T): Unit = {
    if (!ownedSignals.contains(target)) {
       throw new Exception(s"[Error] Thread '$name' trying to drive unmanaged signal: $target")
    }
    target := value
  }



  def entry(block: => Unit): Unit = {

    block
    val totalSteps = steps.length
    if (totalSteps == 0) return

    

    val width = if (totalSteps > 1) log2Ceil(totalSteps) else 1
    val pcReg = RegInit(0.U(width.W))
    pcEntity = pcReg
      


    when (abortSignal) {
      active := false.B
      pcReg  := 0.U
    }
    // 优先级 2: 正常运行逻辑
    .elsewhen (active) {
      // 如果没有暂停，则执行
      when (!pauseSignal) {
        // 默认 PC 自增 (Fetch Next Instruction)
        pcReg := pcReg + 1.U


        if (totalSteps > 0) {
          when (pcReg === (totalSteps - 1).U) {
             active := false.B
             pcReg  := 0.U
          }
        }
        
        // 执行当前 Step 的逻辑
        for ((func, idx) <- steps.zipWithIndex) {
          when (pcReg === idx.U) {
            func()
          }
        }
        
      }
    }
    // 优先级 3: 启动 (Spawn)
    .otherwise { // !active
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