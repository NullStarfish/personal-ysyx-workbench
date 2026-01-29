package mycpu.core.kernel

import chisel3._
import chisel3.util._
import mycpu.core.os._
import mycpu.utils._ // [修复] 添加此行以找到 HardwareThread/Logic
import scala.collection.mutable.ArrayBuffer

class ProcessContext(
  val pid: Int, 
  val parent: Option[ProcessContext],
  val name: String,
  val kernel: Kernel
) {
  var children: List[ProcessContext] = Nil
  def path: String = parent.map(_.path + "/").getOrElse("") + name
}

object PidAllocator {
  private var _next = 0
  def next: Int = { _next += 1; _next }
}

abstract class HwProcess(val pName: String)(implicit parentCtx: Option[ProcessContext], kernel: Kernel) {
  
  val ctx = new ProcessContext(PidAllocator.next, parentCtx, pName, kernel)
  parentCtx.foreach(p => p.children = ctx :: p.children)

  var stdin:  VirtualResourceHandle = _
  var stdout: VirtualResourceHandle = _
  var stderr: VirtualResourceHandle = _

  private val threads = ArrayBuffer[HardwareThread]()
  private val logics  = ArrayBuffer[HardwareLogic]()

  protected def createThread(name: String = "Main"): HardwareThread = {
    val t = new HardwareThread(s"${ctx.path}_$name")
    threads += t
    t
  }
  
  protected def createLogic(name: String = "Daemon"): HardwareLogic = {
    val l = new HardwareLogic(s"${ctx.path}_$name")
    logics += l
    l
  }

  def sys_open(name: String): VirtualResourceHandle = {
    kernel.createConnection(name)
  }

  def spawn[T <: HwProcess](childGen: (Option[ProcessContext], Kernel) => T)
                           (in: VirtualResourceHandle = this.stdin, 
                            out: VirtualResourceHandle = this.stdout): T = {
    val child = childGen(Some(this.ctx), this.kernel)
    child.stdin  = in
    child.stdout = out
    child.stderr = this.stderr
    child
  }

  def entry(): Unit
  
  def build(): Unit = {
    // hasDriver 已在 Kernel 中添加
    if (stdin == null && kernel.hasDriver("term"))  stdin  = sys_open("term")
    if (stdout == null && kernel.hasDriver("term")) stdout = sys_open("term")
    
    entry()
    threads.foreach(t => if(!t.hasStartCondition) t.startWhen(true.B))
    logics.foreach(l => l.run {}) 
  }
}