package mycpu.core.processes

import chisel3._
import chisel3.util._
import mycpu.core.os._
import mycpu.core.bundles._
import mycpu.common._

class LsuProcess extends HwProcess[ExecutePacket, MemoryPacket]("LSU") {
  override def entry(): Unit = {
    val t = createThread()
    val dmem = sys_open("DMEM")

    t.entry {
      val out = Reg(new MemoryPacket)
      
      t.Step("Memory_Access") {
        val in = sys_read()
        out.connectDebug(in)
        
        val isRead  = in.ctrl.memEn && !in.ctrl.memWen
        val isWrite = in.ctrl.memEn && in.ctrl.memWen
        
        val readData = WireDefault(0.U)
        when(isRead) {
          readData := dmem.read(in.aluResult, in.ctrl.memFunct3(1,0))
        } .elsewhen(isWrite) {
          dmem.write(in.aluResult, in.memWData, in.ctrl.memFunct3(1,0))
        }

        out.wbData := Mux(isRead, readData, in.aluResult)
        out.rdAddr := in.rdAddr
        out.regWen := in.ctrl.regWen
      }

      t.Step("Finish") { sys_write(out) }
    }
  }
}