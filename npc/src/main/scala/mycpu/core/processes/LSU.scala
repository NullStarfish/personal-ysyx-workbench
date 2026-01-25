package mycpu.core.processes

import chisel3._
import chisel3.util._
import mycpu.core.os._
import mycpu.core.bundles._
import mycpu.common._

class LSUProcess(implicit val iGen: ExecutePacket, val oGen: MemoryPacket) 
  extends HwProcess[ExecutePacket, MemoryPacket]("LSU") {

  def entry(): Unit = {
    val bus = sys_open("axi_lsu")
    
    val mainThread = createThread("Main")
    implicit val ctx = ThreadCtx(mainThread)

    mainThread.Loop()
    mainThread.entry {
      // 1. 阻塞式读取 (Seq Read)
      val req = sys_read() 
      val rdata = WireDefault(0.U(32.W))

      // 2. AXI 操作 (Seq Read/Write)
      val isRead = req.ctrl.memEn && !req.ctrl.memWen
      
      mainThread.Step("AXI_Op") {
        when(isRead) {
           // 调用驱动的时序读接口
           rdata := bus.readSeq(req.aluResult)
        }
        // ... Write 逻辑 ...
      }

      // 3. 阻塞式写出
      val out = Wire(new MemoryPacket)
      out.wbData := Mux(req.ctrl.memEn, rdata, req.aluResult)
      out.connectDebug(req)
      
      sys_write(out)
    }
  }
}