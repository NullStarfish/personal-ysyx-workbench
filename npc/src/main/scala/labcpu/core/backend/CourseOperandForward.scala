package labcpu.core.backend

import chisel3._
import chisel3.util._
import mycpu.common._
import mycpu.core.bundles._

class CourseOperandForward(enableTraceFields: Boolean = ENABLE_TRACE_FIELDS) extends Module {
  val io = IO(new Bundle {
    val in = Flipped(Decoupled(new DecodePacket(enableTraceFields)))
    val out = Decoupled(new DecodePacket(enableTraceFields))
    val forward = Input(Valid(new MemoryPacket(enableTraceFields)))
  })

  private def forwardHit(regAddr: UInt): Bool = {
    io.forward.valid && io.forward.bits.valid && regAddr =/= 0.U && io.forward.bits.addr === regAddr
  }

  private def resolveRegValue(regAddr: UInt, regValue: UInt): UInt = {
    Mux(forwardHit(regAddr), io.forward.bits.data, regValue)
  }

  val decoded = io.in.bits
  io.out.bits := decoded
  io.out.bits.data.rs1 := resolveRegValue(decoded.bypass.rs1Addr, decoded.data.rs1)
  io.out.bits.data.rs2 := resolveRegValue(decoded.bypass.rs2Addr, decoded.data.rs2)

  io.out.valid := io.in.valid
  io.in.ready := io.out.ready
}
