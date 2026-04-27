package mycpu.pipeline

import HwOS.kernel._
import chisel3._
import mycpu.common._

final class AluProcess(
    localName: String = "Alu",
)(implicit kernel: Kernel)
    extends HwProcess(localName) {

  private val addLhs = WireDefault(0.U(XLEN.W))
  private val addRhs = WireDefault(0.U(XLEN.W))
  private val addOut = addLhs + addRhs

  private val subLhs = WireDefault(0.U(XLEN.W))
  private val subRhs = WireDefault(0.U(XLEN.W))
  private val subOut = subLhs - subRhs

  private val andLhs = WireDefault(0.U(XLEN.W))
  private val andRhs = WireDefault(0.U(XLEN.W))
  private val andOut = andLhs & andRhs

  private val orLhs = WireDefault(0.U(XLEN.W))
  private val orRhs = WireDefault(0.U(XLEN.W))
  private val orOut = orLhs | orRhs

  private val xorLhs = WireDefault(0.U(XLEN.W))
  private val xorRhs = WireDefault(0.U(XLEN.W))
  private val xorOut = xorLhs ^ xorRhs

  private val sllLhs = WireDefault(0.U(XLEN.W))
  private val sllRhs = WireDefault(0.U(XLEN.W))
  private val sllOut = sllLhs << sllRhs(4, 0)

  private val srlLhs = WireDefault(0.U(XLEN.W))
  private val srlRhs = WireDefault(0.U(XLEN.W))
  private val srlOut = srlLhs >> srlRhs(4, 0)

  private val sraLhs = WireDefault(0.U(XLEN.W))
  private val sraRhs = WireDefault(0.U(XLEN.W))
  private val sraOut = (sraLhs.asSInt >> sraRhs(4, 0)).asUInt

  private val sltLhs = WireDefault(0.U(XLEN.W))
  private val sltRhs = WireDefault(0.U(XLEN.W))
  private val sltBool = sltLhs.asSInt < sltRhs.asSInt
  private val sltOut = Mux(sltBool, 1.U(XLEN.W), 0.U(XLEN.W))

  private val sltuLhs = WireDefault(0.U(XLEN.W))
  private val sltuRhs = WireDefault(0.U(XLEN.W))
  private val sltuBool = sltuLhs < sltuRhs
  private val sltuOut = Mux(sltuBool, 1.U(XLEN.W), 0.U(XLEN.W))

  private val eqLhs = WireDefault(0.U(XLEN.W))
  private val eqRhs = WireDefault(0.U(XLEN.W))
  private val eqOut = eqLhs === eqRhs

  private val ltLhs = WireDefault(0.U(XLEN.W))
  private val ltRhs = WireDefault(0.U(XLEN.W))
  private val ltOut = ltLhs.asSInt < ltRhs.asSInt

  private val ltuLhs = WireDefault(0.U(XLEN.W))
  private val ltuRhs = WireDefault(0.U(XLEN.W))
  private val ltuOut = ltuLhs < ltuRhs

  val api: AluApiDecl = new AluApiDecl {
    override def add(lhs: UInt, rhs: UInt): HwInline[UInt] = HwInline.stateless(s"${name}_add") { _ =>
      addLhs := lhs
      addRhs := rhs
      printf(p"[ALU] add lhs=${Hexadecimal(lhs)} rhs=${Hexadecimal(rhs)} result=${Hexadecimal(addOut)}\n")
      addOut
    }
    override def sub(lhs: UInt, rhs: UInt): HwInline[UInt] = HwInline.stateless(s"${name}_sub") { _ =>
      subLhs := lhs
      subRhs := rhs
      printf(p"[ALU] sub lhs=${Hexadecimal(lhs)} rhs=${Hexadecimal(rhs)} result=${Hexadecimal(subOut)}\n")
      subOut
    }
    override def and(lhs: UInt, rhs: UInt): HwInline[UInt] = HwInline.stateless(s"${name}_and") { _ =>
      andLhs := lhs
      andRhs := rhs
      andOut
    }
    override def or(lhs: UInt, rhs: UInt): HwInline[UInt] = HwInline.stateless(s"${name}_or") { _ =>
      orLhs := lhs
      orRhs := rhs
      orOut
    }
    override def xor(lhs: UInt, rhs: UInt): HwInline[UInt] = HwInline.stateless(s"${name}_xor") { _ =>
      xorLhs := lhs
      xorRhs := rhs
      xorOut
    }
    override def sll(lhs: UInt, rhs: UInt): HwInline[UInt] = HwInline.stateless(s"${name}_sll") { _ =>
      sllLhs := lhs
      sllRhs := rhs
      sllOut
    }
    override def srl(lhs: UInt, rhs: UInt): HwInline[UInt] = HwInline.stateless(s"${name}_srl") { _ =>
      srlLhs := lhs
      srlRhs := rhs
      srlOut
    }
    override def sra(lhs: UInt, rhs: UInt): HwInline[UInt] = HwInline.stateless(s"${name}_sra") { _ =>
      sraLhs := lhs
      sraRhs := rhs
      sraOut
    }
    override def slt(lhs: UInt, rhs: UInt): HwInline[UInt] = HwInline.stateless(s"${name}_slt") { _ =>
      sltLhs := lhs
      sltRhs := rhs
      sltOut
    }
    override def sltu(lhs: UInt, rhs: UInt): HwInline[UInt] = HwInline.stateless(s"${name}_sltu") { _ =>
      sltuLhs := lhs
      sltuRhs := rhs
      sltuOut
    }
    override def eq(lhs: UInt, rhs: UInt): HwInline[Bool] = HwInline.stateless(s"${name}_eq") { _ =>
      eqLhs := lhs
      eqRhs := rhs
      eqOut
    }
    override def lt(lhs: UInt, rhs: UInt): HwInline[Bool] = HwInline.stateless(s"${name}_lt") { _ =>
      ltLhs := lhs
      ltRhs := rhs
      ltOut
    }
    override def ltu(lhs: UInt, rhs: UInt): HwInline[Bool] = HwInline.stateless(s"${name}_ltu") { _ =>
      ltuLhs := lhs
      ltuRhs := rhs
      ltuOut
    }
  }

  def RequestAluApi(): HwInline[AluApiDecl] = HwInline.bindings(s"${name}_alu_api") { _ =>
    api
  }

  override def entry(): Unit = {}
}
