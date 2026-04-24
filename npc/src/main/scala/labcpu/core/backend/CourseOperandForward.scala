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

  val decoded = io.in.bits
  val forwardValid = io.forward.valid && io.forward.bits.valid
  val rs1ForwardHit = forwardValid && decoded.bypass.rs1Addr =/= 0.U && io.forward.bits.addr === decoded.bypass.rs1Addr
  val rs2ForwardHit = forwardValid && decoded.bypass.rs2Addr =/= 0.U && io.forward.bits.addr === decoded.bypass.rs2Addr

  io.out.bits := decoded
  io.out.bits.data.rs1 := Mux(rs1ForwardHit, io.forward.bits.data, decoded.data.rs1)
  io.out.bits.data.rs2 := Mux(rs2ForwardHit, io.forward.bits.data, decoded.data.rs2)

  io.out.valid := io.in.valid
  io.in.ready := io.out.ready
}
