package mycpu.core.backend

import chisel3._
import chisel3.util._
import mycpu.common._
import mycpu.common.Instructions._
import mycpu.core.bundles._
// [修复] 显式导入需要的组件，避免与 common._ 中的 implicit 冲突
import mycpu.core.components.{RegFile, ImmGen}

class Decode extends Module {
  val io = IO(new Bundle {
    val in  = Flipped(Decoupled(new FetchPacket))
    val out = Decoupled(new DecodePacket())
    val regWrite = Flipped(new WriteBackIO())

    val debug_regs = Output(Vec(32, UInt(XLEN.W))) // 新增
  })

  val inst = io.in.bits.inst
  val rs1Addr = inst(19, 15)
  val rs2Addr = inst(24, 20)
  val rdAddr  = inst(11, 7)
  
  // 1. 立即数
  val immType = Wire(ImmType())
  val immGen = Module(new ImmGen)
  immGen.io.inst := inst
  immGen.io.sel  := immType

  // 2. 寄存器堆
  val regFile = Module(new RegFile)
  regFile.io.raddr1 := rs1Addr
  regFile.io.raddr2 := rs2Addr
  regFile.io.wen    := io.regWrite.wen
  regFile.io.waddr  := io.regWrite.addr
  regFile.io.wdata  := io.regWrite.data


  io.debug_regs := regFile.io.debug_regs // 连接内部 RegFile
  
  // 3. 控制信号解码
  val Y = true.B
  val N = false.B
  val defaultCtrl = List(ImmType.Z, ALUOp.NOP, 0.U, 0.U, N, N, N, CSROp.N, N, N)
  
