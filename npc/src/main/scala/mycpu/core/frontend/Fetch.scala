package mycpu.core.frontend

import chisel3._
import chisel3.util._
import mycpu.common._
import mycpu.core.bundles._
import mycpu.utils._

class Fetch extends Module {
  val io = IO(new Bundle {
    val axi = new AXI4LiteBundle(XLEN, XLEN)
    val out = Decoupled(new FetchPacket)
    val next_pc = Input(UInt(XLEN.W))
    val pc_update_en = Input(Bool())
  })

  val readBridge = Module(new AXI4ReadBridge(XLEN, XLEN))
  io.axi.aw.valid := false.B
  io.axi.aw.bits := DontCare
  io.axi.w.valid := false.B
  io.axi.w.bits := DontCare
  io.axi.b.ready := false.B

  io.axi.ar <> readBridge.io.axi.ar
  io.axi.r <> readBridge.io.axi.r

  val pcReg = RegInit(START_ADDR.U(XLEN.W))
  val epochReg = RegInit(false.B)
  val reqPendingReg = RegInit(false.B)
  val reqPcReg = Reg(UInt(XLEN.W))
  val reqEpochReg = Reg(Bool())

  val outValidReg = RegInit(false.B)
  val outBitsReg = Reg(new FetchPacket)

  val reqBits = Wire(new AXI4BundleA(AXI_ID_WIDTH, XLEN))
  reqBits.id := 0.U
  reqBits.addr := pcReg
  reqBits.len := 0.U
  reqBits.size := "b010".U
  reqBits.burst := AXI4Parameters.BURST_FIXED
  reqBits.lock := false.B
  reqBits.cache := 0.U
  reqBits.prot := 0.U
  reqBits.qos := 0.U

  readBridge.io.rReq.valid := !reqPendingReg && !outValidReg && !io.pc_update_en
  readBridge.io.rReq.bits := reqBits
  readBridge.io.rStream.ready := true.B

  io.out.valid := outValidReg
  io.out.bits := outBitsReg

  when(io.out.fire) {
    outValidReg := false.B
  }

  when(io.pc_update_en) {
    pcReg := io.next_pc
    epochReg := ~epochReg
    outValidReg := false.B
  }

  when(readBridge.io.rReq.fire) {
    reqPendingReg := true.B
    reqPcReg := pcReg
    reqEpochReg := epochReg
    pcReg := pcReg + 4.U
  }

  when(readBridge.io.rStream.fire) {
    reqPendingReg := false.B
    when(reqEpochReg === epochReg) {
      outBitsReg.pc := reqPcReg
      outBitsReg.inst := readBridge.io.rStream.bits.data
      outBitsReg.dnpc := reqPcReg + 4.U
      outBitsReg.isException := readBridge.io.rStream.bits.resp =/= AXI4Parameters.RESP_OKAY
      outValidReg := true.B
    }
  }
}
