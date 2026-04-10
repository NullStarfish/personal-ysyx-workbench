package mycpu.core.components

import chisel3._

class HazardUnit extends Module {
  val io = IO(new Bundle {
    val decodeInst = Input(UInt(32.W))

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

  val decodeRs1 = io.decodeInst(19, 15)
  val decodeRs2 = io.decodeInst(24, 20)
  val decodeOpcode = io.decodeInst(6, 0)

  val usesRs1 = decodeOpcode =/= "b0110111".U && decodeOpcode =/= "b0010111".U && decodeOpcode =/= "b1101111".U
  val usesRs2 =
    decodeOpcode === "b0110011".U ||
      decodeOpcode === "b0100011".U ||
      decodeOpcode === "b1100011".U

  def hazardsWith(rd: UInt): Bool =
    (rd =/= 0.U) &&
      ((usesRs1 && (decodeRs1 === rd)) || (usesRs2 && (decodeRs2 === rd)))

  io.loadUseStall :=
    (io.idLoadValid && hazardsWith(io.idLoadRd)) ||
      (io.exLoadValid && hazardsWith(io.exLoadRd)) ||
      (io.memPendingLoad && hazardsWith(io.memPendingRd))

  io.redirectFlush := io.exFire && io.exRedirectValid
}
