package mycpu.core.processes
import chisel3._
import chisel3.util._
import mycpu.core.os._
import mycpu.core.kernel._
import mycpu.common._
import mycpu.utils._

import mycpu.core.bundles._

class FetchProcess extends HwProcess[UInt, FetchPacket]("Fetch") {
  override def entry(): Unit = {
    val monitor = createLogic("PC_Observer")
    val pcDev = sys_open("PC")(monitor)

    // 观察 PC 变化触发 Fetch
    val pcChanged = Wire(Bool())
    monitor.run {
      val cur = pcDev.read(0.U)
      pcChanged := (cur =/= RegNext(cur)) || RegNext(true.B, false.B) // 处理 Reset
    }

    val t = createThread("Fetch_Worker")
    val imem = sys_open("IMEM")(t)
    val pcReader = sys_open("PC")(t)

    t.startWhen(pcChanged)
    t.Step("Fetch_Inst") {
      val curPc = pcReader.read(0.U)
      // 使用统一 API 读取指令内存
      val inst = imem.read(curPc, AccessSize.Word, false.B)
      
      val pkt = Wire(new FetchPacket)
      pkt.inst := inst
      // 这里的 sys_write 会在后级堵塞时自动挂起 Fetch 线程
      sys_write(pkt) 
    }
  }
}