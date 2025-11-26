package mycpu.core.backend

import chisel3._
import chisel3.util._
import mycpu.common._
import mycpu.core.bundles._
import mycpu.core.components._

class Execute extends Module {
  val io = IO(new Bundle {
    val in  = Flipped(Decoupled(new DecodePacket))
    val out = Decoupled(new ExecutePacket)
    // [修改] 移除 Redirect 接口，因为我们通过 WB 统一回传 PC
    // val redirect = Valid(UInt(XLEN.W)) 

    val debug_csrs = Output(new Bundle {
      val mtvec   = UInt(XLEN.W)
      val mepc    = UInt(XLEN.W)
      val mstatus = UInt(XLEN.W)
      val mcause  = UInt(XLEN.W)
    })
  })
  
  // 连接基础调试信息
  io.out.bits.connectDebug(io.in.bits)

  // 为了满足全解耦，这里使用 Queue 隔离或者直接连接逻辑均可
  // 使用 Queue(1) 可以切断组合逻辑路径，提升时序
  val inQueue = Queue(io.in, 1) 
  val data = inQueue.bits
  val ctrl = data.ctrl

  val op1 = Mux(ctrl.op1Sel === 1.U, data.pc, data.rs1Data)
  val op2 = Mux(ctrl.op2Sel === 1.U, data.imm, data.rs2Data)

  val alu = Module(new ALU)
  alu.io.a  := op1
  alu.io.b  := op2
  alu.io.op := ctrl.aluOp
  
  // 打印调试
  when (io.out.fire) {
      printf("EXECUTE: pc=0x%x inst=0x%x op1=0x%x op2=0x%x aluOp=%d aluOut=0x%x\n",
        data.pc, data.inst, op1, op2, ctrl.aluOp.asUInt, alu.io.out)
  }

  val csr = Module(new CSR)
  csr.io.cmd   := ctrl.csrOp
  csr.io.addr  := data.csrAddr
  io.debug_csrs.mtvec   := csr.io.debug_mtvec
  io.debug_csrs.mepc    := csr.io.debug_mepc
  io.debug_csrs.mstatus := csr.io.debug_mstatus
  io.debug_csrs.mcause  := csr.io.debug_mcause

  val zimm = data.rs1Addr
  csr.io.wdata := Mux(ctrl.op2Sel === 1.U, zimm, op1)

  csr.io.pc      := data.pc
  csr.io.isEcall := ctrl.isEcall && inQueue.valid
  csr.io.isMret  := ctrl.isMret  && inQueue.valid
  
  val simEbreak = Module(new SimEbreak)
  simEbreak.io.valid := ctrl.isEbreak && inQueue.valid
  simEbreak.io.is_ebreak := 0.U 

  // 分支判断
  val isEq  = data.rs1Data === data.rs2Data
  val isLt  = data.rs1Data.asSInt < data.rs2Data.asSInt
  val isLtu = data.rs1Data < data.rs2Data
  
  val branchCondition = MuxLookup(ctrl.memFunct3, false.B)(Seq(
    0.U -> isEq,
    1.U -> !isEq,
    4.U -> isLt,
    5.U -> !isLt,
    6.U -> isLtu,
    7.U -> !isLtu
  ))
  
  val takeBranch = ctrl.isBranch && branchCondition

  val jumpTarget = (data.pc + data.imm)
  val jalrTarget = (data.rs1Data + data.imm) & ~1.U(XLEN.W)
  
  val finalTarget = Mux(ctrl.isMret, csr.io.epc,
                    Mux(ctrl.isEcall, csr.io.evec,
                    Mux(ctrl.isJump && ctrl.op1Sel === 0.U, jalrTarget, jumpTarget)))

  val isRedirect = takeBranch || ctrl.isJump || ctrl.isEcall || ctrl.isMret
  
  // [关键] 计算实际的下一条 PC (dnpc)
  // 这个值会一路传到 WB，然后 WB 告诉 Fetch 取哪里
  val realNextPc = Mux(isRedirect, finalTarget, data.pc + 4.U)

  val resultData = Mux(ctrl.isJump, data.pc + 4.U, 
                   Mux(ctrl.csrOp =/= CSROp.N, csr.io.rdata, alu.io.out))

  io.out.bits.aluResult := resultData
  io.out.bits.memWData  := data.rs2Data
  io.out.bits.rdAddr    := data.rdAddr
  io.out.bits.ctrl      := ctrl
  io.out.bits.pcTarget  := finalTarget
  io.out.bits.redirect  := DontCare // 废弃字段
  
  // [关键] 更新 dnpc
  io.out.bits.dnpc      := realNextPc 

  io.out.valid := inQueue.valid
  inQueue.ready := io.out.ready
}