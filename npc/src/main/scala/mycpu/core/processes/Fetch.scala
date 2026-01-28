package mycpu.core.processes

import chisel3._
import chisel3.util._
import mycpu.core.os._
import mycpu.core.kernel._
import mycpu.common._
import mycpu.core.bundles._

class FetchProcess extends HwProcess[UInt, FetchPacket]("Fetch") {
  override def entry(): Unit = {
    // ---------------------------------------------
    // 1. 资源句柄
    // ---------------------------------------------
    val t = createThread("Fetch_Worker")
    val imem = sys_open("IMEM")(t)
    val pcDev = sys_open("PC")(t)

    // ---------------------------------------------
    // 2. 线程逻辑 (死循环模式)
    // ---------------------------------------------
    
    // 让线程一上电就启动，并且永远不 OFFLINE
    t.startWhen(true.B) 
    
    t.entry {
      // 记录上次取指的 PC (线程私有状态)
      val lastFetchedPc = RegInit(0.U(32.W)) 
      // 暂存当前要取的 PC
      val targetPc      = RegInit(0.U(32.W))

      // === Step 0: 等待 PC 变化 ===
      t.Step("Wait_New_PC") {
        val curr = pcDev.read(0.U)
        
        // 核心逻辑：只有当当前 PC 与上次不同时，才继续
        // 上电时：last=0, curr=30000000 -> 不相等 -> 继续
        // 取指后：last=30000000, curr=30000000 -> 相等 -> 阻塞(Stall)
        // Main更新后：last=30000000, curr=30000004 -> 不相等 -> 继续
        t.waitCondition(curr =/= lastFetchedPc)
        
        // 锁定目标 PC，供后续步骤使用
        targetPc := curr
      }

      // === Step 1 & 2: 调用驱动 (自动生成步骤) ===
      // 注意：这里使用的是 targetPc 寄存器，它在 Step 0 结束时更新
      val inst = imem.read(targetPc)

      // === Step 3: 发送并更新状态 ===
      val pkt = Wire(new FetchPacket)
      pkt.inst := inst

      t.Step("Push_And_Update") {
        sys_write(pkt) // 写入管道 (如果下游满则阻塞)
        
        // 标记该 PC 已处理
        lastFetchedPc := targetPc
        
        t.agentPrint("Fetched: PC=%x, Inst=%x", targetPc, inst)
        
        // 关键：回到 Step 0，而不是退出线程
        t.Loop() 
      }
    }
  }
}