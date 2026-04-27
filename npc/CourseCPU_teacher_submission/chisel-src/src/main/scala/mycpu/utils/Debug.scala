package mycpu.utils

import chisel3._

object Debug {
  // 定义你的宏开关
  val isDebug = false 

  // [修改点]：将 args: Any* 改为 args: Data*
  def log(format: String, args: Bits*): Unit = {
    if (isDebug) {
      // 这里的 printf 是 chisel3.printf
      printf(format, args: _*)
    }
  }
}