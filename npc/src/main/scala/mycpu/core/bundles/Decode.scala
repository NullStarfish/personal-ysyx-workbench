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
  def rawRs1:  RAWRegInfo 
  def rawRs2: RAWRegInfo
}



class DecodePacket extends Bundle with withRetireTrace with DecodeOut {
  val rs1 = Valid(new RegReadMeta)
  val rs2 = Valid(new RegReadMeta)
  val rd = UInt(5.W)


  val rawRs1 = new Bundle with RAWRegInfo {
    def valid = rs1.valid
    def addr = rs1.bits.addr
  }  
  val rawRs2 = new Bundle with RAWRegInfo {
    def valid = rs2.valid
    def addr = rs2.bits.addr
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
