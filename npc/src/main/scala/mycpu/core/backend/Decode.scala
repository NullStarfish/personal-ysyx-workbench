package mycpu.core.backend

import chisel3._
import chisel3.util._
import mycpu.common._
import mycpu.common.Instructions._
import mycpu.core.bundles._
import mycpu.core.components.{ImmGen, PerceptronBranchPredictor, RegFile}

class Decode extends Module {
  val io = IO(new Bundle {
    val in = Flipped(Decoupled(new FetchPacket))
    val out = Decoupled(new DecodePacket())
    val regWrite = Flipped(new WriteBackIO())
    val exForward = Input(new ForwardInfo)
    val memForward = Input(new ForwardInfo)
    val bpUpdate = Input(new BranchPredictUpdateBundle)
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
  val rs1Data = Mux(
    io.exForward.valid && rs1Addr =/= 0.U && io.exForward.addr === rs1Addr,
    io.exForward.data,
    Mux(io.memForward.valid && rs1Addr =/= 0.U && io.memForward.addr === rs1Addr, io.memForward.data, rs1Raw),
  )
  val rs2Data = Mux(
    io.exForward.valid && rs2Addr =/= 0.U && io.exForward.addr === rs2Addr,
    io.exForward.data,
    Mux(io.memForward.valid && rs2Addr =/= 0.U && io.memForward.addr === rs2Addr, io.memForward.data, rs2Raw),
  )

  val predictor = Module(new PerceptronBranchPredictor(entries = 32, historyLength = 8))
  predictor.io.pc := io.in.bits.pc
  predictor.io.update := io.bpUpdate.valid
  predictor.io.updatePc := io.bpUpdate.pc
  predictor.io.actualTaken := io.bpUpdate.actualTaken
  predictor.io.predictedTaken := io.bpUpdate.predictedTaken

  private def decodeFamily(opcode: UInt): UInt = MuxLookup(opcode, ExecFamily.Alu)(
    Seq(
      "b0110111".U -> ExecFamily.Upper,
      "b0010111".U -> ExecFamily.Upper,
      "b1101111".U -> ExecFamily.Jump,
      "b1100111".U -> ExecFamily.Jump,
      "b1100011".U -> ExecFamily.Branch,
      "b0000011".U -> ExecFamily.Mem,
      "b0100011".U -> ExecFamily.Mem,
      "b0010011".U -> ExecFamily.Alu,
      "b0110011".U -> ExecFamily.Alu,
      "b1110011".U -> ExecFamily.Csr,
    ),
  )

  private def decodeFormat(opcode: UInt, family: UInt, funct3: UInt): UInt = MuxLookup(opcode, DecodeFormat.None)(
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
        family === ExecFamily.Csr,
        Mux(
          opcode === "b1110011".U && funct3 === 0.U,
          DecodeFormat.Sys,
          Mux(funct3(2), DecodeFormat.CsrImm, DecodeFormat.CsrReg),
        ),
        DecodeFormat.None,
      ),
    ),
  )

  private def decodeOp(opcode: UInt, family: UInt, funct3: UInt, funct7: UInt, inst: UInt): UInt = {
    val decodedOp = WireDefault(ExecOp.Nop)
    switch(family) {
      is(ExecFamily.Upper) {
        decodedOp := Mux(opcode === "b0110111".U, ExecOp.Lui, ExecOp.Auipc)
      }
      is(ExecFamily.Jump) {
        decodedOp := Mux(opcode === "b1100111".U, ExecOp.Jalr, ExecOp.Jal)
      }
      is(ExecFamily.Branch) {
        switch(funct3) {
          is("b000".U) { decodedOp := ExecOp.Beq }
          is("b001".U) { decodedOp := ExecOp.Bne }
          is("b100".U) { decodedOp := ExecOp.Blt }
          is("b101".U) { decodedOp := ExecOp.Bge }
          is("b110".U) { decodedOp := ExecOp.Bltu }
          is("b111".U) { decodedOp := ExecOp.Bgeu }
        }
      }
      is(ExecFamily.Mem) {
        decodedOp := Mux(opcode === "b0100011".U, ExecOp.Store, ExecOp.Load)
      }
      is(ExecFamily.Alu) {
        when(opcode === "b0010011".U) {
          switch(funct3) {
            is("b000".U) { decodedOp := ExecOp.Add }
            is("b111".U) { decodedOp := ExecOp.And }
            is("b110".U) { decodedOp := ExecOp.Or }
            is("b100".U) { decodedOp := ExecOp.Xor }
            is("b010".U) { decodedOp := ExecOp.Slt }
            is("b011".U) { decodedOp := ExecOp.Sltu }
            is("b001".U) { decodedOp := ExecOp.Sll }
            is("b101".U) { decodedOp := Mux(funct7 === "b0100000".U, ExecOp.Sra, ExecOp.Srl) }
          }
        }.otherwise {
          switch(funct3) {
            is("b000".U) { decodedOp := Mux(funct7 === "b0100000".U, ExecOp.Sub, ExecOp.Add) }
            is("b111".U) { decodedOp := ExecOp.And }
            is("b110".U) { decodedOp := ExecOp.Or }
            is("b100".U) { decodedOp := ExecOp.Xor }
            is("b010".U) { decodedOp := ExecOp.Slt }
            is("b011".U) { decodedOp := ExecOp.Sltu }
            is("b001".U) { decodedOp := ExecOp.Sll }
            is("b101".U) { decodedOp := Mux(funct7 === "b0100000".U, ExecOp.Sra, ExecOp.Srl) }
          }
        }
      }
      is(ExecFamily.Csr) {
        when(inst === Instructions.ECALL.value.U || inst === Instructions.MRET.value.U || inst === Instructions.EBREAK.value.U) {
          decodedOp := ExecOp.Nop
        }.otherwise {
          switch(funct3) {
            is("b001".U) { decodedOp := ExecOp.CsrRw }
            is("b010".U) { decodedOp := ExecOp.CsrRs }
            is("b011".U) { decodedOp := ExecOp.CsrRc }
            is("b101".U) { decodedOp := ExecOp.CsrRw }
            is("b110".U) { decodedOp := ExecOp.CsrRs }
            is("b111".U) { decodedOp := ExecOp.CsrRc }
          }
        }
      }
    }
    decodedOp
  }

