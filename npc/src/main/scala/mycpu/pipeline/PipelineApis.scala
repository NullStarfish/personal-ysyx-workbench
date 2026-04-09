package mycpu.pipeline

import HwOS.kernel.function.HwInline
import chisel3._

trait FetchApiDecl {
  def writePC(nextPc: UInt): HwInline[Unit]
  def offsetPC(delta: SInt): HwInline[Unit]
  def currentPC(): HwInline[UInt]
}

trait TraceApiDecl {
  def issue(pc: UInt, inst: UInt): HwInline[Unit]
  def commit(): HwInline[Unit]
}

trait DecodeApiDecl {
  def decodeInst(pc: UInt, inst: UInt): HwInline[Unit]
}

trait RegfileApiDecl {
  def read(addr: UInt): HwInline[UInt]
  def write(addr: UInt, data: UInt): HwInline[Unit]
}

trait RegfileProbeApiDecl {
  def read(addr: UInt): HwInline[UInt]
  def readAllFlat(): HwInline[UInt]
}

trait MemoryApiDecl {
  def read_once(addr: UInt, size: UInt): HwInline[UInt]
  def write_once(addr: UInt, size: UInt, data: UInt, strb: UInt): HwInline[Unit]
}

trait WritebackApiDecl {
  def wbPath(): HwInline[Unit]
  def writeReg(rd: UInt, data: UInt): HwInline[Unit]
  def writeRegAndRedirect(rd: UInt, data: UInt, nextPc: UInt): HwInline[Unit]
  def redirect(nextPc: UInt): HwInline[Unit]
  def redirectRelative(delta: SInt): HwInline[Unit]
  def commit(): HwInline[Unit]
}

trait AluApiDecl {
  def add(lhs: UInt, rhs: UInt): HwInline[UInt]
  def sub(lhs: UInt, rhs: UInt): HwInline[UInt]
  def and(lhs: UInt, rhs: UInt): HwInline[UInt]
  def or(lhs: UInt, rhs: UInt): HwInline[UInt]
  def xor(lhs: UInt, rhs: UInt): HwInline[UInt]
  def sll(lhs: UInt, rhs: UInt): HwInline[UInt]
  def srl(lhs: UInt, rhs: UInt): HwInline[UInt]
  def sra(lhs: UInt, rhs: UInt): HwInline[UInt]
  def slt(lhs: UInt, rhs: UInt): HwInline[UInt]
  def sltu(lhs: UInt, rhs: UInt): HwInline[UInt]
  def eq(lhs: UInt, rhs: UInt): HwInline[Bool]
  def lt(lhs: UInt, rhs: UInt): HwInline[Bool]
  def ltu(lhs: UInt, rhs: UInt): HwInline[Bool]
}

trait CsrApiDecl {
  def rw(addr: UInt, src: UInt): HwInline[UInt]
  def rs(addr: UInt, src: UInt): HwInline[UInt]
  def rc(addr: UInt, src: UInt): HwInline[UInt]
}

trait CsrProbeApiDecl {
  def read(addr: UInt): HwInline[UInt]
  def mtvec(): HwInline[UInt]
  def mepc(): HwInline[UInt]
  def mstatus(): HwInline[UInt]
  def mcause(): HwInline[UInt]
}

trait LsuApiDecl {
  def loadPath(): HwInline[Unit]
  def storePath(): HwInline[Unit]
  def loadWord(rd: UInt, addr: UInt): HwInline[Unit]
  def storeWord(addr: UInt, data: UInt): HwInline[Unit]
  def loadByte(rd: UInt, addr: UInt, unsigned: Bool): HwInline[Unit]
  def loadHalf(rd: UInt, addr: UInt, unsigned: Bool): HwInline[Unit]
  def storeByte(addr: UInt, data: UInt): HwInline[Unit]
  def storeHalf(addr: UInt, data: UInt): HwInline[Unit]
}

trait ExecuteApiDecl {
  def execPath(): HwInline[Unit]
  def memPath(): HwInline[Unit]
  def add(rd: UInt, lhs: UInt, rhs: UInt): HwInline[Unit]
  def sub(rd: UInt, lhs: UInt, rhs: UInt): HwInline[Unit]
  def and(rd: UInt, lhs: UInt, rhs: UInt): HwInline[Unit]
  def or(rd: UInt, lhs: UInt, rhs: UInt): HwInline[Unit]
  def xor(rd: UInt, lhs: UInt, rhs: UInt): HwInline[Unit]
  def sll(rd: UInt, lhs: UInt, rhs: UInt): HwInline[Unit]
  def srl(rd: UInt, lhs: UInt, rhs: UInt): HwInline[Unit]
  def sra(rd: UInt, lhs: UInt, rhs: UInt): HwInline[Unit]
  def slt(rd: UInt, lhs: UInt, rhs: UInt): HwInline[Unit]
  def sltu(rd: UInt, lhs: UInt, rhs: UInt): HwInline[Unit]
  def writeReg(rd: UInt, data: UInt): HwInline[Unit]
  def redirect(nextPc: UInt): HwInline[Unit]
  def redirectRelative(delta: SInt): HwInline[Unit]
  def eq(lhs: UInt, rhs: UInt, pc: UInt, offset: UInt): HwInline[Unit]
  def ne(lhs: UInt, rhs: UInt, pc: UInt, offset: UInt): HwInline[Unit]
  def lt(lhs: UInt, rhs: UInt, pc: UInt, offset: UInt): HwInline[Unit]
  def ltu(lhs: UInt, rhs: UInt, pc: UInt, offset: UInt): HwInline[Unit]
  def ge(lhs: UInt, rhs: UInt, pc: UInt, offset: UInt): HwInline[Unit]
  def geu(lhs: UInt, rhs: UInt, pc: UInt, offset: UInt): HwInline[Unit]
  def loadWord(rd: UInt, base: UInt, offset: UInt): HwInline[Unit]
  def storeWord(base: UInt, offset: UInt, data: UInt): HwInline[Unit]
  def loadByte(rd: UInt, base: UInt, offset: UInt, unsigned: Bool): HwInline[Unit]
  def loadHalf(rd: UInt, base: UInt, offset: UInt, unsigned: Bool): HwInline[Unit]
  def storeByte(base: UInt, offset: UInt, data: UInt): HwInline[Unit]
  def storeHalf(base: UInt, offset: UInt, data: UInt): HwInline[Unit]
  def mem(isLoad: Bool, rd: UInt, base: UInt, offset: UInt, data: UInt, kind: UInt, unsigned: Bool): HwInline[Unit]
  def load(rd: UInt, base: UInt, offset: UInt, kind: UInt, unsigned: Bool): HwInline[Unit]
  def store(base: UInt, offset: UInt, data: UInt, kind: UInt): HwInline[Unit]
  def auipc(rd: UInt, pc: UInt, imm: UInt): HwInline[Unit]
  def jal(rd: UInt, pc: UInt, offset: SInt): HwInline[Unit]
  def jalr(rd: UInt, pc: UInt, base: UInt, offset: UInt): HwInline[Unit]
  def csrRw(rd: UInt, addr: UInt, src: UInt): HwInline[Unit]
  def csrRs(rd: UInt, addr: UInt, src: UInt): HwInline[Unit]
  def csrRc(rd: UInt, addr: UInt, src: UInt): HwInline[Unit]
}
