package mycpu.core.backend

import chisel3._
import chisel3.util._
import mycpu.common._
import mycpu.core.bundles._

class WriteBack extends Module {
  val io = IO(new Bundle {
    val in       = Flipped(Decoupled(new MemoryPacket))
    val regWrite = new WriteBackIO() 
    
    // Debug 接口
    val debug_out   = Output(new Bundle with HasDebugInfo)
    val debug_valid = Output(Bool())
  })

  io.in.ready := true.B 

  io.regWrite.wen  := io.in.valid && io.in.bits.regWen
  io.regWrite.addr := io.in.bits.rdAddr
  io.regWrite.data := io.in.bits.wbData
  
  // 连接透传的调试信息
  io.debug_out.connectDebug(io.in.bits)

  // [关键修复] 增加 pc =/= 0.U 的条件
  // 只有当 valid 有效，且不是复位期间，且 PC 不为 0 时，才认为是有效提交
  io.debug_valid := io.in.valid && !reset.asBool && (io.in.bits.pc =/= 0.U)
}