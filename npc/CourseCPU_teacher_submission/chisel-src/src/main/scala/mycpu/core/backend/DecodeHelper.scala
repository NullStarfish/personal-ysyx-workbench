package mycpu.core.backend
import chisel3._
import chisel3.util._
object InstFormat extends ChiselEnum {
  val Unknown = Value
  val R = Value
  val I = Value
  val S = Value
  val B = Value
  val U = Value
  val J = Value
}

object DecodeHelper {
  private def decodeFormat(inst: UInt): InstFormat.Type = {
    val opcode = inst(6, 0)
    val fmt = WireDefault(InstFormat.Unknown)

    switch(opcode) {
      is("b0110011".U) { fmt := InstFormat.R } // OP
      is("b0010011".U) { fmt := InstFormat.I } // OP-IMM
      is("b0000011".U) { fmt := InstFormat.I } // LOAD
      is("b1100111".U) { fmt := InstFormat.I } // JALR
      is("b1110011".U) { fmt := InstFormat.I } // SYSTEM/CSR

      is("b0100011".U) { fmt := InstFormat.S } // STORE
      is("b1100011".U) { fmt := InstFormat.B } // BRANCH
      is("b0110111".U) { fmt := InstFormat.U } // LUI
      is("b0010111".U) { fmt := InstFormat.U } // AUIPC
      is("b1101111".U) { fmt := InstFormat.J } // JAL
    }

    fmt
  }
}