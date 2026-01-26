package mycpu.core.os

import chisel3._
import chisel3.util._
import mycpu.common._
import mycpu.utils._
import mycpu.core.kernel._
import scala.collection.mutable.ArrayBuffer



abstract class ResourceHandle {
  val name: String
  
  // 增加 setup 接口，默认不实现
  def setup(t: HardwareAgent): Unit = {}

  def unsupported(method: String): Nothing = {
    val ctx = ContextScope.current.getClass.getSimpleName
    throw new Exception(s"[$name] Error: Method '$method' is not supported in $ctx")
  }

  def read(addr: UInt, size: UInt = AccessSize.Word, signed: Bool = false.B): UInt
  def write(addr: UInt, data: UInt, size: UInt = AccessSize.Word): UInt
  def ioctl(cmd: UInt, arg: UInt): UInt
}


// =========================================================
// 3. 进程基类 (HwProcess) - 现在的定义更像是一个“任务组”
// =========================================================
abstract class HwProcess[I <: Data, O <: Data](val pName: String) {
  // --- 内核注入区 ---
  var _stdin:  HwQueue[I] = _
  var _stdout: HwQueue[O] = _
  var _container: Module  = _ // 父模块引用

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
  def sys_open(name: String)(implicit t: HardwareAgent): ResourceHandle = {
    val handle = Kernel.getDriverInstance(name)
    // 在获取句柄时立刻完成零件注册
    handle.setup(t)
    handle
  }

  // --- API: 管道操作 (Syscalls) ---
  
  def sys_peek(): (Bool, I) = ContextScope.current match {
    case LogicCtx(l) => 
      // Logic 直接看 Wire
      (_stdin.canPop, _stdin.peek())
    case ThreadCtx(t) =>
      // Thread 环境（Step 外部）：创建一个 Step 进行采样，并存入寄存器
      // 1. 分别创建 Bool 和 数据的寄存器（不能用 Tuple）
      val validReg = RegInit(false.B)
      val dataReg  = Reg(chiselTypeOf(_stdin.peek()))

      // 2. 注入采样步骤
      t.Step("Sys_Peek") {
        validReg := _stdin.canPop
        dataReg  := _stdin.peek()
      }
      
      // 3. 返回寄存器组成的 Scala Tuple
      (validReg, dataReg)
    case AtomicCtx(t) =>
      (_stdin.canPop, _stdin.peek())
  }

  

  // 2. 消耗数据 (Consume/Pop) - 必须在确认 valid 后调用
  def sys_consume(): Unit = {
    ContextScope.current match {
      case LogicCtx(l) => 
        val readyProxy = l.driveManaged(_stdin.deq.ready, false.B)
        l.write(readyProxy, true.B)
        
      case ThreadCtx(t) =>
        t.Step("sys_consume") {
          _stdin.tryPop()
        }

      case AtomicCtx(t) => 
        _stdin.tryPop()
    }
  }

  // 3. 阻塞式读取 (Read) - 仅限 Thread
  def sys_read(): I = ContextScope.current match {
    case ThreadCtx(t) =>
      val data = Reg(chiselTypeOf(_stdin.peek()))
      t.Step("Sys_Read_Sequential") {
        t.waitCondition(_stdin.canPop)
        data := _stdin.peek()
        _stdin.tryPop()
      }
      data

    case AtomicCtx(t) => 
      // 原子读取：如果不满足条件，当前 Step 直接 Stall
      t.waitCondition(_stdin.canPop)
      _stdin.tryPop() // 内部执行 _stdin.deq.ready := true.B
      _stdin.peek()   // 返回瞬时 Wire
        
    case LogicCtx(_) => 
      throw new Exception("sys_read is blocking, not allowed in Logic. Use sys_peek.")
  }

  // 4. 写入输出 (Write)
  def sys_write(data: O): Unit = ContextScope.current match {
    case ThreadCtx(t) =>
      t.Step("Sys_Write_Sequential") {
        t.waitCondition(_stdout.canPush)
        _stdout.tryPush(data)
      }

    case AtomicCtx(t) => 
      t.waitCondition(_stdout.canPush)
      _stdout.tryPush(data)

    case LogicCtx(l) =>
      when(_stdout.canPush) {
        _stdout.tryPush(data)
      }
  }

  // 用户入口
  def entry(): Unit
  

  private[os] def postBuild(): Unit = {
    threads.foreach { t =>
      if (!t.hasStartCondition) { 
        printf(p"Warning!!!! ${t.name} doesn't have a start condition!!!\n")
        t.startWhen(true.B)
      }
    }
  }
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

  proc.postBuild()
  

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