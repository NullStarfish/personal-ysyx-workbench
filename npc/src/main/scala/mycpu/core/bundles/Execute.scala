package mycpu.core.bundles
import chisel3._
import mycpu.common._
class ExecuteDataBundle extends Bundle {
  val pc = XLenU
  val rs1 = XLenU
  val rs2 = XLenU
  val imm = XLenU
}
object ALUSrcA extends ChiselEnum {
  val Rs1, Pc = Value
}


object ALUSrcB extends ChiselEnum {
  val Rs2, Imm = Value
}

object WBSel extends ChiselEnum {
  val Alu, Csr, PcPlus4 = Value
}



class SysBundle extends Bundle {
  val csrOp = CSROp()
  val csrAddr = UInt(12.W)
  val ecall = Bool()
  val ebreak = Bool()
  val mret = Bool()
}



// class ExecuteCtrlBundle extends Bundle {
//   val aluOp = ALUOp()
//   val aluSrcA = ALUSrcA()
//   val aluSrcB = ALUSrcB()
//   val wbSel = WBSel()
//   val branchType = BranchType()
//   val isJump = Bool()
//   val isJalr = Bool()
// }

trait ExecuteData { 
  def pc : UInt
  def imm: UInt
  def rs1: UInt
  def rs2: UInt  
}



trait ExecuteCtrl { 
  def aluOp : ALUOp.Type
  def aluSrcA : ALUSrcA.Type
  def aluSrcB : ALUSrcB.Type
  def wbSel : WBSel.Type
  def branchType : BranchType.Type
  def isJump : Bool
  def isJalr : Bool
  def sys : SysBundle
}



trait ExecuteOut { 
  def wbData: WriteBackData
  def memData: MemData
  def memCtrl: MemCtrl
  def wbCtrl: WriteBackCtrl 
  def ifRedct: FetchRedirect
  def forward: ForwardSource
}



class ExecutePacket(enableTraceFields: Boolean = ENABLE_TRACE_FIELDS) extends Bundle with withRetireTrace with ExecuteOut {
  val lhs = XLenU
  val rhs = XLenU

  val wbCtrl = new Bundle with WriteBackCtrl {
    val wen = Bool()
    val rd = UInt(5.W)
  }

  val memCtrl = new Bundle with MemCtrl {
    val en = Bool()
    val write = Bool()
    val unsigned = Bool()
    val subop = SizeSubop()
  }

  val wbData = new Bundle with WriteBackData {
    def wdata = ExecutePacket.this.lhs
  }

  val memData = new Bundle with MemData {
    def data = ExecutePacket.this.lhs
    def addr = ExecutePacket.this.rhs
  }

  val ifRedct = new Bundle with FetchRedirect {
    val redirect = new Bundle with ValidUIntView {
      val valid  = Bool()
      def bits = ExecutePacket.this.rhs
    }
  }

  val forward = new Bundle with ForwardSource {
    def valid = ExecutePacket.this.wbCtrl.wen && !ExecutePacket.this.memCtrl.en && (ExecutePacket.this.wbCtrl.rd =/= 0.U)
    def addr = ExecutePacket.this.wbCtrl.rd
    def data = ExecutePacket.this.wbData.wdata
  }
}
