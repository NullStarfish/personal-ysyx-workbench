package mycpu.axi
import chisel3._
import HwOS.kernel._

object AXI4Api {
  def axi_read_once(bus: AXI4Bundle, id: UInt, addr: UInt, size: UInt): HwInline[UInt] = HwInline.thread("axi_read") { t =>
    val stepTag = s"axi_read_${System.identityHashCode(new Object())}"
    val responseData = WireInit(0.U(bus.dataWidth.W))

    t.Step(s"${stepTag}_IssueAddr") {
      bus.ar.valid := true.B
      bus.ar.bits.addr := addr
      bus.ar.bits.id := id
      bus.ar.bits.len := 0.U; bus.ar.bits.size := size;
      bus.ar.bits.burst := 0.U; bus.ar.bits.lock := false.B
      bus.ar.bits.cache := 0.U; bus.ar.bits.prot := 0.U; bus.ar.bits.qos := 0.U
      t.waitCondition(bus.ar.ready)
    }

    t.Step(s"${stepTag}_WaitData") {
      bus.r.ready := true.B
      t.waitCondition(bus.r.valid)
      when(bus.r.valid) {
        chisel3.assert(
          bus.r.bits.resp === 0.U || bus.r.bits.resp === 1.U,
          "axi_read only accepts AXI OKAY/EXOKAY responses in this demo",
        )
        responseData := bus.r.bits.data
        printf(p"axi read data: ${bus.r.bits.data}\n")
      }
    }

    responseData
  }

  def axi_write_once(
      bus: AXI4Bundle,
      id: UInt,
      addr: UInt,
      size: UInt,
      data: UInt,
      strb: UInt,
  ): HwInline[Unit] = HwInline.thread("axi_write") { t =>
    val stepTag = s"axi_write_${System.identityHashCode(new Object())}"

    t.Step(s"${stepTag}_IssueAddrData") {
      bus.aw.valid := true.B
      bus.aw.bits.addr := addr
      bus.aw.bits.id := id
      bus.aw.bits.len := 0.U
      bus.aw.bits.size := size
      bus.aw.bits.burst := 0.U
      bus.aw.bits.lock := false.B
      bus.aw.bits.cache := 0.U
      bus.aw.bits.prot := 0.U
      bus.aw.bits.qos := 0.U

      bus.w.valid := true.B
      bus.w.bits.data := data
      bus.w.bits.strb := strb
      bus.w.bits.last := true.B

      t.waitCondition(bus.aw.ready && bus.w.ready)
    }

    t.Step(s"${stepTag}_WaitResp") {
      bus.b.ready := true.B
      t.waitCondition(bus.b.valid)
      when(bus.b.valid) {
        chisel3.assert(
          bus.b.bits.resp === 0.U || bus.b.bits.resp === 1.U,
          "axi_write only accepts AXI OKAY/EXOKAY responses in this demo",
        )
      }
    }
  }
}
