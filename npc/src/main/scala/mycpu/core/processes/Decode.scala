package mycpu.core.processes

import chisel3._
import chisel3.util._
import mycpu.core.os._
import mycpu.core.bundles._
import mycpu.common._

class DecodeProcess(implicit val iGen: FetchPacket, val oGen: DecodePacket) 
  extends HwProcess[FetchPacket, DecodePacket]("Decode") {

  def entry(): Unit = {
    // 1. 获取 RegFile 驱动
    val rf = sys_open("regfile")

    // 2. 创建一个守护逻辑 (Daemon Logic)
    // 它每一拍都在运行，不需要 Step
    val logic = createLogic("Combinational_Decode")
    
    // 隐式上下文：下面的代码都在 logic 作用域内
    implicit val ctx = LogicCtx(logic)

    logic.run {
      // A. 偷看输入 (Peek Input) - 组合逻辑
      val (valid, fetchPkt) = sys_peek()

      // B. 默认行为
      val outValid = logic.driveManaged(_stdout.enq.valid, false.B)
      val outBits  = logic.driveManaged(_stdout.enq.bits,  DontCare)
      
      // C. 只有当输入有效且输出不堵塞时
      when (valid && _stdout.canPush) {
        val rs1Addr = fetchPkt.inst(19, 15)
        val rs2Addr = fetchPkt.inst(24, 20)

        // D. 组合逻辑读寄存器 (Zero Delay!)
        // 这会直接生成 Wire 连接，没有时序开销
        val rs1Data = rf.readComb(rs1Addr)
        val rs2Data = rf.readComb(rs2Addr)

        // E. 组装输出包
        val out = Wire(new DecodePacket)
        out.connectDebug(fetchPkt)
        out.rs1Data := rs1Data
        out.rs2Data := rs2Data
        out.rdAddr  := fetchPkt.inst(11, 7)
        // ... 其他解码逻辑 ...
        out.ctrl := DontCare 

        // F. 写入输出
        logic.write(outValid, true.B)
        logic.write(outBits, out)

        // G. 消耗输入 (Pop Input)
        sys_consume()
      }
    }
  }
}