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
  def loadWord(rd: UInt, addr: UInt): HwInline[Unit]
  def storeWord(addr: UInt, data: UInt): HwInline[Unit]
  def loadByte(rd: UInt, addr: UInt, unsigned: Bool): HwInline[Unit]
  def loadHalf(rd: UInt, addr: UInt, unsigned: Bool): HwInline[Unit]
  def storeByte(addr: UInt, data: UInt): HwInline[Unit]
  def storeHalf(addr: UInt, data: UInt): HwInline[Unit]
  def readCSR(addr: UInt): HwInline[UInt]
  def writeCSR(addr: UInt, data: UInt): HwInline[Unit]
}
