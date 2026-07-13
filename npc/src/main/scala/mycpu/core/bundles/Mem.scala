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
  def inst: InstInfo
  def sys: SysInfo
  def wbCtrl : WriteBackCtrl
  def wbData: WriteBackData
  def forward: ForwardSource
}

class MemoryPacket extends Bundle with withRetireTrace with MemOut {
  val inst: Bundle with InstInfo = new Bundle with InstInfo {
    val pc = XLenU
    val except = new Bundle with ExceptionInfo {
      def pc = MemoryPacket.this.inst.pc
      val no = ExceptionNumber()
      val valid = Bool()
    }
  }

  val wbCtrl = new Bundle with WriteBackCtrl {
    val rd = UInt(5.W)
    val wen = Bool()
  }

  val wbData = new Bundle with WriteBackData {
    val wdata = XLenU
  }

  val sys = new Bundle with SysInfo {
    val ebreak = Bool()
    val mret = Bool()
    val fencei = Bool()
    val csr = new Bundle with CsrInfo {
      val csrOp = CSROp()
      val csrAddr = UInt(12.W)
      def wdata = MemoryPacket.this.wbData.wdata
    }
  }

  val forward = new Bundle with ForwardSource {
    def valid = MemoryPacket.this.wbCtrl.wen && (MemoryPacket.this.wbCtrl.rd =/= 0.U)
    def addr = MemoryPacket.this.wbCtrl.rd
    def data = MemoryPacket.this.wbData.wdata
  }
  
}
