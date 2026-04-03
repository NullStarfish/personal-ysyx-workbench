package mycpu.pipeline

import HwOS.kernel._
import chisel3._
import chisel3.util._
import mycpu.common._

object InstFormat extends ChiselEnum {
  val Unknown = Value
  val R = Value
  val I = Value
  val S = Value
  val B = Value
  val U = Value
  val J = Value
}

object InstFamily extends ChiselEnum {
  val Unknown = Value
  val ALU = Value
  val LOAD = Value
  val STORE = Value
  val BRANCH = Value
  val JUMP = Value
  val UPPER = Value
  val CSR = Value
  val SYSTEM = Value
}

final class DecodeProcess(
    executeRef: ApiRef[ExecuteApiDecl],
    regfileRef: ApiRef[RegfileApiDecl],
    localName: String = "Decode",
)(implicit kernel: Kernel)
    extends HwProcess(localName) {

  private val decodedRs1Value = RegInit(0.U(XLEN.W))
  private val decodedRs2Value = RegInit(0.U(XLEN.W))

  val api: DecodeApiDecl = new DecodeApiDecl {
    def decode(inst: UInt): HwInline[Unit] = HwInline.thread(s"${name}_decode") { t =>
      val stepTag = s"${name}_decode_${System.identityHashCode(new Object())}"
      val exec = executeRef.get
      val regApi = regfileRef.get

      val format = decodeFormat(inst)
      val family = decodeFamily(inst, format)

      val rd = inst(11, 7)
      val rs1 = inst(19, 15)
      val rs2 = inst(24, 20)
      val funct3 = inst(14, 12)
      val funct7 = inst(31, 25)

      val immI = Cat(Fill(XLEN - 12, inst(31)), inst(31, 20))
      val immS = Cat(Fill(XLEN - 12, inst(31)), inst(31, 25), inst(11, 7))
      val immB = Cat(
        Fill(XLEN - 13, inst(31)),
        inst(31),
        inst(7),
        inst(30, 25),
        inst(11, 8),
        0.U(1.W),
      )
      val immU = Cat(inst(31, 12), 0.U(12.W))
      val immJ = Cat(
        Fill(XLEN - 21, inst(31)),
        inst(31),
        inst(19, 12),
        inst(20),
        inst(30, 21),
        0.U(1.W),
      )

      t.Step(s"${stepTag}_ReadOperands") {
        decodedRs1Value := SysCall.Inline(regApi.read(rs1))
        decodedRs2Value := SysCall.Inline(regApi.read(rs2))
      }

      t.Step(s"${stepTag}_Dispatch") {
        printf(
          p"[DECODE] inst=${Hexadecimal(inst)} format=${format.asUInt} family=${family.asUInt} rd=${Decimal(rd)} rs1=${Decimal(rs1)} rs2=${Decimal(rs2)} funct3=${Hexadecimal(funct3)} funct7=${Hexadecimal(funct7)}\n",
        )
        switch(family) {
          is(InstFamily.ALU) {
            when(format === InstFormat.I) {
              switch(funct3) {
                is("b000".U) {
                  SysCall.Inline(exec.add(rd, decodedRs1Value, immI))
                }
                is("b111".U) { SysCall.Inline(exec.and(rd, decodedRs1Value, immI)) }
                is("b110".U) { SysCall.Inline(exec.or(rd, decodedRs1Value, immI)) }
                is("b100".U) { SysCall.Inline(exec.xor(rd, decodedRs1Value, immI)) }
                is("b001".U) { SysCall.Inline(exec.sll(rd, decodedRs1Value, immI)) }
                is("b101".U) {
                  when(funct7 === "b0100000".U) {
                    SysCall.Inline(exec.sra(rd, decodedRs1Value, immI))
                  }.otherwise {
                    SysCall.Inline(exec.srl(rd, decodedRs1Value, immI))
                  }
                }
                is("b010".U) { SysCall.Inline(exec.slt(rd, decodedRs1Value, immI)) }
                is("b011".U) { SysCall.Inline(exec.sltu(rd, decodedRs1Value, immI)) }
              }
            }.otherwise {
              switch(funct3) {
                is("b000".U) {
                  when(funct7 === "b0100000".U) {
                    SysCall.Inline(exec.sub(rd, decodedRs1Value, decodedRs2Value))
                  }.otherwise {
                    SysCall.Inline(exec.add(rd, decodedRs1Value, decodedRs2Value))
                  }
                }
                is("b111".U) { SysCall.Inline(exec.and(rd, decodedRs1Value, decodedRs2Value)) }
                is("b110".U) { SysCall.Inline(exec.or(rd, decodedRs1Value, decodedRs2Value)) }
                is("b100".U) { SysCall.Inline(exec.xor(rd, decodedRs1Value, decodedRs2Value)) }
                is("b001".U) { SysCall.Inline(exec.sll(rd, decodedRs1Value, decodedRs2Value)) }
                is("b101".U) {
                  when(funct7 === "b0100000".U) {
                    SysCall.Inline(exec.sra(rd, decodedRs1Value, decodedRs2Value))
                  }.otherwise {
                    SysCall.Inline(exec.srl(rd, decodedRs1Value, decodedRs2Value))
                  }
                }
                is("b010".U) { SysCall.Inline(exec.slt(rd, decodedRs1Value, decodedRs2Value)) }
                is("b011".U) { SysCall.Inline(exec.sltu(rd, decodedRs1Value, decodedRs2Value)) }
              }
            }
          }

          is(InstFamily.LOAD) {
            switch(funct3) {
              is("b000".U) { SysCall.Inline(exec.loadByte(rd, decodedRs1Value, immI, false.B)) }
              is("b001".U) { SysCall.Inline(exec.loadHalf(rd, decodedRs1Value, immI, false.B)) }
              is("b010".U) { SysCall.Inline(exec.loadWord(rd, decodedRs1Value, immI)) }
              is("b100".U) { SysCall.Inline(exec.loadByte(rd, decodedRs1Value, immI, true.B)) }
              is("b101".U) { SysCall.Inline(exec.loadHalf(rd, decodedRs1Value, immI, true.B)) }
            }
          }

          is(InstFamily.STORE) {
            switch(funct3) {
              is("b000".U) { SysCall.Inline(exec.storeByte(decodedRs1Value, immS, decodedRs2Value)) }
              is("b001".U) { SysCall.Inline(exec.storeHalf(decodedRs1Value, immS, decodedRs2Value)) }
              is("b010".U) { SysCall.Inline(exec.storeWord(decodedRs1Value, immS, decodedRs2Value)) }
            }
          }

          is(InstFamily.BRANCH) {
            switch(funct3) {
              is("b000".U) { SysCall.Inline(exec.eq(decodedRs1Value, decodedRs2Value, immB.asSInt)) }
              is("b001".U) { SysCall.Inline(exec.ne(decodedRs1Value, decodedRs2Value, immB.asSInt)) }
              is("b100".U) { SysCall.Inline(exec.lt(decodedRs1Value, decodedRs2Value, immB.asSInt)) }
              is("b110".U) { SysCall.Inline(exec.ltu(decodedRs1Value, decodedRs2Value, immB.asSInt)) }
            }
          }

          is(InstFamily.JUMP) {
            SysCall.Inline(exec.writeReg(rd, 0.U(XLEN.W)))
            when(inst(6, 0) === "b1101111".U) {
              SysCall.Inline(exec.redirectRelative(immJ.asSInt))
            }.otherwise {
              SysCall.Inline(exec.redirect(immI))
            }
          }

          is(InstFamily.UPPER) {
            when(inst(6, 0) === "b0110111".U) {
              SysCall.Inline(exec.writeReg(rd, immU))
            }.otherwise {
              SysCall.Inline(exec.add(rd, 0.U(XLEN.W), immU))
            }
          }

          is(InstFamily.CSR) {
            val csrSrc = Mux(funct3(2), rs1.pad(XLEN), decodedRs1Value)
            switch(funct3) {
              is("b001".U) { SysCall.Inline(exec.csrRw(rd, inst(31, 20), csrSrc)) }
              is("b010".U) { SysCall.Inline(exec.csrRs(rd, inst(31, 20), csrSrc)) }
              is("b011".U) { SysCall.Inline(exec.csrRc(rd, inst(31, 20), csrSrc)) }
              is("b101".U) { SysCall.Inline(exec.csrRw(rd, inst(31, 20), csrSrc)) }
              is("b110".U) { SysCall.Inline(exec.csrRs(rd, inst(31, 20), csrSrc)) }
              is("b111".U) { SysCall.Inline(exec.csrRc(rd, inst(31, 20), csrSrc)) }
            }
          }

          is(InstFamily.SYSTEM) {
            printf(p"[DECODE] system instruction inst=${Hexadecimal(inst)}\n")
          }
        }
      }
      SysCall.Return()
    }
    override def decodeInst(inst: UInt): HwInline[Unit] = decode(inst)
  }

  def RequestDecodeApi(): HwInline[DecodeApiDecl] = HwInline.bindings(s"${name}_decode_api") { _ =>
    api
  }

  def decodeInst(inst: UInt): HwInline[Unit] = api.decodeInst(inst)

  override def entry(): Unit = {}

  private def decodeFormat(inst: UInt): InstFormat.Type = {
    val opcode = inst(6, 0)
    val fmt = WireDefault(InstFormat.Unknown)

    switch(opcode) {
      is("b0110011".U) { fmt := InstFormat.R }
      is("b0010011".U) { fmt := InstFormat.I }
      is("b0000011".U) { fmt := InstFormat.I }
      is("b1100111".U) { fmt := InstFormat.I }
      is("b1110011".U) { fmt := InstFormat.I }

      is("b0100011".U) { fmt := InstFormat.S }
      is("b1100011".U) { fmt := InstFormat.B }
      is("b0110111".U) { fmt := InstFormat.U }
      is("b0010111".U) { fmt := InstFormat.U }
      is("b1101111".U) { fmt := InstFormat.J }
    }

    fmt
  }

  private def decodeFamily(inst: UInt, format: InstFormat.Type): InstFamily.Type = {
    val opcode = inst(6, 0)
    val family = WireDefault(InstFamily.Unknown)

    switch(opcode) {
      is("b0110011".U) { family := InstFamily.ALU }
      is("b0010011".U) { family := InstFamily.ALU }
      is("b0000011".U) { family := InstFamily.LOAD }
      is("b0100011".U) { family := InstFamily.STORE }
      is("b1100011".U) { family := InstFamily.BRANCH }
      is("b1101111".U) { family := InstFamily.JUMP }
      is("b1100111".U) { family := InstFamily.JUMP }
      is("b0110111".U) { family := InstFamily.UPPER }
      is("b0010111".U) { family := InstFamily.UPPER }
      is("b1110011".U) {
        when(inst(14, 12) === 0.U) {
          family := InstFamily.SYSTEM
        }.otherwise {
          family := InstFamily.CSR
        }
      }
    }

    family
  }
}
