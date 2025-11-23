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
    val redirect = Valid(UInt(XLEN.W)) 

    val debug_csrs = Output(new Bundle {
      val mtvec   = UInt(XLEN.W)
      val mepc    = UInt(XLEN.W)
      val mstatus = UInt(XLEN.W)
      val mcause  = UInt(XLEN.W)
    })


  })
  
  val inQueue = Queue(io.in, 1) 
  val data = inQueue.bits
  val ctrl = data.ctrl

  // 1. 操作数
  val op1 = Mux(ctrl.op1Sel === 1.U, data.pc, data.rs1Data)
  val op2 = Mux(ctrl.op2Sel === 1.U, data.imm, data.rs2Data)

  // 2. ALU
  val alu = Module(new ALU)
  alu.io.a  := op1
  alu.io.b  := op2
  alu.io.op := ctrl.aluOp

  // 3. CSR 模块
  val csr = Module(new CSR)
  csr.io.cmd   := ctrl.csrOp
  csr.io.addr  := data.csrAddr


  // [新增] 连接
  io.debug_csrs.mtvec   := csr.io.debug_mtvec
  io.debug_csrs.mepc    := csr.io.debug_mepc
  io.debug_csrs.mstatus := csr.io.debug_mstatus
  io.debug_csrs.mcause  := csr.io.debug_mcause


  
  // [修复] CSR 写数据选择逻辑
  // 如果是 CSRRWI/SI/CI (Immediate类型)，Op2Sel 在 Decode 中被设为 1
  // 此时使用 data.rs1Addr (即 zimm) 扩展后作为写数据
  // 否则使用 op1 (即 rs1Data)
  val zimm = data.rs1Addr
  csr.io.wdata := Mux(ctrl.op2Sel === 1.U, zimm, op1)

  csr.io.pc    := data.pc
  csr.io.isEcall := ctrl.isEcall && inQueue.valid
  csr.io.isMret  := ctrl.isMret  && inQueue.valid
  
  // 4. Ebreak (BlackBox)
  // 确保你已经创建了 SimEbreak.scala 并定义了 BlackBox
  val simEbreak = Module(new SimEbreak)
  simEbreak.io.valid := ctrl.isEbreak && inQueue.valid
  simEbreak.io.is_ebreak := 0.U 

  // 5. 分支/跳转逻辑 (使用 MuxLookup 修复之前的 match 错误)
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

  // 计算 PC 目标
  val jumpTarget = (data.pc + data.imm)
  val jalrTarget = (data.rs1Data + data.imm) & ~1.U(XLEN.W)
  
  val finalTarget = Mux(ctrl.isMret, csr.io.epc,
                    Mux(ctrl.isEcall, csr.io.evec,
                    Mux(ctrl.isJump && ctrl.op1Sel === 0.U, jalrTarget, jumpTarget)))

  val isRedirect = takeBranch || ctrl.isJump || ctrl.isEcall || ctrl.isMret

  // 6. 输出
  val resultData = Mux(ctrl.csrOp =/= CSROp.N, csr.io.rdata, alu.io.out)

  io.out.bits.aluResult := resultData
  io.out.bits.memWData  := data.rs2Data
  io.out.bits.rdAddr    := data.rdAddr
  io.out.bits.ctrl      := ctrl
  io.out.bits.pcTarget  := finalTarget
  io.out.bits.redirect  := isRedirect

  io.redirect.valid := isRedirect && inQueue.valid
  io.redirect.bits  := finalTarget

  io.out.valid := inQueue.valid
  inQueue.ready := io.out.ready
}