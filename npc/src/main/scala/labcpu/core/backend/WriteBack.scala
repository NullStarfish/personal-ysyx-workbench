package labcpu.core.backend

import chisel3._
import chisel3.util._
import mycpu.common._
import mycpu.core.bundles._

class WriteBack extends Module {
  val io = IO(new Bundle {
    val in = Input(Valid(new ExecutePacket))
    val dmemRdata = Input(UInt(XLEN.W))
    val out = Output(new MemoryPacket)
    val regWrite = Output(new WriteBackIO)
    val retire = Output(new RetireEventBundle)
  })

  val wbData = Mux(
    io.in.bits.mem.valid && !io.in.bits.mem.write,
    io.dmemRdata,
    io.in.bits.result,
  )
  // Execute has already resolved the architectural next PC. Recomputing it here
  // from redirect.valid loses correctly predicted taken branches, which retire
  // with redirect.valid = false but still have a non-sequential dnpc.
  val dnpc = io.in.bits.retire.dnpc

  io.out.wb := io.in.bits.wb
  io.out.wbData := wbData
  io.out.retire.pc := io.in.bits.retire.pc
  io.out.retire.inst := io.in.bits.retire.inst
  io.out.retire.dnpc := dnpc

  io.regWrite.wen := io.in.valid && io.in.bits.wb.regWen
  io.regWrite.addr := io.in.bits.wb.rd
  io.regWrite.data := wbData

  io.retire.valid := io.in.valid
  io.retire.pc := io.in.bits.retire.pc
  io.retire.dnpc := dnpc
  io.retire.inst := io.in.bits.retire.inst
  io.retire.regWen := io.in.bits.wb.regWen
  io.retire.rd := io.in.bits.wb.rd
  io.retire.data := wbData
}
