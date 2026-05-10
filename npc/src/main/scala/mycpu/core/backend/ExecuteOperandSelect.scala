package mycpu.core.backend

import chisel3._
import chisel3.util._
import mycpu.common._
import mycpu.core.bundles._

class ExecuteOperandSelect(
    enableTraceFields: Boolean = ENABLE_TRACE_FIELDS,
) extends Module {
  val io = IO(new Bundle {
    val in = Flipped(Decoupled(new DecodePacket))
    val out = Decoupled(new DecodePacket)
    val exForward = Input(Valid(new ExecutePacket(enableTraceFields)))
    val memForward = Input(Valid(new MemoryPacket))
  })

  private def forwardHit(srcValid: Bool, src: ForwardSource, regAddr: UInt): Bool = {
    srcValid && src.valid && regAddr =/= 0.U && src.addr === regAddr
  }

  private def resolveRegValue(regAddr: UInt, regValue: UInt): UInt = {
    Mux(
      forwardHit(io.exForward.valid, io.exForward.bits.forward, regAddr),
      io.exForward.bits.forward.data,
      Mux(
        forwardHit(io.memForward.valid, io.memForward.bits.forward, regAddr),
        io.memForward.bits.forward.data,
        regValue,
      ),
    )
  }

  val decoded = io.in.bits
  val forwardedRs1 = resolveRegValue(decoded.raw.rs1.bits, decoded.execData.rs1)
  val forwardedRs2 = resolveRegValue(decoded.raw.rs2.bits, decoded.execData.rs2)

  io.out.bits := decoded
  io.out.bits.rs1.bits.rdata := forwardedRs1
  io.out.bits.rs2.bits.rdata := forwardedRs2

  io.out.valid := io.in.valid
  io.in.ready := io.out.ready
}
