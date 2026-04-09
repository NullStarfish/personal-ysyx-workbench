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
  private val decodePcReg = RegInit(0.U(XLEN.W))
  private val decodeInstReg = RegInit(0.U(32.W))
  private val decodeCompleted = RegInit(false.B)
  private val familyReg = RegInit(InstFamily.Unknown)
  private val formatReg = RegInit(InstFormat.Unknown)
  private val rdReg = RegInit(0.U(5.W))
  private val rs1ValueReg = RegInit(0.U(XLEN.W))
  private val rs2ValueReg = RegInit(0.U(XLEN.W))
  private val funct3Reg = RegInit(0.U(3.W))
  private val funct7Reg = RegInit(0.U(7.W))
  private val immIReg = RegInit(0.U(XLEN.W))
  private val immSReg = RegInit(0.U(XLEN.W))
  private val immBReg = RegInit(0.U(XLEN.W))
  private val immUReg = RegInit(0.U(XLEN.W))
  private val immJReg = RegInit(0.U(XLEN.W))
  private val reserveRdReg = RegInit(0.U(5.W))
  private val wbTokenReg = RegInit(0.U(4.W))

  override def entry(): Unit = {
    decodeWorker.entry {
      val exec = SysCall.Inline(RequestExecuteApi())
      val regApi = SysCall.Inline(RequestRegfileApi())

      decodeWorker.Step("Start") {
        decodeWorker.jump(decodeWorker.stepRef("DecodeDispatch"))
      }

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
          p"[DECODE] pc=${Hexadecimal(decodePcReg)} inst=${Hexadecimal(decodeInstReg)} format=${format.asUInt} family=${family.asUInt} rd=${Decimal(rd)} rs1=${Decimal(rs1)} rs2=${Decimal(rs2)} funct3=${Hexadecimal(funct3)} funct7=${Hexadecimal(funct7)}\n",
        )
        val willWriteRd =
          (family === InstFamily.ALU) ||
            (family === InstFamily.LOAD) ||
            (family === InstFamily.JUMP) ||
            (family === InstFamily.UPPER) ||
            (family === InstFamily.CSR)
        formatReg := format
        familyReg := family
        rdReg := rd
        rs1ValueReg := rs1Value
        rs2ValueReg := rs2Value
        funct3Reg := funct3
        funct7Reg := funct7
        immIReg := immI
        immSReg := immS
        immBReg := immB
        immUReg := immU
        immJReg := immJ
        reserveRdReg := Mux(willWriteRd && rd =/= 0.U, rd, 0.U)
      }

      val wbToken = SysCall.Inline(regApi.reserve(reserveRdReg))

      decodeWorker.Step("LatchToken") {
        wbTokenReg := wbToken
      }

      decodeWorker.Step("DispatchExecute") {
        switch(familyReg) {
          is(InstFamily.ALU) {
            when(formatReg === InstFormat.I) {
              switch(funct3Reg) {
                is("b000".U) { SysCall.Inline(exec.add(rdReg, wbTokenReg, rs1ValueReg, immIReg)) }
                is("b111".U) { SysCall.Inline(exec.and(rdReg, wbTokenReg, rs1ValueReg, immIReg)) }
                is("b110".U) { SysCall.Inline(exec.or(rdReg, wbTokenReg, rs1ValueReg, immIReg)) }
                is("b100".U) { SysCall.Inline(exec.xor(rdReg, wbTokenReg, rs1ValueReg, immIReg)) }
                is("b001".U) { SysCall.Inline(exec.sll(rdReg, wbTokenReg, rs1ValueReg, immIReg)) }
                is("b101".U) {
                  when(funct7Reg === "b0100000".U) {
                    SysCall.Inline(exec.sra(rdReg, wbTokenReg, rs1ValueReg, immIReg))
                  }.otherwise {
                    SysCall.Inline(exec.srl(rdReg, wbTokenReg, rs1ValueReg, immIReg))
                  }
                }
                is("b010".U) { SysCall.Inline(exec.slt(rdReg, wbTokenReg, rs1ValueReg, immIReg)) }
                is("b011".U) { SysCall.Inline(exec.sltu(rdReg, wbTokenReg, rs1ValueReg, immIReg)) }
              }
            }.otherwise {
              switch(funct3Reg) {
                is("b000".U) {
                  when(funct7Reg === "b0100000".U) {
                    SysCall.Inline(exec.sub(rdReg, wbTokenReg, rs1ValueReg, rs2ValueReg))
                  }.otherwise {
                    SysCall.Inline(exec.add(rdReg, wbTokenReg, rs1ValueReg, rs2ValueReg))
                  }
                }
                is("b111".U) { SysCall.Inline(exec.and(rdReg, wbTokenReg, rs1ValueReg, rs2ValueReg)) }
                is("b110".U) { SysCall.Inline(exec.or(rdReg, wbTokenReg, rs1ValueReg, rs2ValueReg)) }
                is("b100".U) { SysCall.Inline(exec.xor(rdReg, wbTokenReg, rs1ValueReg, rs2ValueReg)) }
                is("b001".U) { SysCall.Inline(exec.sll(rdReg, wbTokenReg, rs1ValueReg, rs2ValueReg)) }
                is("b101".U) {
                  when(funct7Reg === "b0100000".U) {
                    SysCall.Inline(exec.sra(rdReg, wbTokenReg, rs1ValueReg, rs2ValueReg))
                  }.otherwise {
                    SysCall.Inline(exec.srl(rdReg, wbTokenReg, rs1ValueReg, rs2ValueReg))
                  }
                }
                is("b010".U) { SysCall.Inline(exec.slt(rdReg, wbTokenReg, rs1ValueReg, rs2ValueReg)) }
                is("b011".U) { SysCall.Inline(exec.sltu(rdReg, wbTokenReg, rs1ValueReg, rs2ValueReg)) }
              }
            }
          }

          is(InstFamily.LOAD) {
            switch(funct3Reg) {
              is("b000".U) { SysCall.Inline(exec.loadByte(rdReg, wbTokenReg, rs1ValueReg, immIReg, false.B)) }
              is("b001".U) { SysCall.Inline(exec.loadHalf(rdReg, wbTokenReg, rs1ValueReg, immIReg, false.B)) }
              is("b010".U) { SysCall.Inline(exec.loadWord(rdReg, wbTokenReg, rs1ValueReg, immIReg)) }
              is("b100".U) { SysCall.Inline(exec.loadByte(rdReg, wbTokenReg, rs1ValueReg, immIReg, true.B)) }
              is("b101".U) { SysCall.Inline(exec.loadHalf(rdReg, wbTokenReg, rs1ValueReg, immIReg, true.B)) }
            }
          }

          is(InstFamily.STORE) {
            switch(funct3Reg) {
              is("b000".U) { SysCall.Inline(exec.storeByte(rs1ValueReg, immSReg, rs2ValueReg)) }
              is("b001".U) { SysCall.Inline(exec.storeHalf(rs1ValueReg, immSReg, rs2ValueReg)) }
              is("b010".U) { SysCall.Inline(exec.storeWord(rs1ValueReg, immSReg, rs2ValueReg)) }
            }
          }

          is(InstFamily.BRANCH) {
            switch(funct3Reg) {
              is("b000".U) { SysCall.Inline(exec.eq(rs1ValueReg, rs2ValueReg, decodePcReg, immBReg)) }
              is("b001".U) { SysCall.Inline(exec.ne(rs1ValueReg, rs2ValueReg, decodePcReg, immBReg)) }
              is("b100".U) { SysCall.Inline(exec.lt(rs1ValueReg, rs2ValueReg, decodePcReg, immBReg)) }
              is("b110".U) { SysCall.Inline(exec.ltu(rs1ValueReg, rs2ValueReg, decodePcReg, immBReg)) }
              is("b101".U) { SysCall.Inline(exec.ge(rs1ValueReg, rs2ValueReg, decodePcReg, immBReg)) }
              is("b111".U) { SysCall.Inline(exec.geu(rs1ValueReg, rs2ValueReg, decodePcReg, immBReg)) }
            }
          }

          is(InstFamily.JUMP) {
            when(decodeInstReg(6, 0) === "b1101111".U) {
              SysCall.Inline(exec.jal(rdReg, wbTokenReg, decodePcReg, immJReg.asSInt))
            }.otherwise {
              SysCall.Inline(exec.jalr(rdReg, wbTokenReg, decodePcReg, rs1ValueReg, immIReg))
            }
          }

          is(InstFamily.UPPER) {
            when(decodeInstReg(6, 0) === "b0110111".U) {
              SysCall.Inline(exec.writeReg(rdReg, wbTokenReg, immUReg))
            }.otherwise {
              SysCall.Inline(exec.auipc(rdReg, wbTokenReg, decodePcReg, immUReg))
            }
          }

          is(InstFamily.CSR) {
            val csrSrc = Mux(funct3Reg(2), decodeInstReg(19, 15).pad(XLEN), rs1ValueReg)
            switch(funct3Reg) {
              is("b001".U) { SysCall.Inline(exec.csrRw(rdReg, wbTokenReg, decodeInstReg(31, 20), csrSrc)) }
              is("b010".U) { SysCall.Inline(exec.csrRs(rdReg, wbTokenReg, decodeInstReg(31, 20), csrSrc)) }
              is("b011".U) { SysCall.Inline(exec.csrRc(rdReg, wbTokenReg, decodeInstReg(31, 20), csrSrc)) }
              is("b101".U) { SysCall.Inline(exec.csrRw(rdReg, wbTokenReg, decodeInstReg(31, 20), csrSrc)) }
              is("b110".U) { SysCall.Inline(exec.csrRs(rdReg, wbTokenReg, decodeInstReg(31, 20), csrSrc)) }
              is("b111".U) { SysCall.Inline(exec.csrRc(rdReg, wbTokenReg, decodeInstReg(31, 20), csrSrc)) }
            }
          }

          is(InstFamily.SYSTEM) {
            printf(p"[DECODE] system instruction inst=${Hexadecimal(decodeInstReg)}\n")
          }
        }

        when(familyReg === InstFamily.SYSTEM || familyReg === InstFamily.Unknown) {
          decodeWorker.jump(decodeWorker.stepRef("Finish"))
        }
      }

      SysCall.Inline(exec.execPath())
      decodeWorker.Step("Finish") {
        decodeCompleted := true.B
        decodeWorker.jump(decodeWorker.stepRef("Done"))
      }
      decodeWorker.Step("Done") {}
      SysCall.Return()
    }
  }

  val api: DecodeApiDecl = new DecodeApiDecl {
    def decode(pc: UInt, inst: UInt): HwInline[Unit] = HwInline.thread(s"${name}_decode") { t =>
      val stepTag = s"${name}_decode_${System.identityHashCode(new Object())}"
      val lock = SysCall.Inline(decodeSlotLock.RequestLease(0))

      t.Step(s"${stepTag}_AcquireSlot") {
        SysCall.Inline(lock.Acquire())
      }
      t.Prev.edge.add {
        decodePcReg := pc
        decodeInstReg := inst
        decodeCompleted := false.B
      }

      t.Step(s"${stepTag}_StartWorker") {
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
    override def decodeInst(pc: UInt, inst: UInt): HwInline[Unit] = decode(pc, inst)
  }

  def RequestDecodeApi(): HwInline[DecodeApiDecl] = HwInline.bindings(s"${name}_decode_api") { _ =>
    api
  }

  def decodeInst(pc: UInt, inst: UInt): HwInline[Unit] = api.decodeInst(pc, inst)

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
