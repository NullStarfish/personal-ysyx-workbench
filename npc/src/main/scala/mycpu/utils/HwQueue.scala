package mycpu.utils
import chisel3._
import chisel3.util._



/**
 * HwQueue: 一个纯 Scala 类，包装了硬件 Queue，提供类似软件的 API
 */
class HwQueue[T <: Data](gen: T, entries: Int, name: String = "Queue", debug: Boolean = true) {
  
  // 在类实例化时，直接在当前父模块中生成硬件 Queue
  private val q = Module(new Queue(gen, entries))
  q.suggestName(name) // 给 Verilog 里的模块起个好名字

  // === 公开基础 IO (为了兼容 <> 连接) ===
  val enq = q.io.enq
  val deq = q.io.deq
  val count = q.io.count

  // 默认断开连接 (防止 Chisel 报错未驱动)
  enq.valid := false.B
  enq.bits  := DontCare
  deq.ready := false.B

  // === 调试逻辑 (生成时自动挂载) ===
  if (debug) {
    // 这里利用 printf 的时序特性，只有在 fire 时打印
    when(enq.fire) {
      printf(s"[$name] Push: Data=%x, Count=%d\n", enq.bits.asUInt, count + 1.U)
    }
    when(deq.fire) {
      printf(s"[$name] Pop:  Data=%x, Count=%d\n", deq.bits.asUInt, count - 1.U)
    }
  }

  /**
   * 尝试压入数据
   * 返回: Bool (是否成功压入)
   */
  def tryPush(data: T): Bool = {
    enq.valid := true.B
    enq.bits  := data
    enq.ready // 返回 ready 信号作为成功标志
  }

  /**
   * 尝试弹出数据
   * 返回: Bool (是否成功弹出)
   */
  def tryPop(): Bool = {
    deq.ready := true.B
    deq.valid // 返回 valid 信号作为成功标志
  }

  // 查看队头数据 (不消耗)
  def peek(): T = deq.bits
  
  // 状态查询
  def canPush: Bool = enq.ready
  def canPop: Bool  = deq.valid
  def isEmpty: Bool = !deq.valid
  def isFull: Bool  = !enq.ready
}