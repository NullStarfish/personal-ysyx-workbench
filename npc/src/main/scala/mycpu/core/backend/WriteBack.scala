package mycpu.core.backend

import chisel3._
import chisel3.util._
import mycpu.common._
import mycpu.core.bundles._

class WriteBack extends Module {
  val io = IO(new Bundle {
    val in       = Flipped(Decoupled(new MemoryPacket))
    val regWrite = new WriteBackIO() // 去 RegFile
  })

  // 简单的透传
  io.regWrite.wen  := io.in.valid && io.in.bits.regWen
  io.regWrite.addr := io.in.bits.rdAddr
  io.regWrite.data := io.in.bits.wbData
  
  // WB 阶段总是 Ready 接收，除非你想在这里处理中断/调试
  io.in.ready := true.B 
}