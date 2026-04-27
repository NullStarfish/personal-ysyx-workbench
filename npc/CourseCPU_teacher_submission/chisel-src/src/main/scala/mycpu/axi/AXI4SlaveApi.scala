package mycpu.axi

import HwOS.kernel._
import chisel3._

object AXI4SlaveApi {
  class AXI4SlaveWriteReq(idWidth: Int, addrWidth: Int, dataWidth: Int) extends Bundle {
    val a = new AXI4BundleA(idWidth, addrWidth)
    val w = new AXI4BundleW(dataWidth)
  }

  def axi_listen_read_addr_once(bus: AXI4Bundle): HwInline[AXI4BundleA] = HwInline.thread("axi_slave_listen_read_addr") { t =>
    val stepTag = s"axi_slave_listen_read_addr_${System.identityHashCode(new Object())}"
    val req = WireDefault(0.U.asTypeOf(new AXI4BundleA(bus.idWidth, bus.addrWidth)))

    t.Step(s"${stepTag}_WaitAddr") {
      bus.ar.ready := true.B
      t.waitCondition(bus.ar.valid)
      when(bus.ar.valid) {
        req := bus.ar.bits
      }
    }

    req
  }

  def axi_send_read_data_once(
      bus: AXI4Bundle,
      id: UInt,
      data: UInt,
      resp: UInt = AXI4Parameters.RESP_OKAY,
      last: Bool = true.B,
  ): HwInline[Unit] = HwInline.thread("axi_slave_send_read_data") { t =>
    val stepTag = s"axi_slave_send_read_data_${System.identityHashCode(new Object())}"

    t.Step(s"${stepTag}_SendData") {
      bus.r.valid := true.B
      bus.r.bits.id := id
      bus.r.bits.data := data
      bus.r.bits.resp := resp
      bus.r.bits.last := last
      t.waitCondition(bus.r.ready)
    }
  }

  def axi_listen_write_addr_once(bus: AXI4Bundle): HwInline[AXI4BundleA] = HwInline.thread("axi_slave_listen_write_addr") { t =>
    val stepTag = s"axi_slave_listen_write_addr_${System.identityHashCode(new Object())}"
    val req = WireDefault(0.U.asTypeOf(new AXI4BundleA(bus.idWidth, bus.addrWidth)))

    t.Step(s"${stepTag}_WaitAddr") {
      bus.aw.ready := true.B
      t.waitCondition(bus.aw.valid)
      when(bus.aw.valid) {
        req := bus.aw.bits
      }
    }

    req
  }

  def axi_listen_write_data_once(bus: AXI4Bundle): HwInline[AXI4BundleW] = HwInline.thread("axi_slave_listen_write_data") { t =>
    val stepTag = s"axi_slave_listen_write_data_${System.identityHashCode(new Object())}"
    val req = WireDefault(0.U.asTypeOf(new AXI4BundleW(bus.dataWidth)))

    t.Step(s"${stepTag}_WaitData") {
      bus.w.ready := true.B
      t.waitCondition(bus.w.valid)
      when(bus.w.valid) {
        req := bus.w.bits
      }
    }

    req
  }

  def axi_listen_write_once(bus: AXI4Bundle): HwInline[AXI4SlaveWriteReq] = HwInline.thread("axi_slave_listen_write") { t =>
    val stepTag = s"axi_slave_listen_write_${System.identityHashCode(new Object())}"
    val req = WireDefault(0.U.asTypeOf(new AXI4SlaveWriteReq(bus.idWidth, bus.addrWidth, bus.dataWidth)))

    t.Step(s"${stepTag}_WaitReq") {
      bus.aw.ready := true.B
      bus.w.ready := true.B
      t.waitCondition(bus.aw.valid && bus.w.valid)

      when(bus.aw.valid && bus.w.valid) {
        req.a := bus.aw.bits
        req.w := bus.w.bits
      }
    }

    req
  }

  def axi_send_write_resp_once(
      bus: AXI4Bundle,
      id: UInt,
      resp: UInt = AXI4Parameters.RESP_OKAY,
  ): HwInline[Unit] = HwInline.thread("axi_slave_send_write_resp") { t =>
    val stepTag = s"axi_slave_send_write_resp_${System.identityHashCode(new Object())}"

    t.Step(s"${stepTag}_SendResp") {
      bus.b.valid := true.B
      bus.b.bits.id := id
      bus.b.bits.resp := resp
      t.waitCondition(bus.b.ready)
    }
  }
}
