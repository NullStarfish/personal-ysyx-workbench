package mycpu.core.processes

import chisel3._
import chisel3.util._
import mycpu.core.os._
import mycpu.core.bundles._
import mycpu.core.components._
import mycpu.common._

class DecodeProcess extends HwProcess[FetchPacket, DecodePacket]("Decode") {
  override def entry(): Unit = {
    // === 1. 环境准备 ===
    val l = createLogic("Decode_Unit")
    
    // 打开资源句柄
    val rf  = sys_open("RF")(l)
    val csr = sys_open("CSR")(l)

    // 实例化组合逻辑组件
    val immGen = Module(new ImmGen)

    l.run {
      // === 2. 读取输入 (Fetch -> Decode 管道) ===
      val (valid, in) = sys_peek()
      
      // 默认输出初始化
      val out = Wire(new DecodePacket)
      out := DontCare 

      when(valid) {
        val inst = in.inst
        val rs1Addr = inst(19, 15)
        val rs2Addr = inst(24, 20)
        val rdAddr  = inst(11, 7)
        val csrAddr = inst(31, 20)

        // === 3. 译码逻辑 (Control Signals) ===
        // 这里我们可以复用你原来的 ListLookup 逻辑，或者将其封装进一个纯函数对象
        val ctrlSignals = DecodeLogic.decode(inst) // 假设我们把那张大表移到了 DecodeLogic 对象里

        // === 4. 资源访问 (寄存器与 CSR) ===
        // 因为在 Logic 上下文，这些 read 都是组合逻辑直连
        val rs1Data = rf.read(rs1Addr)
        val rs2Data = rf.read(rs2Addr)
        val cData   = csr.read(csrAddr) // 在 Decode 提前读出 CSR

        // === 5. 立即数生成 ===
        immGen.io.inst := inst
        immGen.io.sel  := ctrlSignals.immType

        // === 6. 组装输出包 ===
        out.connectDebug(in) // 透传 PC, inst, dnpc
        out.rs1Data := rs1Data
        out.rs2Data := rs2Data
        out.imm     := immGen.io.out
        out.csrData := cData
        out.rdAddr  := rdAddr
        out.rs1Addr := rs1Addr
        out.csrAddr := csrAddr
        out.ctrl    := ctrlSignals.ctrl

        // === 7. 管道握手 ===
        // 如果后级 Ready，我们就写入并消耗掉当前的输入
        when(_stdout.canPush) {
          sys_write(out)
          sys_consume() // 消耗 Fetch 传来的数据
          
          l.agentPrint("Decoded PC=%x, Inst=%x, OP1=%x, OP2=%x", 
                       in.pc, inst, rs1Data, immGen.io.out)
        }
      }
    }
  }
}