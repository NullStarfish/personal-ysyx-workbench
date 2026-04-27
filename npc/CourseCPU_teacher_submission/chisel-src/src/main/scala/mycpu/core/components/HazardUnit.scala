package mycpu.core.components

import chisel3._

class HazardUnit extends Module {
  val io = IO(new Bundle {
    val decodeRs1Used = Input(Bool())
    val decodeRs2Used = Input(Bool())
    val decodeRs1Addr = Input(UInt(5.W))
    val decodeRs2Addr = Input(UInt(5.W))

    val idWriteValid = Input(Bool())
    val idWriteRd = Input(UInt(5.W))

    val idLoadValid = Input(Bool())
    val idLoadRd = Input(UInt(5.W))

    val exLoadValid = Input(Bool())
    val exLoadRd = Input(UInt(5.W))

    val memPendingLoad = Input(Bool())
    val memPendingRd = Input(UInt(5.W))

    val exFire = Input(Bool())
    val exRedirectValid = Input(Bool())

    val loadUseStall = Output(Bool())
    val redirectFlush = Output(Bool())
  })

  def hazardsWith(rd: UInt): Bool =
    (rd =/= 0.U) &&
      ((io.decodeRs1Used && (io.decodeRs1Addr === rd)) || (io.decodeRs2Used && (io.decodeRs2Addr === rd)))

  io.loadUseStall :=
    (io.idWriteValid && hazardsWith(io.idWriteRd)) ||
      (io.idLoadValid && hazardsWith(io.idLoadRd)) ||
      (io.exLoadValid && hazardsWith(io.exLoadRd)) ||
      (io.memPendingLoad && hazardsWith(io.memPendingRd))

  io.redirectFlush := io.exFire && io.exRedirectValid
}
