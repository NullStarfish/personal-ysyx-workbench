package mycpu.core.processes
import chisel3._
import chisel3.util._
import mycpu.core.os._
import mycpu.common._
import mycpu.utils._

import mycpu.core.bundles._

class FetchProcess(implicit val oGen: FetchPacket) extends HwProcess[UInt, FetchPacket]("Fetch") {
  def entry(): Unit = {
    val pc  = RegInit(START_ADDR.U(XLEN.W))
    val bus = sys_open("axi_lsu") // 申请总线

    val t = creatThread("Main Thread")

    t.Loop()
    t.entry {
      val inst = bus.read(pc, 4.U, false.B)
      val pkt = Wire(new FetchPacket)
      pkt.isException := false.B
      
      sys_write()
    }


  }
}