package mycpu.core.backend

import chisel3._
import chisel3.util._
import mycpu.common._
import mycpu.common.Instructions._
import mycpu.core.bundles._
import mycpu.core.components.{RegFile, ImmGen}
import mycpu.utils._

class Decode extends Module {
  val io = IO(new Bundle {
    val in  = Flipped(Decoupled(new FetchPacket))
    val out = Decoupled(new DecodePacket())
    val regWrite = Flipped(new WriteBackIO())

    val debug_regs = Output(Vec(32, UInt(XLEN.W))) 
  })

  // [修改] 一键透传调试信息 (pc, inst)
  io.out.bits.connectDebug(io.in.bits)

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

  io.debug_regs := regFile.io.debug_regs
  
  // 3. 控制信号解码 (代码保持不变)
  val Y = true.B
  val N = false.B
  val defaultCtrl = List(ImmType.Z, ALUOp.NOP, 0.U, 0.U, N, N, N, CSROp.N, N, N)
  
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
    LB    -> List(ImmType.I, ALUOp.ADD,    0.U, 1.U, Y, Y, N, CSROp.N, N, N),
    LBU   -> List(ImmType.I, ALUOp.ADD,    0.U, 1.U, Y, Y, N, CSROp.N, N, N),
    LHU   -> List(ImmType.I, ALUOp.ADD,    0.U, 1.U, Y, Y, N, CSROp.N, N, N),
    LH    -> List(ImmType.I, ALUOp.ADD,    0.U, 1.U, Y, Y, N, CSROp.N, N, N),


    SB    -> List(ImmType.S, ALUOp.ADD,    0.U, 1.U, N, Y, Y, CSROp.N, N, N),
    SH    -> List(ImmType.S, ALUOp.ADD,    0.U, 1.U, N, Y, Y, CSROp.N, N, N),
    SW    -> List(ImmType.S, ALUOp.ADD,    0.U, 1.U, N, Y, Y, CSROp.N, N, N),


    ADDI  -> List(ImmType.I, ALUOp.ADD,    0.U, 1.U, Y, N, N, CSROp.N, N, N),
    ANDI  -> List(ImmType.I, ALUOp.AND,    0.U, 1.U, Y, N, N, CSROp.N, N, N),
    ORI   -> List(ImmType.I, ALUOp.OR,     0.U, 1.U, Y, N, N, CSROp.N, N, N),
    XORI  -> List(ImmType.I, ALUOp.XOR,    0.U, 1.U, Y, N, N, CSROp.N, N, N),
    SLTI  -> List(ImmType.I, ALUOp.SLT,    0.U, 1.U, Y, N, N, CSROp.N, N, N),
    SLTIU -> List(ImmType.I, ALUOp.SLTU,   0.U, 1.U, Y, N, N, CSROp.N, N, N),
    SLLI  -> List(ImmType.I, ALUOp.SLL,    0.U, 1.U, Y, N, N, CSROp.N, N, N),
    SRLI  -> List(ImmType.I, ALUOp.SRL,    0.U, 1.U, Y, N, N, CSROp.N, N, N),
    SRAI  -> List(ImmType.I, ALUOp.SRA,    0.U, 1.U, Y, N, N, CSROp.N, N, N),



    ADD   -> List(ImmType.Z, ALUOp.ADD,    0.U, 0.U, Y, N, N, CSROp.N, N, N),
    SUB   -> List(ImmType.Z, ALUOp.SUB,    0.U, 0.U, Y, N, N, CSROp.N, N, N),
    AND   -> List(ImmType.Z, ALUOp.AND,    0.U, 0.U, Y, N, N, CSROp.N, N, N),
    OR    -> List(ImmType.Z, ALUOp.OR,     0.U, 0.U, Y, N, N, CSROp.N, N, N),
    XOR   -> List(ImmType.Z, ALUOp.XOR,    0.U, 0.U, Y, N, N, CSROp.N, N, N),
    SLT   -> List(ImmType.Z, ALUOp.SLT,    0.U, 0.U, Y, N, N, CSROp.N, N, N),
    SLTU  -> List(ImmType.Z, ALUOp.SLTU,   0.U, 0.U, Y, N, N, CSROp.N, N, N),
    SLL   -> List(ImmType.Z, ALUOp.SLL,    0.U, 0.U, Y, N, N, CSROp.N, N, N),
    SRL   -> List(ImmType.Z, ALUOp.SRL,    0.U, 0.U, Y, N, N, CSROp.N, N, N),
    SRA   -> List(ImmType.Z, ALUOp.SRA,    0.U, 0.U, Y, N, N, CSROp.N, N, N),



    CSRRW -> List(ImmType.I, ALUOp.COPY_A, 0.U, 0.U, Y, N, N, CSROp.W, N, N),
    CSRRS -> List(ImmType.I, ALUOp.COPY_A, 0.U, 0.U, Y, N, N, CSROp.S, N, N),
    CSRRC -> List(ImmType.I, ALUOp.COPY_A, 0.U, 0.U, Y, N, N, CSROp.C, N, N),
    CSRRWI-> List(ImmType.I, ALUOp.COPY_A, 0.U, 1.U, Y, N, N, CSROp.W, N, N), 
    CSRRSI-> List(ImmType.I, ALUOp.COPY_A, 0.U, 1.U, Y, N, N, CSROp.S, N, N),
    CSRRCI-> List(ImmType.I, ALUOp.COPY_A, 0.U, 1.U, Y, N, N, CSROp.C, N, N),





  )

  val ctrlSignals = ListLookup(inst, defaultCtrl, map)

  when (io.out.fire) {
    Debug.log("decode: pc : %x, inst: %x, ctrl.aluop: %x\n", io.in.bits.pc, io.in.bits.inst, io.out.bits.ctrl.aluOp.asUInt)
  }

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

  io.out.bits.rs1Data := regFile.io.rdata1
  io.out.bits.rs2Data := regFile.io.rdata2
  io.out.bits.imm     := immGen.io.out
  io.out.bits.rdAddr  := rdAddr
  io.out.bits.rs1Addr := rs1Addr
  io.out.bits.ctrl    := ctrl
  io.out.bits.csrAddr := inst(31, 20)

  io.out.valid := io.in.valid
  io.in.ready  := io.out.ready
}