package mycpu.core.components

import chisel3._
import mycpu.core.bundles._

class HazardUnit extends Module {
  val io = IO(new Bundle with HazardResolv {

    val raw = new Bundle {
      val decode = Input(new RAWRsPacket)
      val idExLoad = Input(new RAWRdPacket)
      val exMemLoad = Input(new RAWRdPacket)
      val lsuLoad = Input(new RAWRdPacket)
      val lsuToMemWbFire = Input(Bool())
      val loadUseStall = Output(Bool())
    }
    val ctrl = new Bundle {
      val redirect = Input(Bool())
      val flush = Output(Bool())
    }

    def stall: Bool = raw.loadUseStall
    def flush: Bool = ctrl.flush
 })

  private def hazardsWith(rd: UInt): Bool =
    (rd =/= 0.U) &&
      ((io.raw.decode.rs1.valid && (io.raw.decode.rs1.addr === rd)) ||
        (io.raw.decode.rs2.valid && (io.raw.decode.rs2.addr === rd)))

  io.raw.loadUseStall :=
    (io.raw.idExLoad.valid && hazardsWith(io.raw.idExLoad.addr)) ||
      (io.raw.exMemLoad.valid && hazardsWith(io.raw.exMemLoad.addr)) ||
      (io.raw.lsuLoad.valid && !io.raw.lsuToMemWbFire && hazardsWith(io.raw.lsuLoad.addr))

  io.ctrl.flush := io.ctrl.redirect
}
