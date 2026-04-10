package mycpu.core.frontend

import chisel3._
import chisel3.util._
import mycpu.common._
import mycpu.core.bundles._
import mycpu.utils._

class Fetch extends Module {
  class FetchMeta extends Bundle {
    val pc = UInt(XLEN.W)
    val epoch = Bool()
  }

  val io = IO(new Bundle {
    val axi = new AXI4LiteBundle(XLEN, XLEN)
    val out = Decoupled(new FetchPacket)
    val ctrl = Input(new FetchControlBundle)
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

  val outValidReg = RegInit(false.B)
  val outBitsReg = Reg(new FetchPacket)
  val metaQueue = Module(new Queue(new FetchMeta, entries = 4))

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

  val staleOutstanding = metaQueue.io.count === 1.U && metaQueue.io.deq.valid && (metaQueue.io.deq.bits.epoch =/= epochReg)
  val canIssueReq = !outValidReg && !io.ctrl.stall && !io.ctrl.redirect.valid &&
    metaQueue.io.enq.ready && (metaQueue.io.count === 0.U || staleOutstanding)
  readBridge.io.rReq.valid := canIssueReq
  readBridge.io.rReq.bits := reqBits
  metaQueue.io.enq.valid := readBridge.io.rReq.fire
  metaQueue.io.enq.bits.pc := pcReg
  metaQueue.io.enq.bits.epoch := epochReg

  metaQueue.io.deq.ready := readBridge.io.rStream.valid && !outValidReg
  readBridge.io.rStream.ready := metaQueue.io.deq.valid && !outValidReg

  io.out.valid := outValidReg
  io.out.bits := outBitsReg

  when(io.out.fire) {
    outValidReg := false.B
  }

  when(io.ctrl.redirect.valid) {
    pcReg := io.ctrl.redirect.bits
    epochReg := ~epochReg
    outValidReg := false.B
  }

  when(readBridge.io.rReq.fire) {
    pcReg := pcReg + 4.U
  }

  when(readBridge.io.rStream.fire) {
    when(metaQueue.io.deq.bits.epoch === epochReg) {
      outBitsReg.pc := metaQueue.io.deq.bits.pc
      outBitsReg.inst := readBridge.io.rStream.bits.data
      outBitsReg.dnpc := metaQueue.io.deq.bits.pc + 4.U
      outBitsReg.isException := readBridge.io.rStream.bits.resp =/= AXI4Parameters.RESP_OKAY
      outValidReg := true.B
    }
  }
}
