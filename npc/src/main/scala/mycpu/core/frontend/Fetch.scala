package mycpu.core.frontend

import chisel3._
import chisel3.util._
import mycpu.common._
import mycpu.core.bundles._
import mycpu.utils._

class Fetch extends Module {
  val io = IO(new Bundle {
    val axi          = new AXI4Bundle(AXI_ID_WIDTH, XLEN, XLEN) // Lite 接口，ID宽固定为1
    val next_pc      = Input(UInt(XLEN.W))
    val pc_update_en = Input(Bool()) 
    val out          = Decoupled(new FetchPacket()) 
  })

  class FetchNode extends Bundle {
    val pc   = UInt(XLEN.W)
    val inst = UInt(32.W)
  }

  val fetchQueue = new HwQueue(new FetchNode, entries = 2, "Fetch_Queue")
  val pc = RegInit(START_ADDR.U(XLEN.W))
  val reqSent = RegInit(false.B)

  val AXI4Split(rBus, wBus) = io.axi
  
  // Tie-off 写通道
  wBus.aw.valid := false.B; wBus.aw.bits := DontCare
  wBus.w.valid  := false.B; wBus.w.bits  := DontCare
  wBus.b.ready  := false.B

  // ==============================================================================
  // 修复点 1: 使用 chiselTypeOf 创建完全匹配的 Wire
  // ==============================================================================
  // 这里的 fetchArPacket 会自动获得 idWidth=1，与 io.axi 保持一致
  val fetchArPacket = Wire(chiselTypeOf(rBus.ar.bits))
  
  fetchArPacket.id    := 0.U
  fetchArPacket.addr  := pc
  fetchArPacket.len   := 0.U
  fetchArPacket.size  := 2.U
  fetchArPacket.burst := AXI4Parameters.BURST_FIXED 
  fetchArPacket.lock  := false.B
  fetchArPacket.cache := 0.U
  fetchArPacket.prot  := 0.U
  fetchArPacket.qos   := 0.U

  val fetchThread = new HardwareThread("Fetch_Thread")

  // ==============================================================================
  // 修复点 2: 默认值也使用 chiselTypeOf 确保位宽匹配
  // ==============================================================================
  val arValidProxy = fetchThread.driveManaged(rBus.ar.valid, false.B)
  
  // 这里的 default 值现在也是 1-bit ID，与 rBus.ar.bits 匹配
  val arBitsProxy  = fetchThread.driveManaged(rBus.ar.bits,  0.U.asTypeOf(rBus.ar.bits))
  
  val rReadyProxy  = fetchThread.driveManaged(rBus.r.ready,  false.B)

  fetchThread.startWhen(fetchQueue.canPush && !reqSent)

  fetchThread.entry {
    val instData = Reg(UInt(32.W))
    
    fetchThread.Step("AXI_AR") {
      fetchThread.write(arValidProxy, true.B)
      fetchThread.write(arBitsProxy, fetchArPacket) // 现在位宽一致了，可以安全写入
      fetchThread.waitCondition(rBus.ar.ready)
    }

    fetchThread.Step("AXI_R") {
      fetchThread.write(rReadyProxy, true.B)
      fetchThread.waitCondition(rBus.r.valid)
      instData := rBus.r.bits.data
    }

    fetchThread.Step("Push_Queue") {
      val node = Wire(new FetchNode)
      node.pc   := pc
      node.inst := instData
      
      when (fetchQueue.tryPush(node)) {
         reqSent := true.B
      } .otherwise {
         fetchThread.waitCondition(false.B) 
      }
    }
  }

  val shootLogic = new HardwareLogic("Shoot_Logic")
  val outValid = shootLogic.driveManaged(io.out.valid, false.B)
  
  shootLogic.run {
    io.out.bits := DontCare
    when (fetchQueue.canPop) {
      shootLogic.write(outValid, true.B)
      val node = fetchQueue.peek()
      val p = Wire(new FetchPacket)
      p.pc   := node.pc
      p.inst := node.inst
      p.dnpc := node.pc + 4.U
      p.isException := false.B
      io.out.bits := p

      when (io.out.ready) {
        fetchQueue.tryPop()
      }
    }
  }

  when(io.pc_update_en) {
    pc := io.next_pc
    reqSent := false.B
  }
}