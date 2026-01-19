package mycpu.utils
import chisel3._
import chisel3.util._
import scala.collection.mutable.ArrayBuffer

class HardwareThread {
  // 1. 存储逻辑闭包
  private val steps = ArrayBuffer[() => Unit]()
  private val globals = ArrayBuffer[() => Unit]()
  
  // 2. 物理实体 (延迟创建)
  private var pcEntity: UInt = _
  private var isCombinational = false
  
  // 3. 供外部读取/写入 PC 的接口
  //    必须在 impl/entry 阶段调用，否则报错
  def pc: UInt = {
    require(pcEntity != null, "Error: Cannot access 'thread.pc' outside of Step/Call/Global logic! Hardware not generated yet.")
    pcEntity
  }

  // =================================================================
  // 入口函数
  // =================================================================
  def entry(block: => Unit): Unit = {
    // Phase 1: 收集 (Collection)
    // 执行用户的 block，把 Step/Call/Global 里的闭包收集起来
    block

    // Phase 2: 资源分配 (Allocation)
    val totalSteps = steps.length
    
    // 处理 Corner Case (0 或 1 步)
    if (totalSteps <= 1) {
      // 退化为 Wire，支持组合逻辑
      pcEntity = WireInit(0.U)
      isCombinational = true
      
      // 回放逻辑 (只执行第0步，如果有)
      if (totalSteps == 1) steps(0)()
      // 回放全局逻辑 (依然有效)
      globals.foreach(_())
      
    } else {
      // Normal Case: 创建寄存器
      val width = log2Ceil(totalSteps)
      // 我们创建一个 Reg，但类型标记为 UInt，方便统一处理
      val pcReg = RegInit(0.U(width.W))
      pcEntity = pcReg
      
      // Phase 3: 生成 Step 逻辑 (Generation)
      for ((stepFunc, idx) <- steps.zipWithIndex) {
        when (pcReg === idx.U) {
          // A. 默认行为：PC++
          pcReg := pcReg + 1.U
          
          // B. 用户 Step 行为 (可覆盖 A)
          stepFunc()
        }
      }
      
      // Phase 4: 生成 Global 逻辑 (Highest Priority)
      // C. 全局行为 (可覆盖 A 和 B)
      //    这在生成的 Verilog 中位于最下方，拥有最高优先级
      globals.foreach(_())

      // Phase 5: 循环回绕保护
      //    如果 Global 逻辑没有强制跳转，且 PC 超界，归零
      when (pcReg >= totalSteps.U) {
        pcReg := 0.U
      }
    }
  }

  def Step(block: => Unit): Unit = {
    steps += { () => block }
  }
  
  // 新增：全局逻辑
  // 这里的代码会在每个时钟周期执行，拥有最高修改 PC 的权限
  def Label: UInt = steps.length.U


  def Global(block: => Unit): Unit = {
    globals += { () => block }
  }

  def Call[T <: Data, R <: Data](func: HwFunction[T, R], input: T): R = {
    // 这里的 Wire 用于在生成前占位，将返回值传出去
    val resultWire = Wire(chiselTypeOf(func.ret))
    resultWire := DontCare 

    steps += { () => 
      // 在生成阶段创建 latch
      val latch = Reg(chiselTypeOf(func.ret))
      
      func.args   := input
      func.enable := true.B
      
      // 覆盖默认的 PC++，改为 Stall
      if (!isCombinational)
        pcEntity := pcEntity 
      
      when (func.done) {
        latch := func.ret
        // 完成后进位
        if (!isCombinational)
          pcEntity := pcEntity + 1.U
      }
      resultWire := latch
    }
    resultWire
  }
}