  val family = decodeFamily(opcode)
  val format = decodeFormat(opcode, family, funct3)
  val op = decodeOp(opcode, family, funct3, funct7, inst)
  val subop = WireDefault(ExecSubop.None)
  val lhs = WireDefault(0.U(XLEN.W))
  val rhs = WireDefault(0.U(XLEN.W))
  val offset = WireDefault(0.U(XLEN.W))
  val regWen = WireDefault(false.B)
  val memUnsigned = WireDefault(false.B)
  val csrAddr = WireDefault(inst(31, 20))
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

  switch(format) {
    is(DecodeFormat.RegReg) {
      lhs := rs1Data
      rhs := rs2Data
    }
    is(DecodeFormat.RegImm) {
      lhs := rs1Data
      rhs := immGen.io.out
    }
    is(DecodeFormat.PcImm) {
      lhs := Mux(op === ExecOp.Lui, 0.U, io.in.bits.pc)
      rhs := immGen.io.out
    }
    is(DecodeFormat.PcOffset) {
      lhs := io.in.bits.pc
      offset := immGen.io.out
    }
    is(DecodeFormat.RegOffset) {
      lhs := rs1Data
      offset := immGen.io.out
    }
    is(DecodeFormat.RegRegOffset) {
      lhs := rs1Data
      rhs := rs2Data
      offset := immGen.io.out
    }
    is(DecodeFormat.CsrReg) {
      rhs := rs1Data
    }
    is(DecodeFormat.CsrImm) {
      rhs := Cat(0.U((XLEN - 5).W), rs1Addr)
    }
  }

  switch(family) {
    is(ExecFamily.Mem) {
      regWen := op === ExecOp.Load && rdAddr =/= 0.U
      switch(funct3) {
        is("b000".U) { subop := ExecSubop.Byte }
        is("b001".U) { subop := ExecSubop.Half }
        is("b010".U) { subop := ExecSubop.Word }
        is("b100".U) { subop := ExecSubop.Byte; memUnsigned := true.B }
        is("b101".U) { subop := ExecSubop.Half; memUnsigned := true.B }
      }
    }
    is(ExecFamily.Alu) {
      regWen := rdAddr =/= 0.U
    }
    is(ExecFamily.Upper) {
      regWen := rdAddr =/= 0.U
    }
    is(ExecFamily.Jump) {
      regWen := rdAddr =/= 0.U
    }
    is(ExecFamily.Csr) {
      when(inst === Instructions.ECALL.value.U) {
        isEcall := true.B
      }.elsewhen(inst === Instructions.MRET.value.U) {
        isMret := true.B
      }.elsewhen(inst === Instructions.EBREAK.value.U) {
        isEbreak := true.B
      }.otherwise {
        regWen := rdAddr =/= 0.U
      }
    }
  }

  when(op === ExecOp.Store) {
    switch(funct3) {
      is("b000".U) { subop := ExecSubop.Byte }
      is("b001".U) { subop := ExecSubop.Half }
      is("b010".U) { subop := ExecSubop.Word }
    }
  }

  io.out.bits.data.pc := io.in.bits.pc
  io.out.bits.data.lhs := lhs
  io.out.bits.data.rhs := rhs
  io.out.bits.data.offset := offset
  io.out.bits.exec.family := family
  io.out.bits.exec.op := op
  io.out.bits.exec.subop := subop
  io.out.bits.wb.regWen := regWen
  io.out.bits.wb.rd := rdAddr
  io.out.bits.mem.valid := family === ExecFamily.Mem
  io.out.bits.mem.write := family === ExecFamily.Mem && op === ExecOp.Store
  io.out.bits.mem.unsigned := memUnsigned
  io.out.bits.mem.subop := subop
  io.out.bits.sys.csrAddr := csrAddr
  io.out.bits.sys.isEcall := isEcall
  io.out.bits.sys.isMret := isMret
  io.out.bits.sys.isEbreak := isEbreak
  io.out.bits.pred.predictedTaken := family === ExecFamily.Branch && predictor.io.predictTaken
  io.out.bits.retire.pc := io.in.bits.pc
  io.out.bits.retire.dnpc := io.in.bits.dnpc
  io.out.bits.retire.inst := io.in.bits.inst

  io.out.valid := io.in.valid
  io.in.ready := io.out.ready
}
