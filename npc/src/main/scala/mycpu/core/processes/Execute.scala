package mycpu.core.processes
import chisel3._
import chisel3.util._
import mycpu.core.os._
import mycpu.core.kernel._
import mycpu.common._
import mycpu.utils._

import mycpu.core.bundles._

class ExecuteProcess extends HwProcess[DecodePacket, ExecutePacket]("Execute") {
  override def entry(): Unit = {
    val t = createThread()
    val csr = sys_open("CSR")

    t.entry {
      val p = Reg(new ExecutePacket)
      
      t.Step("ALU_Compute") {
        val in = sys_read()
        p.connectDebug(in)
        
        val alu = Module(new ALU) // 这里的逻辑可以抽象为 HwFunction
        alu.io.a := in.rs1Data
        alu.io.b := in.imm
        alu.io.op := in.ctrl.aluOp
        
        p.aluResult := alu.io.out
        p.ctrl := in.ctrl
        p.memWData := in.rs2Data
      }

      t.Step("CSR_Ops") {
        // 如果是 CSR 指令，调用 CSR 驱动
        when(p.ctrl.csrOp =/= CSROp.N) {
           csr.ioctl(IoctlCmd.CSR_RW, 0.U)
           val old = csr.write(p.inst(31,20), p.aluResult)
           p.aluResult := old
        }
      }

      t.Step("Push") { sys_write(p) }
    }
  }
}