  // ... (此处保持之前的指令映射表 map 逻辑不变) ...
  val map = Array(
    LUI   -> List(ImmType.U, ALUOp.COPY_B, 0.U, 1.U, Y, N, N, CSROp.N, N, N),
    AUIPC -> List(ImmType.U, ALUOp.ADD,    1.U, 1.U, Y, N, N, CSROp.N, N, N),
    JAL   -> List(ImmType.J, ALUOp.ADD,    1.U, 1.U, Y, N, N, CSROp.N, N, Y),
    JALR  -> List(ImmType.I, ALUOp.ADD,    0.U, 1.U, Y, N, N, CSROp.N, N, Y),
    BEQ   -> List(ImmType.B, ALUOp.NOP,    0.U, 0.U, N, N, N, CSROp.N, Y, N),
    BNE   -> List(ImmType.B, ALUOp.NOP,    0.U, 0.U, N, N, N, CSROp.N, Y, N),
    BLT   -> List(ImmType.B, ALUOp.NOP,    0.U, 0.U, N, N, N, CSROp.N, Y, N),
    BGE   -> List(ImmType.B, ALUOp.NOP,    0.U, 0.U, N, N, N, CSROp.N, Y, N),
    BLTU  -> List(ImmType.B, ALUOp.NOP,    0.U, 0.U, N, N, N, CSROp.N, Y, N),
    BGEU  -> List(ImmType.B, ALUOp.NOP,    0.U, 0.U, N, N, N, CSROp.N, Y, N),
    LW    -> List(ImmType.I, ALUOp.ADD,    0.U, 1.U, Y, Y, N, CSROp.N, N, N),
    SW    -> List(ImmType.S, ALUOp.ADD,    0.U, 1.U, N, Y, Y, CSROp.N, N, N),
    ADDI  -> List(ImmType.I, ALUOp.ADD,    0.U, 1.U, Y, N, N, CSROp.N, N, N),
    ADD   -> List(ImmType.Z, ALUOp.ADD,    0.U, 0.U, Y, N, N, CSROp.N, N, N),
    SUB   -> List(ImmType.Z, ALUOp.SUB,    0.U, 0.U, Y, N, N, CSROp.N, N, N),
    AND   -> List(ImmType.Z, ALUOp.AND,    0.U, 0.U, Y, N, N, CSROp.N, N, N),
    OR    -> List(ImmType.Z, ALUOp.OR,     0.U, 0.U, Y, N, N, CSROp.N, N, N),
    XOR   -> List(ImmType.Z, ALUOp.XOR,    0.U, 0.U, Y, N, N, CSROp.N, N, N),
    ANDI  -> List(ImmType.I, ALUOp.AND,    0.U, 1.U, Y, N, N, CSROp.N, N, N),
    ORI   -> List(ImmType.I, ALUOp.OR,     0.U, 1.U, Y, N, N, CSROp.N, N, N),
    XORI  -> List(ImmType.I, ALUOp.XOR,    0.U, 1.U, Y, N, N, CSROp.N, N, N),
    SLTI  -> List(ImmType.I, ALUOp.SLT,    0.U, 1.U, Y, N, N, CSROp.N, N, N),
    SLTIU -> List(ImmType.I, ALUOp.SLTU,   0.U, 1.U, Y, N, N, CSROp.N, N, N),
    SLLI  -> List(ImmType.I, ALUOp.SLL,    0.U, 1.U, Y, N, N, CSROp.N, N, N),
    SRLI  -> List(ImmType.I, ALUOp.SRL,    0.U, 1.U, Y, N, N, CSROp.N, N, N),
    SRAI  -> List(ImmType.I, ALUOp.SRA,    0.U, 1.U, Y, N, N, CSROp.N, N, N),
    
    // CSR Instructions (Check op2Sel logic for ZIMM)
    CSRRW -> List(ImmType.I, ALUOp.COPY_A, 0.U, 0.U, Y, N, N, CSROp.W, N, N),
    CSRRS -> List(ImmType.I, ALUOp.COPY_A, 0.U, 0.U, Y, N, N, CSROp.S, N, N),
    CSRRC -> List(ImmType.I, ALUOp.COPY_A, 0.U, 0.U, Y, N, N, CSROp.C, N, N),
    // For Immediate CSR (RWI, RSI, RCI), we set Op2Sel=1 to indicate we use ZIMM (from rs1Addr)
    CSRRWI-> List(ImmType.I, ALUOp.COPY_A, 0.U, 1.U, Y, N, N, CSROp.W, N, N), 
    CSRRSI-> List(ImmType.I, ALUOp.COPY_A, 0.U, 1.U, Y, N, N, CSROp.S, N, N),
    CSRRCI-> List(ImmType.I, ALUOp.COPY_A, 0.U, 1.U, Y, N, N, CSROp.C, N, N)
  )

  val ctrlSignals = ListLookup(inst, defaultCtrl, map)
  
  immType        := ctrlSignals(0)
  val ctrl = Wire(new ControlSignals)
  ctrl.aluOp     := ctrlSignals(1)
  ctrl.op1Sel    := ctrlSignals(2)
  ctrl.op2Sel    := ctrlSignals(3)
  ctrl.regWen    := ctrlSignals(4)
  ctrl.memEn     := ctrlSignals(5)
  ctrl.memWen    := ctrlSignals(6)
  ctrl.csrOp     := ctrlSignals(7)
  ctrl.isBranch  := ctrlSignals(8)
  ctrl.isJump    := ctrlSignals(9)
  
  ctrl.memFunct3 := inst(14, 12)
  ctrl.isEcall   := (inst === Instructions.ECALL.value.U)
  ctrl.isMret    := (inst === Instructions.MRET.value.U)
  ctrl.isEbreak  := (inst === Instructions.EBREAK.value.U)

  // 输出
  io.out.bits.pc      := io.in.bits.pc
  io.out.bits.rs1Data := regFile.io.rdata1
  io.out.bits.rs2Data := regFile.io.rdata2
  io.out.bits.imm     := immGen.io.out
  io.out.bits.rdAddr  := rdAddr
  // [修复] 连接 rs1Addr
  io.out.bits.rs1Addr := rs1Addr
  io.out.bits.ctrl    := ctrl
  io.out.bits.csrAddr := inst(31, 20)

  io.out.valid := io.in.valid
  io.in.ready  := io.out.ready
}