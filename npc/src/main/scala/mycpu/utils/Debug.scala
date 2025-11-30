package mycpu.utils

object Debug {
  // 必须是 final val，这样编译器将其视为常量
  final val isDebug: Boolean = true 
  // 或者从系统属性读取 (注意：这种方式无法在编译期完全剔除代码，但在运行时会有判断)
  // final val isDebug = System.getProperty("DEBUG") == "1"

  def log(format: String, args: Any*): Unit = {
    if (isDebug) {
      scala.Predef.printf(format, args: _*)
    }
  }
}