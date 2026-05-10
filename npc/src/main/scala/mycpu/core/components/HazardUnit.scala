package mycpu.core.components

import chisel3._
import mycpu.core.bundles._

class HazardUnit extends Module {
  val io = IO(new Bundle {
    val decode = Input(new HazardRsInfo)
    val idExLoad = Input(new HazardRdInfo)
    val lsuLoad = Input(new HazardRdInfo)
    val lsuToMemWbFire = Input(Bool())
    val loadUseStall = Output(Bool())
  })

  private def hazardsWith(rd: UInt): Bool =
    (rd =/= 0.U) &&
      ((io.decode.rs1.valid && (io.decode.rs1.bits === rd)) ||
        (io.decode.rs2.valid && (io.decode.rs2.bits === rd)))

  io.loadUseStall :=
    (io.idExLoad.valid && hazardsWith(io.idExLoad.rd)) ||
      (io.lsuLoad.valid && !io.lsuToMemWbFire && hazardsWith(io.lsuLoad.rd))
}
