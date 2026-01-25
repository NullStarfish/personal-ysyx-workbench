package mycpu.core.os

import chisel3._
import chisel3.util._
import mycpu.common._
import mycpu.utils._
import scala.collection.mutable.ArrayBuffer

// =========================================================
// 1. 上下文抽象 (Execution Context)
// =========================================================
// 驱动程序需要知道它是在 Thread (Step) 中被调用，还是在 Logic (Wire) 中被调用


// =========================================================
// 2. 资源句柄 (Resource Handle)
// =========================================================
abstract class ResourceHandle {

  def read(addr: UInt, size: UInt = 4.U, signed: Bool = false.B): UInt
    
  def write(addr: UInt, data: UInt, size: UInt = 4.U): UInt
}

// =========================================================
// 3. 进程基类 (HwProcess) - 现在的定义更像是一个“任务组”
// =========================================================
abstract class HwProcess[I <: Data, O <: Data](val pName: String) {
  // --- 内核注入区 ---
  var _stdin:  HwQueue[I] = _
  var _stdout: HwQueue[O] = _
  private[os] var _container: Module  = _ // 父模块引用

  // --- 内部任务列表 ---
  private val threads = ArrayBuffer[HardwareThread]()
  private val logics  = ArrayBuffer[HardwareLogic]()

  // --- API: 创建并发任务 (Forking Threads) ---
  
  // 创建一个状态机线程 (时序逻辑)
  protected def createThread(name: String = "MainThread"): HardwareThread = {
    val t = new HardwareThread(s"${pName}_$name")
    threads += t
    t
  }

  // 创建一个守护逻辑 (组合逻辑)
  protected def createLogic(name: String = "Daemon"): HardwareLogic = {
    val l = new HardwareLogic(s"${pName}_$name")
    logics += l
    l
  }

  // --- API: 孵化子进程 (Forking Processes) ---
  // 这实际上是实例化子模块并建立私有连接
  protected def sys_spawn[SubI <: Data, SubO <: Data](
    child: HwProcess[SubI, SubO], 
    input: DecoupledIO[SubI]
  )(implicit subOGen: SubO): DecoupledIO[SubO] = {
    // 在当前 Container 内部实例化子 Container
    val childContainer = Module(new ProcessContainer(child, input.bits, subOGen))
    childContainer.io.stdin <> input
    childContainer.io.stdout
  }

  // --- API: 资源申请 ---
  def sys_open(name: String): ResourceHandle = {
    Kernel.getDriverInstance(name)
  }

  // --- API: 管道操作 (Syscalls) ---
  
  // 1. 组合逻辑偷看 (Peek) - 零延迟，不消耗
  def sys_peek()(implicit ctx: ExecutionContext): (Bool, I) = {
    val (valid, bits) = ctx match {
      case LogicCtx(l) => 
        // Logic 直接看 Wire
        (_stdin.canPop, _stdin.peek())
      case ThreadCtx(t) =>
        // Thread 也看 Wire
        (_stdin.canPop, _stdin.peek())
    }
    (valid, bits)
  }

  // 2. 消耗数据 (Consume/Pop) - 必须在确认 valid 后调用
  def sys_consume()(implicit ctx: ExecutionContext): Unit = {
    ctx match {
      case LogicCtx(l) => 
        // Logic 中，我们需要驱动 ready 信号
        // 注意：HwQueue.tryPop() 是由 HardwareLogic.driveManaged 管理的
        // 这里简化为直接调用，假设 Logic 框架处理了 driveManaged
        val readyProxy = l.driveManaged(_stdin.deq.ready, false.B)
        l.write(readyProxy, true.B)
        
      case ThreadCtx(t) =>
        _stdin.tryPop() // Thread 内部状态机会处理一拍的 Pulse
    }
  }

  // 3. 阻塞式读取 (Read) - 仅限 Thread
  def sys_read()(implicit ctx: ExecutionContext): I = {
    ctx match {
      case ThreadCtx(t) =>
        val data = Reg(chiselTypeOf(_stdin.peek()))
        t.Step("Sys_Read") {
          t.waitCondition(_stdin.canPop)
          data := _stdin.peek()
          _stdin.tryPop()
        }
        data
      case _ => throw new Exception("sys_read is blocking, only allowed in Thread.")
    }
  }

  // 4. 写入输出 (Write)
  def sys_write(data: O)(implicit ctx: ExecutionContext): Unit = {
    ctx match {
      case ThreadCtx(t) =>
        t.Step("Sys_Write") {
          t.waitCondition(_stdout.canPush)
          _stdout.tryPush(data)
        }
      case LogicCtx(l) =>
        // Logic 需要组合逻辑握手
        val validProxy = l.driveManaged(_stdout.enq.valid, false.B)
        val bitsProxy  = l.driveManaged(_stdout.enq.bits,  DontCare)
        
        // 只有当 Logic 决定写的时候
        l.write(validProxy, true.B)
        l.write(bitsProxy, data)
        // 注意：Logic 写通常需要 check ready，否则下一拍可能丢数据
        // 这里假设 Logic 外部有保护
    }
  }

  // 用户入口
  def entry(): Unit
  

}

// =========================================================
// 4. 进程容器 (Container)
// =========================================================
class ProcessContainer[I <: Data, O <: Data](proc: HwProcess[I, O], iGen: I, oGen: O) extends Module {
  val io = IO(new Bundle {
    val stdin  = Flipped(Decoupled(iGen))
    val stdout = Decoupled(oGen)
  })

  // 实例化管道
  val inQ  = new HwQueue(iGen, 2, s"${proc.pName}_InQ")
  val outQ = new HwQueue(oGen, 2, s"${proc.pName}_OutQ")
  inQ.enq <> io.stdin
  io.stdout <> outQ.deq

  // 注入环境
  proc._stdin     = inQ
  proc._stdout    = outQ
  proc._container = this

  // 执行用户定义 (创建 threads/logics)
  proc.entry()
  

}

// =========================================================
// 5. 内核 (Kernel)
// =========================================================
object Kernel {
  type DriverFactory = () => ResourceHandle
  private val registry = scala.collection.mutable.HashMap[String, DriverFactory]()

  // 挂载驱动
  def mount(name: String, factory: DriverFactory): Unit = {
    registry(name) = factory
  }

  def getDriverInstance(name: String): ResourceHandle = {
    if (!registry.contains(name)) throw new Exception(s"Unknown resource: $name")
    registry(name)()
  }

  // 启动链
  class PipeChain[CurrOut <: Data](val lastOut: DecoupledIO[CurrOut]) {
    def | [NextOut <: Data](nextProc: HwProcess[CurrOut, NextOut])(implicit nGen: NextOut): PipeChain[NextOut] = {
      val container = Module(new ProcessContainer(nextProc, lastOut.bits, nGen))
      container.io.stdin <> lastOut
      new PipeChain(container.io.stdout)
    }
    def exit(): Unit = { lastOut.ready := true.B }
  }

  def boot[O <: Data](first: HwProcess[UInt, O])(implicit oGen: O): PipeChain[O] = {
    val container = Module(new ProcessContainer(first, UInt(0.W), oGen))
    container.io.stdin.valid := true.B; container.io.stdin.bits := 0.U
    new PipeChain(container.io.stdout)
  }
}