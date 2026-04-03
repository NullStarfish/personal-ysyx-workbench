package mycpu.pipeline

import HwOS.kernel.function.HwInline
import chisel3._

trait FetchApiDecl {
  def writePC(nextPc: UInt): HwInline[Unit]
  def offsetPC(delta: SInt): HwInline[Unit]
  def currentPC(): HwInline[UInt]
}

trait DecodeApiDecl {
  def decodeInst(inst: UInt): HwInline[Unit]
}

trait RegfileApiDecl {
  def read(addr: UInt): HwInline[UInt]
  def write(addr: UInt, data: UInt): HwInline[Unit]
}

trait MemoryApiDecl {
  def read_once(addr: UInt, size: UInt): HwInline[UInt]
  def write_once(addr: UInt, size: UInt, data: UInt, strb: UInt): HwInline[Unit]
}

trait WritebackApiDecl {
  def writeReg(rd: UInt, data: UInt): HwInline[Unit]
  def redirect(nextPc: UInt): HwInline[Unit]
  def redirectRelative(delta: SInt): HwInline[Unit]
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
  def ne(lhs: UInt, rhs: UInt): HwInline[Bool]
  def lt(lhs: UInt, rhs: UInt): HwInline[Bool]
  def ltu(lhs: UInt, rhs: UInt): HwInline[Bool]
}

trait CsrApiDecl {
  def rw(addr: UInt, src: UInt): HwInline[UInt]
  def rs(addr: UInt, src: UInt): HwInline[UInt]
  def rc(addr: UInt, src: UInt): HwInline[UInt]
}

trait LsuApiDecl {
  def loadWord(rd: UInt, addr: UInt): HwInline[Unit]
  def storeWord(addr: UInt, data: UInt): HwInline[Unit]
  def loadByte(rd: UInt, addr: UInt, unsigned: Bool): HwInline[Unit]
  def loadHalf(rd: UInt, addr: UInt, unsigned: Bool): HwInline[Unit]
  def storeByte(addr: UInt, data: UInt): HwInline[Unit]
  def storeHalf(addr: UInt, data: UInt): HwInline[Unit]
}

trait ExecuteApiDecl {
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
  def eq(lhs: UInt, rhs: UInt, target: SInt): HwInline[Unit]
  def ne(lhs: UInt, rhs: UInt, target: SInt): HwInline[Unit]
  def lt(lhs: UInt, rhs: UInt, target: SInt): HwInline[Unit]
  def ltu(lhs: UInt, rhs: UInt, target: SInt): HwInline[Unit]
  def loadWord(rd: UInt, base: UInt, offset: UInt): HwInline[Unit]
  def storeWord(base: UInt, offset: UInt, data: UInt): HwInline[Unit]
  def loadByte(rd: UInt, base: UInt, offset: UInt, unsigned: Bool): HwInline[Unit]
  def loadHalf(rd: UInt, base: UInt, offset: UInt, unsigned: Bool): HwInline[Unit]
  def storeByte(base: UInt, offset: UInt, data: UInt): HwInline[Unit]
  def storeHalf(base: UInt, offset: UInt, data: UInt): HwInline[Unit]
  def csrRw(rd: UInt, addr: UInt, src: UInt): HwInline[Unit]
  def csrRs(rd: UInt, addr: UInt, src: UInt): HwInline[Unit]
  def csrRc(rd: UInt, addr: UInt, src: UInt): HwInline[Unit]
}
