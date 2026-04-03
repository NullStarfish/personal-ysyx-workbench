package mycpu.pipeline

import HwOS.kernel._
import chisel3._
import mycpu.common._

final class AluProcess(
    localName: String = "Alu",
)(implicit kernel: Kernel)
    extends HwProcess(localName) {

  val api: AluApiDecl = new AluApiDecl {
    override def add(lhs: UInt, rhs: UInt): HwInline[UInt] = HwInline.atomic(s"${name}_add") { _ =>
      val result = lhs + rhs
      printf(p"[ALU] add lhs=${Hexadecimal(lhs)} rhs=${Hexadecimal(rhs)} result=${Hexadecimal(result)}\n")
      result
    }
    override def sub(lhs: UInt, rhs: UInt): HwInline[UInt] = HwInline.atomic(s"${name}_sub") { _ =>
      val result = lhs - rhs
      printf(p"[ALU] sub lhs=${Hexadecimal(lhs)} rhs=${Hexadecimal(rhs)} result=${Hexadecimal(result)}\n")
      result
    }
    override def and(lhs: UInt, rhs: UInt): HwInline[UInt] = HwInline.atomic(s"${name}_and") { _ => lhs & rhs }
    override def or(lhs: UInt, rhs: UInt): HwInline[UInt] = HwInline.atomic(s"${name}_or") { _ => lhs | rhs }
    override def xor(lhs: UInt, rhs: UInt): HwInline[UInt] = HwInline.atomic(s"${name}_xor") { _ => lhs ^ rhs }
    override def sll(lhs: UInt, rhs: UInt): HwInline[UInt] = HwInline.atomic(s"${name}_sll") { _ => lhs << rhs(4, 0) }
    override def srl(lhs: UInt, rhs: UInt): HwInline[UInt] = HwInline.atomic(s"${name}_srl") { _ => lhs >> rhs(4, 0) }
    override def sra(lhs: UInt, rhs: UInt): HwInline[UInt] = HwInline.atomic(s"${name}_sra") { _ => (lhs.asSInt >> rhs(4, 0)).asUInt }
    override def slt(lhs: UInt, rhs: UInt): HwInline[UInt] = HwInline.atomic(s"${name}_slt") { _ => Mux(lhs.asSInt < rhs.asSInt, 1.U(XLEN.W), 0.U(XLEN.W)) }
    override def sltu(lhs: UInt, rhs: UInt): HwInline[UInt] = HwInline.atomic(s"${name}_sltu") { _ => Mux(lhs < rhs, 1.U(XLEN.W), 0.U(XLEN.W)) }
    override def eq(lhs: UInt, rhs: UInt): HwInline[Bool] = HwInline.atomic(s"${name}_eq") { _ => lhs === rhs }
    override def ne(lhs: UInt, rhs: UInt): HwInline[Bool] = HwInline.atomic(s"${name}_ne") { _ => lhs =/= rhs }
    override def lt(lhs: UInt, rhs: UInt): HwInline[Bool] = HwInline.atomic(s"${name}_lt") { _ => lhs.asSInt < rhs.asSInt }
    override def ltu(lhs: UInt, rhs: UInt): HwInline[Bool] = HwInline.atomic(s"${name}_ltu") { _ => lhs < rhs }
  }

  def RequestAluApi(): HwInline[AluApiDecl] = HwInline.bindings(s"${name}_alu_api") { _ =>
    api
  }

  override def entry(): Unit = {}
}
