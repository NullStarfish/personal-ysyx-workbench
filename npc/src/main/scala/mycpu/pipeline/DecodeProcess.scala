package mycpu.pipeline

import HwOS.kernel._
import HwOS.stdlib.sync._
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

  private val decodeSlotLock = spawn(new MutexProcess(1, "DecodeSlotLock"))
  private val decodeWorker = createThread("DecodeWorker")
  private val decodeInstReg = RegInit(0.U(32.W))
  private val decodeCompleted = RegInit(false.B)

  override def entry(): Unit = {
    decodeWorker.entry {
      val exec = SysCall.Inline(RequestExecuteApi())
      val regApi = SysCall.Inline(RequestRegfileApi())

      decodeWorker.Step("DecodeDispatch") {
        val format = decodeFormat(decodeInstReg)
        val family = decodeFamily(decodeInstReg, format)

        val rd = decodeInstReg(11, 7)
        val rs1 = decodeInstReg(19, 15)
        val rs2 = decodeInstReg(24, 20)
        val funct3 = decodeInstReg(14, 12)
        val funct7 = decodeInstReg(31, 25)

        val immI = Cat(Fill(XLEN - 12, decodeInstReg(31)), decodeInstReg(31, 20))
        val immS = Cat(Fill(XLEN - 12, decodeInstReg(31)), decodeInstReg(31, 25), decodeInstReg(11, 7))
        val immB = Cat(
          Fill(XLEN - 13, decodeInstReg(31)),
          decodeInstReg(31),
          decodeInstReg(7),
          decodeInstReg(30, 25),
          decodeInstReg(11, 8),
          0.U(1.W),
        )
        val immU = Cat(decodeInstReg(31, 12), 0.U(12.W))
        val immJ = Cat(
          Fill(XLEN - 21, decodeInstReg(31)),
          decodeInstReg(31),
          decodeInstReg(19, 12),
          decodeInstReg(20),
          decodeInstReg(30, 21),
          0.U(1.W),
        )

        val rs1Value = SysCall.Inline(regApi.read(rs1))
        val rs2Value = SysCall.Inline(regApi.read(rs2))

        printf(
          p"[DECODE] inst=${Hexadecimal(decodeInstReg)} format=${format.asUInt} family=${family.asUInt} rd=${Decimal(rd)} rs1=${Decimal(rs1)} rs2=${Decimal(rs2)} funct3=${Hexadecimal(funct3)} funct7=${Hexadecimal(funct7)}\n",
        )
        switch(family) {
          is(InstFamily.ALU) {
            when(format === InstFormat.I) {
              switch(funct3) {
                is("b000".U) { SysCall.Inline(exec.add(rd, rs1Value, immI)) }
                is("b111".U) { SysCall.Inline(exec.and(rd, rs1Value, immI)) }
                is("b110".U) { SysCall.Inline(exec.or(rd, rs1Value, immI)) }
                is("b100".U) { SysCall.Inline(exec.xor(rd, rs1Value, immI)) }
                is("b001".U) { SysCall.Inline(exec.sll(rd, rs1Value, immI)) }
                is("b101".U) {
                  when(funct7 === "b0100000".U) {
                    SysCall.Inline(exec.sra(rd, rs1Value, immI))
                  }.otherwise {
                    SysCall.Inline(exec.srl(rd, rs1Value, immI))
                  }
                }
                is("b010".U) { SysCall.Inline(exec.slt(rd, rs1Value, immI)) }
                is("b011".U) { SysCall.Inline(exec.sltu(rd, rs1Value, immI)) }
              }
            }.otherwise {
              switch(funct3) {
                is("b000".U) {
                  when(funct7 === "b0100000".U) {
                    SysCall.Inline(exec.sub(rd, rs1Value, rs2Value))
                  }.otherwise {
                    SysCall.Inline(exec.add(rd, rs1Value, rs2Value))
                  }
                }
                is("b111".U) { SysCall.Inline(exec.and(rd, rs1Value, rs2Value)) }
                is("b110".U) { SysCall.Inline(exec.or(rd, rs1Value, rs2Value)) }
                is("b100".U) { SysCall.Inline(exec.xor(rd, rs1Value, rs2Value)) }
                is("b001".U) { SysCall.Inline(exec.sll(rd, rs1Value, rs2Value)) }
                is("b101".U) {
                  when(funct7 === "b0100000".U) {
                    SysCall.Inline(exec.sra(rd, rs1Value, rs2Value))
                  }.otherwise {
                    SysCall.Inline(exec.srl(rd, rs1Value, rs2Value))
                  }
                }
                is("b010".U) { SysCall.Inline(exec.slt(rd, rs1Value, rs2Value)) }
                is("b011".U) { SysCall.Inline(exec.sltu(rd, rs1Value, rs2Value)) }
              }
            }
          }

          is(InstFamily.LOAD) {
            switch(funct3) {
              is("b000".U) { SysCall.Inline(exec.loadByte(rd, rs1Value, immI, false.B)) }
              is("b001".U) { SysCall.Inline(exec.loadHalf(rd, rs1Value, immI, false.B)) }
              is("b010".U) { SysCall.Inline(exec.loadWord(rd, rs1Value, immI)) }
              is("b100".U) { SysCall.Inline(exec.loadByte(rd, rs1Value, immI, true.B)) }
              is("b101".U) { SysCall.Inline(exec.loadHalf(rd, rs1Value, immI, true.B)) }
            }
          }

          is(InstFamily.STORE) {
            switch(funct3) {
              is("b000".U) { SysCall.Inline(exec.storeByte(rs1Value, immS, rs2Value)) }
              is("b001".U) { SysCall.Inline(exec.storeHalf(rs1Value, immS, rs2Value)) }
              is("b010".U) { SysCall.Inline(exec.storeWord(rs1Value, immS, rs2Value)) }
            }
          }

          is(InstFamily.BRANCH) {
            switch(funct3) {
              is("b000".U) { SysCall.Inline(exec.eq(rs1Value, rs2Value, immB.asSInt)) }
              is("b001".U) { SysCall.Inline(exec.ne(rs1Value, rs2Value, immB.asSInt)) }
              is("b100".U) { SysCall.Inline(exec.lt(rs1Value, rs2Value, immB.asSInt)) }
              is("b110".U) { SysCall.Inline(exec.ltu(rs1Value, rs2Value, immB.asSInt)) }
            }
          }

          is(InstFamily.JUMP) {
            SysCall.Inline(exec.writeReg(rd, 0.U(XLEN.W)))
            when(decodeInstReg(6, 0) === "b1101111".U) {
              SysCall.Inline(exec.redirectRelative(immJ.asSInt))
            }.otherwise {
              SysCall.Inline(exec.redirect(immI))
            }
          }

          is(InstFamily.UPPER) {
            when(decodeInstReg(6, 0) === "b0110111".U) {
              SysCall.Inline(exec.writeReg(rd, immU))
            }.otherwise {
              SysCall.Inline(exec.add(rd, 0.U(XLEN.W), immU))
            }
          }

          is(InstFamily.CSR) {
            val csrSrc = Mux(funct3(2), rs1.pad(XLEN), rs1Value)
            switch(funct3) {
              is("b001".U) { SysCall.Inline(exec.csrRw(rd, decodeInstReg(31, 20), csrSrc)) }
              is("b010".U) { SysCall.Inline(exec.csrRs(rd, decodeInstReg(31, 20), csrSrc)) }
              is("b011".U) { SysCall.Inline(exec.csrRc(rd, decodeInstReg(31, 20), csrSrc)) }
              is("b101".U) { SysCall.Inline(exec.csrRw(rd, decodeInstReg(31, 20), csrSrc)) }
              is("b110".U) { SysCall.Inline(exec.csrRs(rd, decodeInstReg(31, 20), csrSrc)) }
              is("b111".U) { SysCall.Inline(exec.csrRc(rd, decodeInstReg(31, 20), csrSrc)) }
            }
          }

          is(InstFamily.SYSTEM) {
            printf(p"[DECODE] system instruction inst=${Hexadecimal(decodeInstReg)}\n")
          }
        }
      }

      decodeWorker.Step("Finish") {
        decodeCompleted := true.B
      }
      SysCall.Return()
    }
  }

  val api: DecodeApiDecl = new DecodeApiDecl {
    def decode(inst: UInt): HwInline[Unit] = HwInline.thread(s"${name}_decode") { t =>
      val stepTag = s"${name}_decode_${System.identityHashCode(new Object())}"
      val lock = SysCall.Inline(decodeSlotLock.RequestLease(0))

      t.Step(s"${stepTag}_AcquireSlot") {
        SysCall.Inline(lock.Acquire())
      }

      t.Step(s"${stepTag}_Issue") {
        decodeInstReg := inst
        decodeCompleted := false.B
        SysCall.Inline(SysCall.start(decodeWorker))
      }

      t.Step(s"${stepTag}_WaitDone") {
        t.waitCondition(decodeCompleted)
      }

      t.Step(s"${stepTag}_ReleaseSlot") {
        SysCall.Inline(lock.Release())
      }
      SysCall.Return()
      ()
    }
    override def decodeInst(inst: UInt): HwInline[Unit] = decode(inst)
  }

  def RequestDecodeApi(): HwInline[DecodeApiDecl] = HwInline.bindings(s"${name}_decode_api") { _ =>
    api
  }

  def decodeInst(inst: UInt): HwInline[Unit] = api.decodeInst(inst)

  private def RequestExecuteApi(): HwInline[ExecuteApiDecl] = HwInline.bindings(s"${name}_execute_link") { _ =>
    executeRef.get
  }

  private def RequestRegfileApi(): HwInline[RegfileApiDecl] = HwInline.bindings(s"${name}_regfile_link") { _ =>
    regfileRef.get
  }

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
