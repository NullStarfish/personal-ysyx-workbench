package mycpu.core.backend

import chisel3._
import chisel3.util._
import mycpu.common._
import mycpu.common.Instructions._
import mycpu.core.bundles._
import mycpu.core.components.{GShareBranchPredictor, ImmGen, RegFile}

class Decode(enableTraceFields: Boolean = ENABLE_TRACE_FIELDS) extends Module {
  val io = IO(new Bundle {
    val in = Flipped(Decoupled(new FetchPacket))
    val out = Decoupled(new DecodePacket(enableTraceFields))
    val regWrite = Flipped(new WriteBackIO())
    val bpUpdate = Input(new BranchPredictUpdateBundle)
    val debug_regs = Output(Vec(32, UInt(XLEN.W)))
    val hazard = Output(new Bundle {
      val rs1Used = Bool()
      val rs2Used = Bool()
      val rs1Addr = UInt(5.W)
      val rs2Addr = UInt(5.W)
    })
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
  regFile.io.wen := io.regWrite.wen
  regFile.io.waddr := io.regWrite.addr
  regFile.io.wdata := io.regWrite.data
  io.debug_regs := regFile.io.debug_regs

  val immType = Wire(ImmType())
  val immGen = Module(new ImmGen)
  immGen.io.inst := inst
  immGen.io.sel := immType

  val rs1Raw = regFile.io.rdata1
  val rs2Raw = regFile.io.rdata2

  val predictor = if (ENABLE_BRANCH_PREDICTOR) {
    Some(Module(new GShareBranchPredictor(entries = 32, historyLength = 5)))
  } else {
    None
  }
  predictor.foreach { p =>
    p.io.pc := io.in.bits.pc
    p.io.update := io.bpUpdate.valid
    p.io.updateIndex := io.bpUpdate.index
    p.io.actualTaken := io.bpUpdate.actualTaken
    p.io.predictedTaken := io.bpUpdate.predictedTaken
  }

  private def decodeFormat(opcode: UInt, funct3: UInt): UInt = MuxLookup(opcode, DecodeFormat.None)(
    Seq(
      "b0110111".U -> DecodeFormat.PcImm,
      "b0010111".U -> DecodeFormat.PcImm,
      "b1101111".U -> DecodeFormat.PcOffset,
      "b1100111".U -> DecodeFormat.RegOffset,
      "b1100011".U -> DecodeFormat.RegRegOffset,
      "b0000011".U -> DecodeFormat.RegOffset,
      "b0100011".U -> DecodeFormat.RegRegOffset,
      "b0010011".U -> DecodeFormat.RegImm,
      "b0110011".U -> DecodeFormat.RegReg,
      "b1110011".U -> Mux(
        opcode === "b1110011".U && funct3 === 0.U,
        DecodeFormat.Sys,
        Mux(funct3(2), DecodeFormat.CsrImm, DecodeFormat.CsrReg),
      ),
    ),
  )

  val format = decodeFormat(opcode, funct3)
  val subop = WireDefault(ExecSubop.None)
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
      regWen := rdAddr =/= 0.U
      aluOp := ALUOp.ADD
      aluSrcA := ALUSrcA.Pc
      aluSrcB := ALUSrcB.Imm
      imm := immGen.io.out
      wbSel := WBSel.PcPlus4
      isJump := true.B
    }
    is("b1100111".U) { // jalr
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
      regWen := rdAddr =/= 0.U
      aluOp := ALUOp.ADD
      aluSrcA := ALUSrcA.Rs1
      aluSrcB := ALUSrcB.Imm
      imm := immGen.io.out
      switch(funct3) {
        is("b000".U) { subop := ExecSubop.Byte }
        is("b001".U) { subop := ExecSubop.Half }
        is("b010".U) { subop := ExecSubop.Word }
        is("b100".U) { subop := ExecSubop.Byte; memUnsigned := true.B }
        is("b101".U) { subop := ExecSubop.Half; memUnsigned := true.B }
      }
    }
    is("b0100011".U) { // store
      aluOp := ALUOp.ADD
      aluSrcA := ALUSrcA.Rs1
      aluSrcB := ALUSrcB.Imm
      imm := immGen.io.out
      switch(funct3) {
        is("b000".U) { subop := ExecSubop.Byte }
        is("b001".U) { subop := ExecSubop.Half }
        is("b010".U) { subop := ExecSubop.Word }
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
  }

  io.hazard.rs1Addr := rs1Addr
  io.hazard.rs2Addr := rs2Addr
  io.hazard.rs1Used :=
    format === DecodeFormat.RegReg ||
      format === DecodeFormat.RegImm ||
      format === DecodeFormat.RegOffset ||
      format === DecodeFormat.RegRegOffset ||
      format === DecodeFormat.CsrReg
  io.hazard.rs2Used :=
    format === DecodeFormat.RegReg ||
      format === DecodeFormat.RegRegOffset

  io.out.bits.data.pc := io.in.bits.pc
  io.out.bits.data.rs1 := rs1Data
  io.out.bits.data.rs2 := rs2Data
  io.out.bits.data.imm := imm
  io.out.bits.bypass.rs1Addr := rs1Addr
  io.out.bits.bypass.rs2Addr := rs2Addr
  io.out.bits.exec.aluOp := aluOp
  io.out.bits.exec.aluSrcA := aluSrcA
  io.out.bits.exec.aluSrcB := aluSrcB
  io.out.bits.exec.wbSel := wbSel
  io.out.bits.exec.branchType := branchType
  io.out.bits.exec.isJump := isJump
  io.out.bits.exec.isJalr := isJalr
  io.out.bits.wb.regWen := regWen
  io.out.bits.wb.rd := rdAddr
  io.out.bits.mem.valid := opcode === "b0000011".U || opcode === "b0100011".U
  io.out.bits.mem.write := opcode === "b0100011".U
  io.out.bits.mem.unsigned := memUnsigned
  io.out.bits.mem.subop := subop
  io.out.bits.sys.csrOp := csrOp
  io.out.bits.sys.csrAddr := csrAddr
  io.out.bits.sys.isEcall := isEcall
  io.out.bits.sys.isMret := isMret
  io.out.bits.sys.isEbreak := isEbreak
  io.out.bits.pred.predictedTaken := (branchType =/= BranchType.None) && predictor.map(_.io.predictTaken).getOrElse(false.B)
  io.out.bits.pred.index := predictor.map(_.io.predictIndex).getOrElse(0.U)
  if (enableTraceFields) {
    io.out.bits.trace.get.pc := io.in.bits.pc
    io.out.bits.trace.get.inst := io.in.bits.inst
    io.out.bits.trace.get.dnpc := io.in.bits.pc + 4.U
    io.out.bits.trace.get.regWen := false.B
    io.out.bits.trace.get.rd := 0.U
    io.out.bits.trace.get.data := 0.U
    io.out.bits.trace.get.ifValid := io.in.valid
    io.out.bits.trace.get.idValid := io.in.valid
    io.out.bits.trace.get.exValid := false.B
    io.out.bits.trace.get.memValid := false.B
    io.out.bits.trace.get.branchResolved := false.B
    io.out.bits.trace.get.branchCorrect := false.B
    io.out.bits.trace.get.redirectValid := false.B
    io.out.bits.trace.get.redirectTarget := 0.U
    io.out.bits.trace.get.actualTaken := false.B
    io.out.bits.trace.get.predictedTaken := false.B
  }

  io.out.valid := io.in.valid
  io.in.ready := io.out.ready
}
