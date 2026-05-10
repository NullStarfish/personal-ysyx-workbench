package mycpu.core.bundles
import chisel3._
import chisel3.util._
import mycpu.common._

class RegReadMeta extends Bundle {
  val addr = UInt(5.W)
  val rdata = XLenU
}

trait DecodeOut {
  def execCtrl: ExecuteCtrl
  def execData: ExecuteData
  def memCtrl: MemCtrl
  def wbCtrl: WriteBackCtrl
  def raw:   RAWDetect
}



class DecodePacket extends Bundle with withRetireTrace with DecodeOut {
  val rs1 = Valid(new RegReadMeta)
  val rs2 = Valid(new RegReadMeta)
  val rd = UInt(5.W)


  val raw = new Bundle with RAWDetect {
    def rd = DecodePacket.this.rd
    object rs1 extends ValidUIntView {
      def valid = DecodePacket.this.rs1.valid
      def bits = DecodePacket.this.rs1.bits.addr
    }
    object rs2 extends ValidUIntView {
      def valid = DecodePacket.this.rs2.valid
      def bits = DecodePacket.this.rs2.bits.addr
    }
  }  

  val execCtrl = new Bundle with ExecuteCtrl {
    val aluOp = ALUOp()
    val aluSrcA = ALUSrcA()
    val aluSrcB = ALUSrcB()
    val wbSel = WBSel()
    val branchType = BranchType()
    val isJump = Bool()
    val isJalr = Bool()
    val sys   = new SysBundle
  }

  val execData = new Bundle with ExecuteData {
    val pc = UInt(32.W)
    val imm = UInt(32.W)
    def rs1 = DecodePacket.this.rs1.bits.rdata
    def rs2 = DecodePacket.this.rs2.bits.rdata
  }


  val memCtrl = new Bundle with MemCtrl {
    val en = Bool()
    val write = Bool()
    val unsigned = Bool()
    val subop = SizeSubop()
  }

  val wbCtrl = new Bundle with WriteBackCtrl {
    val wen = Bool()
    def rd = DecodePacket.this.rd
  }

}
