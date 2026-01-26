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
    // === 1. 定义观察者逻辑 (Monitor Logic) ===
    val monitor = createLogic("PC_Observer")
    
    // 每一个 Agent 需要自己的驱动视图
    val pcForMon = sys_open("PC")(monitor)

    // 内部状态
    val pcChanged = Wire(Bool())
    val isFirstCycle = RegInit(true.B) // 处理复位后的第一条指令

    monitor.run {
      val currentPc = pcForMon.read(0.U)
      val lastPc    = RegNext(currentPc)
      
      // 当当拍 PC 与上一拍不同，或者处于复位后的第一拍
      // 这里 "currentPc =/= lastPc" 捕捉到了 WB 的写回动作
      pcChanged := (currentPc =/= lastPc) || isFirstCycle
      
      when(isFirstCycle) { isFirstCycle := false.B }
      
      // 调试：打印观察到的 PC 变化
      when(pcChanged) {
        monitor.agentPrint("Monitor: PC change detected! New PC = %x", currentPc)
      }
    }

    // === 2. 定义抓取线程 (Main Thread) ===
    val t = createThread("Fetch_Worker")
    val pcForThread = sys_open("PC")(t)
    val imem        = sys_open("IMEM")(t)

    // [关键] 只要监听到变化，线程就开始工作
    // 因为 HardwareThread 默认是非 Mealy 的，
    // 它会在监听到 pcChanged 的下一拍进入 Step_0
    t.startWhen(pcChanged)

    t.Step("Do_Fetch") {
      // 1. 从物理寄存器读出目标地址
      val pcToFetch = pcForThread.read(0.U)
      
      // 2. 发起 AXI 指令读取
      // 在 AtomicCtx 下，SmartAXIDriver 会在这里阻塞直到指令返回
      val inst = imem.read(pcToFetch, AccessSize.Word)
      
      // 3. 组包
      val pkt = Wire(new FetchPacket)
      pkt.pc   := pcToFetch
      pkt.inst := inst
      pkt.dnpc := pcToFetch + 4.U
      
      // 4. 发送到后级 Decode
      // sys_write 在 stdout 满时会自动阻塞 Thread
      sys_write(pkt)
      
      // 线程执行完最后一步会自动变为 inactive
      // 等待下一次 WB 写回物理 PC 触发 pcChanged
      t.agentPrint("Worker: Inst %x fetched from PC %x. Going to sleep...", inst, pcToFetch)
    }
  }
}