package mycpu.core.frontend

import chisel3._
import chisel3.util._
import mycpu.common._
import mycpu.common.Instructions._
import mycpu.core.bundles._
import mycpu.core.components.{ImmGen, RegFile}

class Decode(
    enableTraceFields: Boolean = ENABLE_TRACE_FIELDS,
) extends Module {
  val io = IO(new Bundle {
    val in = Flipped(Decoupled(new I$1Packet))
    val out = Decoupled(new DecodePacket)
    val regWrite = Flipped(new WriteBackIO())
    val forwards = Input(Vec(2, new ForwardPacket))
    val debug_regs = Output(Vec(32, UInt(XLEN.W)))
  })

  val inst = io.in.bits.inst
  val opcode = inst(6, 0)
  val funct3 = inst(14, 12)
  val funct7 = inst(31, 25)
  val rs1Addr = inst(19, 15)
  val rs2Addr = inst(24, 20)
  val rdAddr = inst(11, 7)

  val regFile = Module(new RegFile)
  regFile.io.raddr1 := rs1Addr
  regFile.io.raddr2 := rs2Addr
  regFile.io.wen := io.regWrite.regWrite.wen
  regFile.io.waddr := io.regWrite.regWrite.rd
  regFile.io.wdata := io.regWrite.regWrite.wdata
  io.debug_regs := regFile.io.debug_regs

  val immType = Wire(ImmType())
  val immGen = Module(new ImmGen)
  immGen.io.inst := inst
  immGen.io.sel := immType

  val rs1Raw = regFile.io.rdata1
  val rs2Raw = regFile.io.rdata2

  private def decodeFormat(opcode: UInt, funct3: UInt): DecodeFormat.Type = MuxLookup(opcode, DecodeFormat.None)(
    Seq(
      "b0110111".U -> DecodeFormat.Pc_Imm,
      "b0010111".U -> DecodeFormat.Pc_Imm,
      "b1101111".U -> DecodeFormat.Pc_Offset,
      "b1100111".U -> DecodeFormat.Reg_Offset,
      "b1100011".U -> DecodeFormat.Reg_Reg_Offset,
      "b0000011".U -> DecodeFormat.Reg_Offset,
      "b0100011".U -> DecodeFormat.Reg_Reg_Offset,
      "b0010011".U -> DecodeFormat.Reg_Imm,
      "b0110011".U -> DecodeFormat.Reg_Reg,
      "b1110011".U -> Mux(
        opcode === "b1110011".U && funct3 === 0.U,
        DecodeFormat.Sys,
        Mux(funct3(2), DecodeFormat.CsrImm, DecodeFormat.CsrReg),
      ),
    ),
  )

  val format = decodeFormat(opcode, funct3)
  val subop = WireDefault(SizeSubop.None)
  val rs1Data = WireDefault(rs1Raw)
  val rs2Data = WireDefault(rs2Raw)
  val imm = WireDefault(0.U(XLEN.W))
  val aluOp = WireDefault(ALUOp.NOP)
  val aluSrcA = WireDefault(ALUSrcA.Rs1)
  val aluSrcB = WireDefault(ALUSrcB.Rs2)
  val wbSel = WireDefault(WBSel.Alu)
  val branchType = WireDefault(BranchType.None)
  val isJump = WireDefault(false.B)
  val isJalr = WireDefault(false.B)
  val regWen = WireDefault(false.B)
  val memUnsigned = WireDefault(false.B)
  val csrAddr = WireDefault(inst(31, 20))
  val csrOp = WireDefault(CSROp.N)
  val isEcall = WireDefault(false.B)
  val isMret = WireDefault(false.B)
  val isEbreak = WireDefault(false.B)
  val isFenceI = WireDefault(false.B)

  val instType = TraceVal(WireDefault(InstType.arith))

  private def forwardHit(src: ForwardSource, regAddr: UInt): Bool =
    src.valid && regAddr =/= 0.U && src.addr === regAddr

  private def resolveRegValue(srcValid: Bool, regAddr: UInt, regValue: UInt): UInt = {
    val forwarded = WireDefault(regValue)
    for (src <- io.forwards.reverse) {
      when(srcValid && forwardHit(src, regAddr)) {
        forwarded := src.data
      }
    }
    forwarded
  }

  immType := ImmType.I

  switch(opcode) {
    is("b0110111".U) { immType := ImmType.U }
    is("b0010111".U) { immType := ImmType.U }
    is("b1101111".U) { immType := ImmType.J }
    is("b1100011".U) { immType := ImmType.B }
    is("b0100011".U) { immType := ImmType.S }
    is("b0110011".U) { immType := ImmType.Z }
  }

  switch(opcode) {
    is("b0110111".U) { // lui
      regWen := rdAddr =/= 0.U
      aluOp := ALUOp.COPY_B
      aluSrcB := ALUSrcB.Imm
      imm := immGen.io.out
    }
    is("b0010111".U) { // auipc
      regWen := rdAddr =/= 0.U
      aluOp := ALUOp.ADD
      aluSrcA := ALUSrcA.Pc
      aluSrcB := ALUSrcB.Imm
      imm := immGen.io.out
    }
    is("b1101111".U) { // jal
      instType.foreach(_ := InstType.redirect)
      regWen := rdAddr =/= 0.U
      aluOp := ALUOp.ADD
      aluSrcA := ALUSrcA.Pc
      aluSrcB := ALUSrcB.Imm
      imm := immGen.io.out
      wbSel := WBSel.PcPlus4
      isJump := true.B
    }
    is("b1100111".U) { // jalr
      instType.foreach(_ := InstType.redirect)
      regWen := rdAddr =/= 0.U
      aluOp := ALUOp.ADD
      aluSrcA := ALUSrcA.Rs1
      aluSrcB := ALUSrcB.Imm
      imm := immGen.io.out
      wbSel := WBSel.PcPlus4
      isJump := true.B
      isJalr := true.B
    }
    is("b1100011".U) { // branch
      instType.foreach(_ := InstType.redirect)
      imm := immGen.io.out
      switch(funct3) {
        is("b000".U) { branchType := BranchType.Eq }
        is("b001".U) { branchType := BranchType.Ne }
        is("b100".U) { branchType := BranchType.Lt; aluOp := ALUOp.SLT }
        is("b101".U) { branchType := BranchType.Ge; aluOp := ALUOp.SLT }
        is("b110".U) { branchType := BranchType.Ltu; aluOp := ALUOp.SLTU }
        is("b111".U) { branchType := BranchType.Geu; aluOp := ALUOp.SLTU }
      }
    }
    is("b0000011".U) { // load
      instType.foreach(_ := InstType.mem)
      regWen := rdAddr =/= 0.U
      aluOp := ALUOp.ADD
      aluSrcA := ALUSrcA.Rs1
      aluSrcB := ALUSrcB.Imm
      imm := immGen.io.out
      switch(funct3) {
        is("b000".U) { subop := SizeSubop.Byte }
        is("b001".U) { subop := SizeSubop.Half }
        is("b010".U) { subop := SizeSubop.Word }
        is("b100".U) { subop := SizeSubop.Byte; memUnsigned := true.B }
        is("b101".U) { subop := SizeSubop.Half; memUnsigned := true.B }
      }
    }
    is("b0100011".U) { // store
      instType.foreach(_ := InstType.mem)
      aluOp := ALUOp.ADD
      aluSrcA := ALUSrcA.Rs1
      aluSrcB := ALUSrcB.Imm
      imm := immGen.io.out
      switch(funct3) {
        is("b000".U) { subop := SizeSubop.Byte }
        is("b001".U) { subop := SizeSubop.Half }
        is("b010".U) { subop := SizeSubop.Word }
      }
    }
    is("b0010011".U) { // alu imm
      regWen := rdAddr =/= 0.U
      aluSrcA := ALUSrcA.Rs1
      aluSrcB := ALUSrcB.Imm
      imm := immGen.io.out
      switch(funct3) {
        is("b000".U) { aluOp := ALUOp.ADD }
        is("b111".U) { aluOp := ALUOp.AND }
        is("b110".U) { aluOp := ALUOp.OR }
        is("b100".U) { aluOp := ALUOp.XOR }
        is("b010".U) { aluOp := ALUOp.SLT }
        is("b011".U) { aluOp := ALUOp.SLTU }
        is("b001".U) { aluOp := ALUOp.SLL }
        is("b101".U) { aluOp := Mux(funct7 === "b0100000".U, ALUOp.SRA, ALUOp.SRL) }
      }
    }
    is("b0110011".U) { // alu reg
      regWen := rdAddr =/= 0.U
      switch(funct3) {
        is("b000".U) { aluOp := Mux(funct7 === "b0100000".U, ALUOp.SUB, ALUOp.ADD) }
        is("b111".U) { aluOp := ALUOp.AND }
        is("b110".U) { aluOp := ALUOp.OR }
        is("b100".U) { aluOp := ALUOp.XOR }
        is("b010".U) { aluOp := ALUOp.SLT }
        is("b011".U) { aluOp := ALUOp.SLTU }
        is("b001".U) { aluOp := ALUOp.SLL }
        is("b101".U) { aluOp := Mux(funct7 === "b0100000".U, ALUOp.SRA, ALUOp.SRL) }
      }
    }
    is("b1110011".U) { // csr/sys
      instType.foreach(_ := InstType.sys)
      when(inst === Instructions.ECALL.value.U) {
        isEcall := true.B
      }.elsewhen(inst === Instructions.MRET.value.U) {
        isMret := true.B
      }.elsewhen(inst === Instructions.EBREAK.value.U) {
        isEbreak := true.B
      }.otherwise {
        regWen := rdAddr =/= 0.U
        wbSel := WBSel.Csr
        csrOp := MuxLookup(funct3, CSROp.N)(Seq(
          "b001".U -> CSROp.W,
          "b010".U -> CSROp.S,
          "b011".U -> CSROp.C,
          "b101".U -> CSROp.W,
          "b110".U -> CSROp.S,
          "b111".U -> CSROp.C,
        ))
        when(funct3(2)) {
          rs1Data := Cat(0.U((XLEN - 5).W), rs1Addr)
        }
      }
    }
    is("b0001111".U) { // misc-mem
      instType.foreach(_ := InstType.sys)
      when(inst === Instructions.FENCEI.value.U) {
        isFenceI := true.B
      }
    }
  }

  val rs1Valid =
      format === DecodeFormat.Reg_Reg ||
      format === DecodeFormat.Reg_Imm ||
      format === DecodeFormat.Reg_Offset ||
      format === DecodeFormat.Reg_Reg_Offset ||
      format === DecodeFormat.CsrReg

  val rs2Valid =
    format === DecodeFormat.Reg_Reg ||
      format === DecodeFormat.Reg_Reg_Offset

  io.out.bits.rs1.bits.addr := rs1Addr
  io.out.bits.rs1.bits.rdata := resolveRegValue(rs1Valid, rs1Addr, rs1Data)
  io.out.bits.rs1.valid := rs1Valid
  io.out.bits.rs2.bits.addr := rs2Addr
  io.out.bits.rs2.bits.rdata := resolveRegValue(rs2Valid, rs2Addr, rs2Data)
  io.out.bits.rs2.valid := rs2Valid

  io.out.bits.rd := rdAddr

  io.out.bits.execData.pc := io.in.bits.pc
  io.out.bits.execData.imm := imm

  io.out.bits.execCtrl.aluOp := aluOp
  io.out.bits.execCtrl.aluSrcA := aluSrcA
  io.out.bits.execCtrl.aluSrcB := aluSrcB
  io.out.bits.execCtrl.wbSel := wbSel
  io.out.bits.execCtrl.branchType := branchType
  io.out.bits.execCtrl.isJump := isJump
  io.out.bits.execCtrl.isJalr := isJalr
  io.out.bits.execCtrl.sys.csrOp := csrOp
  io.out.bits.execCtrl.sys.csrAddr := csrAddr
  io.out.bits.execCtrl.sys.ecall := isEcall
  io.out.bits.execCtrl.sys.mret := isMret
  io.out.bits.execCtrl.sys.ebreak := isEbreak
  io.out.bits.execCtrl.sys.fencei := isFenceI

  io.out.bits.wbCtrl.wen := regWen

  io.out.bits.memCtrl.en := opcode === "b0000011".U || opcode === "b0100011".U
  io.out.bits.memCtrl.write := opcode === "b0100011".U
  io.out.bits.memCtrl.unsigned := memUnsigned
  io.out.bits.memCtrl.subop := subop

  if (enableTraceFields) {
    io.out.bits.retireTrace.get.pc := io.in.bits.pc
    io.out.bits.retireTrace.get.inst := io.in.bits.inst
    io.out.bits.retireTrace.get.icacheHit := true.B
    io.out.bits.retireTrace.get.dnpc := io.in.bits.pc + 4.U
    io.out.bits.retireTrace.get.regWrite.wen := io.regWrite.regWrite.wen
    io.out.bits.retireTrace.get.regWrite.rd := io.regWrite.regWrite.rd
    io.out.bits.retireTrace.get.regWrite.wdata := io.regWrite.regWrite.wdata
    io.out.bits.retireTrace.get.csrs := 0.U.asTypeOf(new CsrDebugBundle)
    instType.foreach(io.out.bits.retireTrace.get.instType := _)
  }

  io.out.valid := io.in.valid
  io.in.ready := io.out.ready
}
