package mycpu.pipeline

import HwOS.kernel._
import chisel3._
import mycpu.common._

final class ExecuteProcess(
    lsuRef: ApiRef[LsuApiDecl],
    writebackRef: ApiRef[WritebackApiDecl],
    localName: String = "Execute",
)(implicit kernel: Kernel)
    extends HwProcess(localName) {

  private val alu = spawn(new AluProcess("Alu"))
  private val csr = spawn(new CsrProcess("Csr"))

  val api: ExecuteApiDecl = new ExecuteApiDecl {
    private def aluApi = alu.api
    private def csrApi = csr.api
    private def lsuApi = lsuRef.get
    private def wbApi = writebackRef.get

    private def writeComputedReg(opName: String, rd: UInt, result: UInt): Unit = {
      printf(p"[EXEC] ${opName} lhs-result write rd=${Decimal(rd)} data=${Hexadecimal(result)}\n")
      SysCall.Inline(wbApi.writeReg(rd, result))
    }

    def add(rd: UInt, lhs: UInt, rhs: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_add") { _ =>
      val result = SysCall.Inline(aluApi.add(lhs, rhs))
      writeComputedReg("add", rd, result)
    }

    def sub(rd: UInt, lhs: UInt, rhs: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_sub") { _ =>
      val result = SysCall.Inline(aluApi.sub(lhs, rhs))
      writeComputedReg("sub", rd, result)
    }

    def and(rd: UInt, lhs: UInt, rhs: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_and") { _ =>
      val result = SysCall.Inline(aluApi.and(lhs, rhs))
      writeComputedReg("and", rd, result)
    }

    def or(rd: UInt, lhs: UInt, rhs: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_or") { _ =>
      val result = SysCall.Inline(aluApi.or(lhs, rhs))
      writeComputedReg("or", rd, result)
    }

    def xor(rd: UInt, lhs: UInt, rhs: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_xor") { _ =>
      val result = SysCall.Inline(aluApi.xor(lhs, rhs))
      writeComputedReg("xor", rd, result)
    }

    def sll(rd: UInt, lhs: UInt, rhs: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_sll") { _ =>
      val result = SysCall.Inline(aluApi.sll(lhs, rhs))
      writeComputedReg("sll", rd, result)
    }

    def srl(rd: UInt, lhs: UInt, rhs: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_srl") { _ =>
      val result = SysCall.Inline(aluApi.srl(lhs, rhs))
      writeComputedReg("srl", rd, result)
    }

    def sra(rd: UInt, lhs: UInt, rhs: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_sra") { _ =>
      val result = SysCall.Inline(aluApi.sra(lhs, rhs))
      writeComputedReg("sra", rd, result)
    }

    def slt(rd: UInt, lhs: UInt, rhs: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_slt") { _ =>
      val result = SysCall.Inline(aluApi.slt(lhs, rhs))
      writeComputedReg("slt", rd, result)
    }

    def sltu(rd: UInt, lhs: UInt, rhs: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_sltu") { _ =>
      val result = SysCall.Inline(aluApi.sltu(lhs, rhs))
      writeComputedReg("sltu", rd, result)
    }

    def writeReg(rd: UInt, data: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_write_reg") { _ =>
      printf(p"[EXEC] writeReg rd=${Decimal(rd)} data=${Hexadecimal(data)}\n")
      SysCall.Inline(wbApi.writeReg(rd, data))
    }

    def redirect(nextPc: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_redirect") { _ =>
      printf(p"[EXEC] redirect nextPc=${Hexadecimal(nextPc)}\n")
      SysCall.Inline(wbApi.redirect(nextPc))
    }

    def redirectRelative(delta: SInt): HwInline[Unit] = HwInline.atomic(s"${name}_redirect_relative") { _ =>
      printf(p"[EXEC] redirectRelative delta=${Hexadecimal(delta.asUInt)}\n")
      SysCall.Inline(wbApi.redirectRelative(delta))
    }

    def eq(lhs: UInt, rhs: UInt, target: SInt): HwInline[Unit] = HwInline.atomic(s"${name}_eq") { _ =>
      val result = SysCall.Inline(aluApi.eq(lhs, rhs))
      printf(p"[EXEC] eq lhs=${Hexadecimal(lhs)} rhs=${Hexadecimal(rhs)} result=${result}\n")
      when(result) {
        SysCall.Inline(wbApi.redirectRelative(target))
      }
    }

    def ne(lhs: UInt, rhs: UInt, target: SInt): HwInline[Unit] = HwInline.atomic(s"${name}_ne") { _ =>
      val result = SysCall.Inline(aluApi.ne(lhs, rhs))
      printf(p"[EXEC] ne lhs=${Hexadecimal(lhs)} rhs=${Hexadecimal(rhs)} result=${result}\n")
      when(result) {
        SysCall.Inline(wbApi.redirectRelative(target))
      }
    }

    def lt(lhs: UInt, rhs: UInt, target: SInt): HwInline[Unit] = HwInline.atomic(s"${name}_lt") { _ =>
      val result = SysCall.Inline(aluApi.lt(lhs, rhs))
      printf(p"[EXEC] lt lhs=${Hexadecimal(lhs)} rhs=${Hexadecimal(rhs)} result=${result}\n")
      when(result) {
        SysCall.Inline(wbApi.redirectRelative(target))
      }
    }

    def ltu(lhs: UInt, rhs: UInt, target: SInt): HwInline[Unit] = HwInline.atomic(s"${name}_ltu") { _ =>
      val result = SysCall.Inline(aluApi.ltu(lhs, rhs))
      printf(p"[EXEC] ltu lhs=${Hexadecimal(lhs)} rhs=${Hexadecimal(rhs)} result=${result}\n")
      when(result) {
        SysCall.Inline(wbApi.redirectRelative(target))
      }
    }

    def loadWord(rd: UInt, base: UInt, offset: UInt): HwInline[Unit] = HwInline.thread(s"${name}_load_word") { t =>
      val stepTag = s"${name}_load_word_${System.identityHashCode(new Object())}"
      val addr = SysCall.Inline(aluApi.add(base, offset))
      printf(p"[EXEC] loadWord base=${Hexadecimal(base)} offset=${Hexadecimal(offset)} addr=${Hexadecimal(addr)} rd=${Decimal(rd)}\n")
      SysCall.Call(lsuApi.loadWord(rd, addr), s"${stepTag}_Done")
      t.Step(s"${stepTag}_Done") {}
      SysCall.Return()
    }

    def storeWord(base: UInt, offset: UInt, data: UInt): HwInline[Unit] = HwInline.thread(s"${name}_store_word") { t =>
      val stepTag = s"${name}_store_word_${System.identityHashCode(new Object())}"
      val addr = SysCall.Inline(aluApi.add(base, offset))
      printf(p"[EXEC] storeWord base=${Hexadecimal(base)} offset=${Hexadecimal(offset)} addr=${Hexadecimal(addr)} data=${Hexadecimal(data)}\n")
      SysCall.Call(lsuApi.storeWord(addr, data), s"${stepTag}_Done")
      t.Step(s"${stepTag}_Done") {}
      SysCall.Return()
    }

    def loadByte(rd: UInt, base: UInt, offset: UInt, unsigned: Bool): HwInline[Unit] = HwInline.thread(s"${name}_load_byte") { t =>
      val stepTag = s"${name}_load_byte_${System.identityHashCode(new Object())}"
      val addr = SysCall.Inline(aluApi.add(base, offset))
      printf(p"[EXEC] loadByte base=${Hexadecimal(base)} offset=${Hexadecimal(offset)} addr=${Hexadecimal(addr)} unsigned=${unsigned} rd=${Decimal(rd)}\n")
      SysCall.Call(lsuApi.loadByte(rd, addr, unsigned), s"${stepTag}_Done")
      t.Step(s"${stepTag}_Done") {}
      SysCall.Return()
    }

    def loadHalf(rd: UInt, base: UInt, offset: UInt, unsigned: Bool): HwInline[Unit] = HwInline.thread(s"${name}_load_half") { t =>
      val stepTag = s"${name}_load_half_${System.identityHashCode(new Object())}"
      val addr = SysCall.Inline(aluApi.add(base, offset))
      printf(p"[EXEC] loadHalf base=${Hexadecimal(base)} offset=${Hexadecimal(offset)} addr=${Hexadecimal(addr)} unsigned=${unsigned} rd=${Decimal(rd)}\n")
      SysCall.Call(lsuApi.loadHalf(rd, addr, unsigned), s"${stepTag}_Done")
      t.Step(s"${stepTag}_Done") {}
      SysCall.Return()
    }

    def storeByte(base: UInt, offset: UInt, data: UInt): HwInline[Unit] = HwInline.thread(s"${name}_store_byte") { t =>
      val stepTag = s"${name}_store_byte_${System.identityHashCode(new Object())}"
      val addr = SysCall.Inline(aluApi.add(base, offset))
      printf(p"[EXEC] storeByte base=${Hexadecimal(base)} offset=${Hexadecimal(offset)} addr=${Hexadecimal(addr)} data=${Hexadecimal(data)}\n")
      SysCall.Call(lsuApi.storeByte(addr, data), s"${stepTag}_Done")
      t.Step(s"${stepTag}_Done") {}
      SysCall.Return()
    }

    def storeHalf(base: UInt, offset: UInt, data: UInt): HwInline[Unit] = HwInline.thread(s"${name}_store_half") { t =>
      val stepTag = s"${name}_store_half_${System.identityHashCode(new Object())}"
      val addr = SysCall.Inline(aluApi.add(base, offset))
      printf(p"[EXEC] storeHalf base=${Hexadecimal(base)} offset=${Hexadecimal(offset)} addr=${Hexadecimal(addr)} data=${Hexadecimal(data)}\n")
      SysCall.Call(lsuApi.storeHalf(addr, data), s"${stepTag}_Done")
      t.Step(s"${stepTag}_Done") {}
      SysCall.Return()
    }

    def csrRw(rd: UInt, addr: UInt, src: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_csr_rw") { _ =>
      val oldValue = SysCall.Inline(csrApi.rw(addr, src))
      printf(p"[EXEC] csrRw rd=${Decimal(rd)} addr=${Hexadecimal(addr)} src=${Hexadecimal(src)} old=${Hexadecimal(oldValue)}\n")
      SysCall.Inline(wbApi.writeReg(rd, oldValue))
    }

    def csrRs(rd: UInt, addr: UInt, src: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_csr_rs") { _ =>
      val oldValue = SysCall.Inline(csrApi.rs(addr, src))
      printf(p"[EXEC] csrRs rd=${Decimal(rd)} addr=${Hexadecimal(addr)} src=${Hexadecimal(src)} old=${Hexadecimal(oldValue)}\n")
      SysCall.Inline(wbApi.writeReg(rd, oldValue))
    }

    def csrRc(rd: UInt, addr: UInt, src: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_csr_rc") { _ =>
      val oldValue = SysCall.Inline(csrApi.rc(addr, src))
      printf(p"[EXEC] csrRc rd=${Decimal(rd)} addr=${Hexadecimal(addr)} src=${Hexadecimal(src)} old=${Hexadecimal(oldValue)}\n")
      SysCall.Inline(wbApi.writeReg(rd, oldValue))
    }
  }

  def RequestExecuteApi(): HwInline[ExecuteApiDecl] = HwInline.bindings(s"${name}_execute_api") { _ =>
    api
  }

  override def entry(): Unit = {}
}
