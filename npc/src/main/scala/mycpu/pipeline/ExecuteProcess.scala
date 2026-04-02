package mycpu.pipeline

import HwOS.kernel._
import chisel3._
import mycpu.common._

final class ExecuteProcess(
    fetchRef: ApiRef[FetchApiDecl],
    regfile: RegfileProcess,
    localName: String = "Execute",
)(implicit kernel: Kernel)
    extends HwProcess(localName) {

  val api: ExecuteApiDecl = new ExecuteApiDecl {
    private def commitReg(opName: String, rd: UInt, result: UInt): Unit = {
      printf(p"[EXEC] ${opName} write rd=${Decimal(rd)} data=${Hexadecimal(result)}\n")
    }

    private def writeComputedReg(opName: String, rd: UInt, result: UInt): Unit = {
      val regApi = SysCall.Inline(regfile.RequestRegfileApi())
      printf(p"[EXEC] ${opName} lhs-result write rd=${Decimal(rd)} data=${Hexadecimal(result)}\n")
      SysCall.Inline(regApi.write(rd, result))
    }

    def add(rd: UInt, lhs: UInt, rhs: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_add") { _ =>
      val result = lhs + rhs
      printf(p"[EXEC] add lhs=${Hexadecimal(lhs)} rhs=${Hexadecimal(rhs)} result=${Hexadecimal(result)}\n")
      writeComputedReg("add", rd, result)
    }

    def sub(rd: UInt, lhs: UInt, rhs: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_sub") { _ =>
      val result = lhs - rhs
      printf(p"[EXEC] sub lhs=${Hexadecimal(lhs)} rhs=${Hexadecimal(rhs)} result=${Hexadecimal(result)}\n")
      writeComputedReg("sub", rd, result)
    }

    def and(rd: UInt, lhs: UInt, rhs: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_and") { _ =>
      val result = lhs & rhs
      printf(p"[EXEC] and lhs=${Hexadecimal(lhs)} rhs=${Hexadecimal(rhs)} result=${Hexadecimal(result)}\n")
      writeComputedReg("and", rd, result)
    }

    def or(rd: UInt, lhs: UInt, rhs: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_or") { _ =>
      val result = lhs | rhs
      printf(p"[EXEC] or lhs=${Hexadecimal(lhs)} rhs=${Hexadecimal(rhs)} result=${Hexadecimal(result)}\n")
      writeComputedReg("or", rd, result)
    }

    def xor(rd: UInt, lhs: UInt, rhs: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_xor") { _ =>
      val result = lhs ^ rhs
      printf(p"[EXEC] xor lhs=${Hexadecimal(lhs)} rhs=${Hexadecimal(rhs)} result=${Hexadecimal(result)}\n")
      writeComputedReg("xor", rd, result)
    }

    def sll(rd: UInt, lhs: UInt, rhs: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_sll") { _ =>
      val shamt = rhs(4, 0)
      val result = lhs << shamt
      printf(p"[EXEC] sll lhs=${Hexadecimal(lhs)} shamt=${Hexadecimal(shamt)} result=${Hexadecimal(result)}\n")
      writeComputedReg("sll", rd, result)
    }

    def srl(rd: UInt, lhs: UInt, rhs: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_srl") { _ =>
      val shamt = rhs(4, 0)
      val result = lhs >> shamt
      printf(p"[EXEC] srl lhs=${Hexadecimal(lhs)} shamt=${Hexadecimal(shamt)} result=${Hexadecimal(result)}\n")
      writeComputedReg("srl", rd, result)
    }

    def sra(rd: UInt, lhs: UInt, rhs: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_sra") { _ =>
      val shamt = rhs(4, 0)
      val result = (lhs.asSInt >> shamt).asUInt
      printf(p"[EXEC] sra lhs=${Hexadecimal(lhs)} shamt=${Hexadecimal(shamt)} result=${Hexadecimal(result)}\n")
      writeComputedReg("sra", rd, result)
    }

    def slt(rd: UInt, lhs: UInt, rhs: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_slt") { _ =>
      val result = Mux(lhs.asSInt < rhs.asSInt, 1.U(XLEN.W), 0.U(XLEN.W))
      printf(p"[EXEC] slt lhs=${Hexadecimal(lhs)} rhs=${Hexadecimal(rhs)} result=${Hexadecimal(result)}\n")
      writeComputedReg("slt", rd, result)
    }

    def sltu(rd: UInt, lhs: UInt, rhs: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_sltu") { _ =>
      val result = Mux(lhs < rhs, 1.U(XLEN.W), 0.U(XLEN.W))
      printf(p"[EXEC] sltu lhs=${Hexadecimal(lhs)} rhs=${Hexadecimal(rhs)} result=${Hexadecimal(result)}\n")
      writeComputedReg("sltu", rd, result)
    }

    def writeReg(rd: UInt, data: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_write_reg") { _ =>
      val regApi = SysCall.Inline(regfile.RequestRegfileApi())
      printf(p"[EXEC] writeReg rd=${Decimal(rd)} data=${Hexadecimal(data)}\n")
      SysCall.Inline(regApi.write(rd, data))
    }

    def redirect(nextPc: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_redirect") { _ =>
      val fetchApi = SysCall.Inline(HwInline.bindings(s"${name}_fetch_link_abs") { _ => fetchRef.get })
      printf(p"[EXEC] redirect nextPc=${Hexadecimal(nextPc)}\n")
      SysCall.Inline(fetchApi.writePC(nextPc))
    }

    def redirectRelative(delta: SInt): HwInline[Unit] = HwInline.atomic(s"${name}_redirect_relative") { _ =>
      val fetchApi = SysCall.Inline(HwInline.bindings(s"${name}_fetch_link_rel") { _ => fetchRef.get })
      printf(p"[EXEC] redirectRelative delta=${Hexadecimal(delta.asUInt)}\n")
      SysCall.Inline(fetchApi.offsetPC(delta))
    }

    def eq(lhs: UInt, rhs: UInt, target: SInt): HwInline[Unit] = HwInline.atomic(s"${name}_eq") { _ =>
      val fetchApi = SysCall.Inline(HwInline.bindings(s"${name}_fetch_link_eq") { _ => fetchRef.get })
      val result = lhs === rhs
      printf(p"[EXEC] eq lhs=${Hexadecimal(lhs)} rhs=${Hexadecimal(rhs)} result=${result}\n")
      when(result) {
        SysCall.Inline(fetchApi.offsetPC(target))
      }
    }

    def ne(lhs: UInt, rhs: UInt, target: SInt): HwInline[Unit] = HwInline.atomic(s"${name}_ne") { _ =>
      val fetchApi = SysCall.Inline(HwInline.bindings(s"${name}_fetch_link_ne") { _ => fetchRef.get })
      val result = lhs =/= rhs
      printf(p"[EXEC] ne lhs=${Hexadecimal(lhs)} rhs=${Hexadecimal(rhs)} result=${result}\n")
      when(result) {
        SysCall.Inline(fetchApi.offsetPC(target))
      }
    }

    def lt(lhs: UInt, rhs: UInt, target: SInt): HwInline[Unit] = HwInline.atomic(s"${name}_lt") { _ =>
      val fetchApi = SysCall.Inline(HwInline.bindings(s"${name}_fetch_link_lt") { _ => fetchRef.get })
      val result = lhs.asSInt < rhs.asSInt
      printf(p"[EXEC] lt lhs=${Hexadecimal(lhs)} rhs=${Hexadecimal(rhs)} result=${result}\n")
      when(result) {
        SysCall.Inline(fetchApi.offsetPC(target))
      }
    }

    def ltu(lhs: UInt, rhs: UInt, target: SInt): HwInline[Unit] = HwInline.atomic(s"${name}_ltu") { _ =>
      val fetchApi = SysCall.Inline(HwInline.bindings(s"${name}_fetch_link_ltu") { _ => fetchRef.get })
      val result = lhs < rhs
      printf(p"[EXEC] ltu lhs=${Hexadecimal(lhs)} rhs=${Hexadecimal(rhs)} result=${result}\n")
      when(result) {
        SysCall.Inline(fetchApi.offsetPC(target))
      }
    }

    def loadWord(rd: UInt, addr: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_load_word") { _ =>
      val result = 0.U(XLEN.W)
      printf(p"[EXEC] loadWord addr=${Hexadecimal(addr)} result=${Hexadecimal(result)}\n")
      writeComputedReg("loadWord", rd, result)
    }

    def storeWord(addr: UInt, data: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_store_word") { _ =>
      printf(p"[EXEC] storeWord addr=${Hexadecimal(addr)} data=${Hexadecimal(data)}\n")
    }

    def loadByte(rd: UInt, addr: UInt, unsigned: Bool): HwInline[Unit] = HwInline.atomic(s"${name}_load_byte") { _ =>
      val result = 0.U(XLEN.W)
      printf(p"[EXEC] loadByte addr=${Hexadecimal(addr)} unsigned=${unsigned} result=${Hexadecimal(result)}\n")
      writeComputedReg("loadByte", rd, result)
    }

    def loadHalf(rd: UInt, addr: UInt, unsigned: Bool): HwInline[Unit] = HwInline.atomic(s"${name}_load_half") { _ =>
      val result = 0.U(XLEN.W)
      printf(p"[EXEC] loadHalf addr=${Hexadecimal(addr)} unsigned=${unsigned} result=${Hexadecimal(result)}\n")
      writeComputedReg("loadHalf", rd, result)
    }

    def storeByte(addr: UInt, data: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_store_byte") { _ =>
      printf(p"[EXEC] storeByte addr=${Hexadecimal(addr)} data=${Hexadecimal(data)}\n")
    }

    def storeHalf(addr: UInt, data: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_store_half") { _ =>
      printf(p"[EXEC] storeHalf addr=${Hexadecimal(addr)} data=${Hexadecimal(data)}\n")
    }

    def readCSR(addr: UInt): HwInline[UInt] = HwInline.atomic(s"${name}_read_csr") { _ =>
      val result = 0.U(XLEN.W)
      printf(p"[EXEC] readCSR addr=${Hexadecimal(addr)} result=${Hexadecimal(result)}\n")
      result
    }

    def writeCSR(addr: UInt, data: UInt): HwInline[Unit] = HwInline.atomic(s"${name}_write_csr") { _ =>
      printf(p"[EXEC] writeCSR addr=${Hexadecimal(addr)} data=${Hexadecimal(data)}\n")
    }
  }

  def RequestExecuteApi(): HwInline[ExecuteApiDecl] = HwInline.bindings(s"${name}_execute_api") { _ =>
    api
  }

  override def entry(): Unit = {}
}
