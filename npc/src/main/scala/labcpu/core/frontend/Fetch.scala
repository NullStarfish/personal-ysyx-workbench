package labcpu.core.frontend

import chisel3._
import chisel3.util._
import labcpu.core.bundles._
import mycpu.common._
import mycpu.core.bundles.FetchPacket

class Fetch(startAddr: BigInt = START_ADDR) extends Module {
  val io = IO(new Bundle {
    val imem = new InstMemIO
    val out = Decoupled(new FetchPacket)
    val stall = Input(Bool())
    val redirect = Flipped(Valid(UInt(XLEN.W)))
    val debug_pc = Output(UInt(XLEN.W))
  })

  val pcReg = RegInit(startAddr.U(XLEN.W))

  io.imem.addr := pcReg
  io.out.valid := true.B
  io.out.bits.pc := pcReg
  io.out.bits.inst := io.imem.rdata
  io.out.bits.dnpc := pcReg + 4.U
  io.out.bits.isException := false.B
  io.debug_pc := pcReg

  when(io.redirect.valid) {
    pcReg := io.redirect.bits
  }.elsewhen(!io.stall) {
    pcReg := pcReg + 4.U
  }
}
