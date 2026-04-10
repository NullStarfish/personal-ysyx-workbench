package mycpu.core.backend

import chisel3._
import chisel3.util._
import mycpu.common._
import mycpu.common.Instructions._
import mycpu.core.bundles._
import mycpu.core.components.{ImmGen, RegFile}

class Decode extends Module {
  val io = IO(new Bundle {
    val in = Flipped(Decoupled(new FetchPacket))
    val out = Decoupled(new DecodePacket())
    val regWrite = Flipped(new WriteBackIO())
    val exForward = Input(new ForwardInfo)
    val memForward = Input(new ForwardInfo)
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

  val family = WireDefault(ExecFamily.Alu)
  val op = WireDefault(ExecOp.Nop)
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
    is("b0110111".U) { // LUI
      immType := ImmType.U
      family := ExecFamily.Upper
      op := ExecOp.Lui
      lhs := 0.U
      rhs := immGen.io.out
      regWen := rdAddr =/= 0.U
    }
    is("b0010111".U) { // AUIPC
      immType := ImmType.U
      family := ExecFamily.Upper
      op := ExecOp.Auipc
      lhs := io.in.bits.pc
      rhs := immGen.io.out
      regWen := rdAddr =/= 0.U
    }
    is("b1101111".U) { // JAL
      immType := ImmType.J
      family := ExecFamily.Jump
      op := ExecOp.Jal
      lhs := io.in.bits.pc
      rhs := 0.U
      offset := immGen.io.out
      regWen := rdAddr =/= 0.U
    }
    is("b1100111".U) { // JALR
      immType := ImmType.I
      family := ExecFamily.Jump
      op := ExecOp.Jalr
      lhs := rs1Data
      rhs := 0.U
      offset := immGen.io.out
      regWen := rdAddr =/= 0.U
    }
    is("b1100011".U) { // BRANCH
      immType := ImmType.B
      family := ExecFamily.Branch
      lhs := rs1Data
      rhs := rs2Data
      offset := immGen.io.out
      switch(funct3) {
        is("b000".U) { op := ExecOp.Beq }
        is("b001".U) { op := ExecOp.Bne }
        is("b100".U) { op := ExecOp.Blt }
        is("b101".U) { op := ExecOp.Bge }
        is("b110".U) { op := ExecOp.Bltu }
        is("b111".U) { op := ExecOp.Bgeu }
      }
    }
    is("b0000011".U) { // LOAD
      immType := ImmType.I
      family := ExecFamily.Mem
      op := ExecOp.Load
      lhs := rs1Data
      rhs := 0.U
      offset := immGen.io.out
      regWen := rdAddr =/= 0.U
      switch(funct3) {
        is("b000".U) { subop := ExecSubop.Byte }
        is("b001".U) { subop := ExecSubop.Half }
        is("b010".U) { subop := ExecSubop.Word }
        is("b100".U) { subop := ExecSubop.Byte; memUnsigned := true.B }
        is("b101".U) { subop := ExecSubop.Half; memUnsigned := true.B }
      }
    }
    is("b0100011".U) { // STORE
      immType := ImmType.S
      family := ExecFamily.Mem
      op := ExecOp.Store
      lhs := rs1Data
      rhs := rs2Data
      offset := immGen.io.out
      switch(funct3) {
        is("b000".U) { subop := ExecSubop.Byte }
        is("b001".U) { subop := ExecSubop.Half }
        is("b010".U) { subop := ExecSubop.Word }
      }
    }
    is("b0010011".U) { // OP-IMM
      immType := ImmType.I
      family := ExecFamily.Alu
      lhs := rs1Data
      rhs := immGen.io.out
      regWen := rdAddr =/= 0.U
      switch(funct3) {
        is("b000".U) { op := ExecOp.Add }
        is("b111".U) { op := ExecOp.And }
        is("b110".U) { op := ExecOp.Or }
        is("b100".U) { op := ExecOp.Xor }
        is("b010".U) { op := ExecOp.Slt }
        is("b011".U) { op := ExecOp.Sltu }
        is("b001".U) { op := ExecOp.Sll }
        is("b101".U) { op := Mux(funct7 === "b0100000".U, ExecOp.Sra, ExecOp.Srl) }
      }
    }
    is("b0110011".U) { // OP
      immType := ImmType.Z
      family := ExecFamily.Alu
      lhs := rs1Data
      rhs := rs2Data
      regWen := rdAddr =/= 0.U
      switch(funct3) {
        is("b000".U) { op := Mux(funct7 === "b0100000".U, ExecOp.Sub, ExecOp.Add) }
        is("b111".U) { op := ExecOp.And }
        is("b110".U) { op := ExecOp.Or }
        is("b100".U) { op := ExecOp.Xor }
        is("b010".U) { op := ExecOp.Slt }
        is("b011".U) { op := ExecOp.Sltu }
        is("b001".U) { op := ExecOp.Sll }
        is("b101".U) { op := Mux(funct7 === "b0100000".U, ExecOp.Sra, ExecOp.Srl) }
      }
    }
    is("b1110011".U) { // SYSTEM / CSR
      when(inst === Instructions.ECALL.value.U) {
        family := ExecFamily.Csr
        isEcall := true.B
      }.elsewhen(inst === Instructions.MRET.value.U) {
        family := ExecFamily.Csr
        isMret := true.B
      }.elsewhen(inst === Instructions.EBREAK.value.U) {
        family := ExecFamily.Csr
        isEbreak := true.B
      }.otherwise {
        immType := ImmType.I
        family := ExecFamily.Csr
        regWen := rdAddr =/= 0.U
        rhs := Mux(funct3(2), Cat(0.U((XLEN - 5).W), rs1Addr), rs1Data)
        switch(funct3) {
          is("b001".U) { op := ExecOp.CsrRw }
          is("b010".U) { op := ExecOp.CsrRs }
          is("b011".U) { op := ExecOp.CsrRc }
          is("b101".U) { op := ExecOp.CsrRw }
          is("b110".U) { op := ExecOp.CsrRs }
          is("b111".U) { op := ExecOp.CsrRc }
        }
      }
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

  io.out.valid := io.in.valid
  io.in.ready := io.out.ready
}
