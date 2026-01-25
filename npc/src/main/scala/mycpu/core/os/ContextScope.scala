package mycpu.core.os

import scala.collection.mutable.Stack
import mycpu.utils._

// 上下文定义
sealed trait ExecutionContext
case class ThreadCtx(t: HardwareThread) extends ExecutionContext
case class LogicCtx(l: HardwareLogic)   extends ExecutionContext

// 上下文管理器 (单例)
object ContextScope {
  private val scopeStack = Stack[ExecutionContext]()

  // 压栈并执行代码块
  def withContext[T](ctx: ExecutionContext)(block: => T): T = {
    scopeStack.push(ctx)
    try {
      block
    } finally {
      scopeStack.pop()
    }
  }

  // 获取当前上下文
  def current: ExecutionContext = {
    if (scopeStack.isEmpty) {
      throw new Exception("[HwOS Error] You are calling a driver method outside of a Thread.entry or Logic.run block!")
    }
    scopeStack.top
  }
}