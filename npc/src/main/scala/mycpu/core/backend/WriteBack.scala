package mycpu.core.backend

import chisel3._
import chisel3.util._
import mycpu.common._
import mycpu.core.bundles._

class WriteBack extends Module {
  val io = IO(new Bundle {
    val in       = Flipped(Decoupled(new MemoryPacket))
    val regWrite = new WriteBackIO() 
  })

  io.in.ready := true.B 

  io.regWrite.wen  := io.in.valid && io.in.bits.wb.regWen
  io.regWrite.addr := io.in.bits.wb.rd
  io.regWrite.data := io.in.bits.wbData
}
