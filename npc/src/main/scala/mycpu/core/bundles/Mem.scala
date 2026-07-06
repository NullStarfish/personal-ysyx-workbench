package mycpu.core.bundles
import chisel3._
import mycpu.common._


trait ReadReq {
  def addr: UInt
}
trait ReadReply {
  def data: UInt
}

trait WriteReq {
  def addr: UInt
  def strb: UInt
}



trait MemCtrl { 
  def en: Bool
  def write: Bool
  def unsigned: Bool
  def subop: SizeSubop.Type
}

trait MemData {
  def data: UInt
  def addr: UInt
}

trait MemIn {
  def ctrl: MemCtrl
  def data: MemData
}

trait MemOut {
  def wbCtrl : WriteBackCtrl
  def wbData: WriteBackData
  def forward: ForwardSource
}

class MemoryPacket extends Bundle with withRetireTrace with MemOut {
  val wbCtrl = new Bundle with WriteBackCtrl {
    val rd = UInt(5.W)
    val wen = Bool()
  }

  val wbData = new Bundle with WriteBackData {
    val wdata = XLenU
  }

  val forward = new Bundle with ForwardSource {
    def valid = MemoryPacket.this.wbCtrl.wen && (MemoryPacket.this.wbCtrl.rd =/= 0.U)
    def addr = MemoryPacket.this.wbCtrl.rd
    def data = MemoryPacket.this.wbData.wdata
  }
  
}
