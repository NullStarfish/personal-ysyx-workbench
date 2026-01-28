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

  def read(addr: UInt, size: UInt = AccessSize.Word, signed: Bool = false.B): UInt = unsupported("read")
  def write(addr: UInt, data: UInt, size: UInt = AccessSize.Word): UInt = unsupported("write")
  def ioctl(cmd: UInt, arg: UInt): UInt = unsupported("ioctl")
}


// =========================================================
// 3. 进程基类 (HwProcess) - 现在的定义更像是一个“任务组”
// =========================================================
abstract class HwProcess (val pName: String) {
  // --- 内核注入区 ---

  private val threads = ArrayBuffer[HardwareThread]()
  private val logics  = ArrayBuffer[HardwareLogic]()

  
  protected def createThread(name: String = "MainThread"): HardwareThread = {
    val t = new HardwareThread(s"${pName}_$name")
    threads += t
    t
  }

  protected def createLogic(name: String = "Daemon"): HardwareLogic = {
    val l = new HardwareLogic(s"${pName}_$name")
    logics += l
    l
  }

  //命名空间，先到自己的命名空间寻找，如果找不到，就去找kernel
  //sys_open就是去找handle


  //如果driver全是基于Hardware agent写的怎么办
  //这个setup必须要被执行，而且所谓的驱动，全部都是使用Agent写的

  //关键在于，这个driver到底要不要放多份，来让所有Process访问到，还是每个Process维护一份handle。

  //如果按照我们说的handle只有一份，那么，谁来执行setup？谁来执行read和write？？

  //对于sys_write，我们找到一个handle，由于ContextScope的存在，我们直接能找到调用它的thread或者logic
  //只要解决setup就行了：setup必须要绑定一个thread吗？真的吗



  //我们发现，现在的Driver完全是在Process的Thread中注入程序
  //可以让Driver也是一个程序吗？

  //这样Driver就没有所有权了：
  

  //大错特错！！！
  //我们需要一个Super Process，这才是我们的Kernel。这个所有Driver程序都由这个kernel程序代理
  //让kernel来执行所有的read， write以及 ioctl
  //子程序只有权力执行sys_read, sys_write和sys_ioctl


  //那么现在子程序的交互应该是什么样子的？
  //对于子程序的sys_open: 向kernel发送请求，请求得到Virtual handle。
  //所有的sys_open的硬件名称空间都由kernel维护。要么就不要有stdout和stdin了？不行。不然无法流水线

  //这个Virtual handle只能向kernel发送请求，且其handle形式应该为：sys_write, sys_read, sys_ioctl
  //kernel来决定是否向实际硬件handle发送请求：包括最简单的方法：维护一张资源锁表。

  //对于kernel:实际上，sys_open, sys_read, sys_ioctl这些方法应该都定义在Kernel里面，我们利用scala的闭包，捉取子程序中的数据到kernel来处理
  //那么子程序该怎么调用kernel中的这些方法？请AI来实现吧，

  //那么kernel应该是什么？首先kernel应该有很多线程：
  //首先是一个监听调度线程：sys_open, sys_read, sys_ioctl实际上是发送一个信号，让这个监听线程启动，来处理这个事件，然后再通过函数返回值，返回一个Virtual Resource Handle给子进程
  
  //还有kernel应该有能力fork：或者说，所有进程都应该有能力fork，而kernel需要将客户的process加载起来


  def sys_open(name: String)(implicit t: HardwareAgent): ResourceHandle = {
    val handle = Kernel.getDriverInstance(name)
    // 在获取句柄时立刻完成零件注册
    handle.setup(t)
    handle
  }

  def entry(): Unit
  
  private[os] def postBuild(): Unit = {
    threads.foreach { t =>
      if (!t.hasStartCondition) { 
        //printf(p"Warning!!!! ${t.name} doesn't have a start condition!!!\n")
        t.startWhen(true.B)
      }
    }
  }
}

// stdin和stdout需要在常见的时候指定
// ResourceHandle强绑定资源，每个资源一个Handle
//这里的stdin和stdout以及stderr只是一个引用
class Process(proc: HwProcess, stdin: ResourceHandle, stdout: ResourceHandle, stderr: ResourceHandle)  {


  

  proc.entry()

  proc.postBuild()
  

}

/* . sys_open这个方法返回一个VirtualResourceHandle，但是这个handle不应该是由client来new的。sys_open应该请求kernel，是kernel来new一个VirtualResourceHandle，然后sys_open仅仅只是返回这个VirtualResourceHandle的引用。

具体实现：元编程
参考HardwareAgent的实现：我们有一个收集期。假如用户进程在实现的时候，使用了sys_open，那么kernel就通过一个数据结构收集起来，

对于

kernel会检测这个标志，考虑是否给这个


VirtualResourceHandle中的api，其作用应当是向kernel中的一个HwQueue push事务请求，kernel的处理线程读取这个HwQueue的请求包，然后重新 */