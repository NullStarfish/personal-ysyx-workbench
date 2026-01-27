package mycpu.core.os

import chisel3._
import chisel3.util._
import mycpu.common._
import mycpu.core.bundles._
import mycpu.core.backend.DecodeLogic
import mycpu.core.components.ImmGen

class MainProcess extends HwProcess[FetchPacket, UInt]("Processor") {
  override def entry(): Unit = {
    val t = createThread("Main_Transaction")
    
    // 打开驱动服务句柄
    val rf  = sys_open("RF")(t)
    val ex  = sys_open("EX")(t)
    val mem = sys_open("DMEM")(t)
    val pc  = sys_open("PC")(t)
    val csr = sys_open("CSR")(t)
    
    val immGen = Module(new ImmGen)
    immGen.io := DontCare

    t.Step("Transaction_Commit") {
      // 1. 等待并获取指令
      val (valid, in) = sys_peek()
      t.waitCondition(valid)

      val inst = in.inst
      val curPc = in.pc
      
      // 2. 调度资源：译码的同时配置驱动 (Side-effect happens here)
      val ctrl = DecodeLogic(inst, csr, mem)

      // 3. 准备参数
      immGen.io.inst := inst
      immGen.io.sel  := ctrl.immType
      val imm = immGen.io.out
      
      val rs1Val = rf.read(inst(19, 15))
      val rs2Val = rf.read(inst(24, 20))
      val zimm   = inst(19, 15).asUInt // 为 CSRI 指令准备

      val arg1 = MuxLookup(ctrl.arg1, 0.U)(Seq(
        Arg1Type.REG  -> rs1Val,
        Arg1Type.PC   -> curPc,
        Arg1Type.ZERO -> 0.U,
        Arg1Type.ZIMM -> zimm
      ))
      val arg2 = MuxLookup(ctrl.arg2, 0.U)(Seq(
        Arg2Type.REG  -> rs2Val,
        Arg2Type.IMM  -> imm,
        Arg2Type.CONST_4 -> 4.U
      ))

      // 4. 执行核心意图
      val writeBackData = WireDefault(0.U(32.W))
      val nextPc        = WireDefault(curPc + 4.U)

      switch(ctrl.service) {
        is(ServiceType.ALU) {
          // 参数直接来自 arg1, arg2。Param 提取由 EX 服务内部处理或这里显式传
          writeBackData := ex.write(arg1, arg2, inst(31, 25) ## inst(14, 12)) 
        }
        
        is(ServiceType.MEM_RD) {
          writeBackData := mem.read(arg1 + arg2) 
        }
        
        is(ServiceType.MEM_WR) {
          mem.write(arg1 + arg2, rs2Val)
        }
        
        is(ServiceType.BRANCH) {
          // 跳转指令写回 PC+4 (Link)
          writeBackData := curPc + 4.U
          
          val isTaken = BranchLogic(inst(14, 12), rs1Val, rs2Val)
          // 计算目标：JALR 是 (reg+imm)&~1，其他是 pc+imm
          val target = (arg1 + arg2) & Mux(ctrl.immType === ImmType.I, ~1.U(32.W), ~0.U(32.W))
          
          // J-Type(JAL) 或 I-Type(JALR) 是无条件跳转，B-Type(BEQ等) 视条件而定
          when(ctrl.immType === ImmType.J || ctrl.immType === ImmType.I || isTaken) {
            nextPc := target
          }
        }
        
        is(ServiceType.CSR) {
          writeBackData := csr.write(inst(31, 20), arg1)
        }
      }

      // 5. 提交结果到资源
      when(ctrl.regWen) {
        rf.write(inst(11, 7), writeBackData)
      }
      
      // 更新物理 PC，驱动 FetchProcess 进入下一轮
      pc.write(0.U, nextPc)
      
      // 指令退休
      sys_consume()
      t.agentPrint("COMMIT PC=%x, INST=%x, RES=%x", curPc, inst, writeBackData)
    }
  }

  // 纯组合逻辑分支判断
  def BranchLogic(f3: UInt, r1: UInt, r2: UInt): Bool = {
    MuxLookup(f3, false.B)(Seq(
      0.U -> (r1 === r2),            // BEQ
      1.U -> (r1 =/= r2),            // BNE
      4.U -> (r1.asSInt < r2.asSInt),  // BLT
      5.U -> (r1.asSInt >= r2.asSInt), // BGE
      6.U -> (r1 < r2),              // BLTU
      7.U -> (r1 >= r2)              // BGEU
    ))
  }
}