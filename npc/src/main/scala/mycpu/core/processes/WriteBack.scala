package mycpu.core.processes
import chisel3._
import chisel3.util._
import mycpu.core.os._
import mycpu.core.bundles._
import mycpu.common._

class WbProcess extends HwProcess[MemoryPacket, UInt]("WriteBack") {
  override def entry(): Unit = {
    val t = createThread()
    val rf = sys_open("RF")
    val pcCtrl = sys_open("PC_CTRL")

    t.entry {
      t.Step("Commit") {
        val in = sys_read()
        
        // 写回通用寄存器
        when(in.regWen && in.rdAddr =/= 0.U) {
          rf.write(in.rdAddr, in.wbData)
        }

        // 如果发生了跳转/分支 (这里需要从 in 获取跳转信息)
        // 如果判断失败：
        // pcCtrl.write(0.U, in.pcTarget)
      }
    }
  }
}