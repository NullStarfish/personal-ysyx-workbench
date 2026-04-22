package mycpu.core.backend

import chisel3._
import chisel3.util._
import mycpu.common._
import mycpu.core.bundles._

class ExecuteOperandSelect(enableTraceFields: Boolean = ENABLE_TRACE_FIELDS) extends Module {
  val io = IO(new Bundle {
    val in = Flipped(Decoupled(new DecodePacket(enableTraceFields)))
    val out = Decoupled(new DecodePacket(enableTraceFields))
    val exForward = Input(Valid(new ExecutePacket(enableTraceFields)))
    val memForward = Input(Valid(new MemoryPacket(enableTraceFields)))
  })

  private def forwardHit[T <: Bundle with ForwardSourceView](src: ValidIO[T], regAddr: UInt): Bool = {
    src.valid && src.bits.valid && regAddr =/= 0.U && src.bits.addr === regAddr
  }

  private def resolveRegValue(regAddr: UInt, regValue: UInt): UInt = {
    Mux(
      forwardHit(io.exForward, regAddr),
      io.exForward.bits.data,
      Mux(
        forwardHit(io.memForward, regAddr),
        io.memForward.bits.data,
        regValue,
      ),
    )
  }

  val decoded = io.in.bits
  val forwardedRs1 = resolveRegValue(decoded.bypass.rs1Addr, decoded.data.rs1)
  val forwardedRs2 = resolveRegValue(decoded.bypass.rs2Addr, decoded.data.rs2)

  io.out.bits := decoded
  io.out.bits.data.rs1 := forwardedRs1
  io.out.bits.data.rs2 := forwardedRs2

  io.out.valid := io.in.valid
  io.in.ready := io.out.ready
}
