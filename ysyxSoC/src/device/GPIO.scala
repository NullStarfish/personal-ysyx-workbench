package ysyx

import chisel3._
import chisel3.util._

import freechips.rocketchip.amba.apb._
import org.chipsalliance.cde.config.Parameters
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.util._

class GPIOIO extends Bundle {
  val out = Output(UInt(16.W))
  val in = Input(UInt(16.W))
  val seg = Output(Vec(8, UInt(8.W)))
}

class GPIOCtrlIO extends Bundle {
  val clock = Input(Clock())
  val reset = Input(Reset())
  val in = Flipped(new APBBundle(APBBundleParameters(addrBits = 32, dataBits = 32)))
  val gpio = new GPIOIO
}

class gpio_top_apb extends BlackBox {
  val io = IO(new GPIOCtrlIO)
}

class gpioChisel extends Module {
  val io = IO(new GPIOCtrlIO)

  val gpioOutReg = RegInit(0.U(16.W))
  val segHexReg = RegInit(0.U(32.W))
  val enable = io.in.psel && io.in.penable
  val writeMask = Cat((0 until 4).reverse.map(i => Fill(8, io.in.pstrb(i))))

  io.gpio.out := gpioOutReg
  for (i <- 0 until 8) {
    io.gpio.seg(i) := hexToSeg(segHexReg(i * 4 + 3, i * 4))
  }

  io.in.pready := true.B
  io.in.pslverr := false.B
  io.in.prdata := addrMapper(io.in.paddr)
  io.in.pduser := DontCare

  when(enable && io.in.pwrite) {
    switch(io.in.paddr(3, 0)) {
      is(0x0.U) {
        val nextGpioOut = (gpioOutReg & ~writeMask(15, 0)) | (io.in.pwdata(15, 0) & writeMask(15, 0))
        gpioOutReg := nextGpioOut
        printf(p"[gpio] out <= 0x${Hexadecimal(nextGpioOut)} (${Binary(nextGpioOut)})\n")
      }
      is(0x8.U) {
        val nextSegHex = (segHexReg & ~writeMask) | (io.in.pwdata & writeMask)
        segHexReg := nextSegHex
        printf(p"[gpio] seg <= 0x${Hexadecimal(nextSegHex)}\n")
      }
    }
  }

  def addrMapper(addr: UInt): UInt = {
    val result = WireDefault(0.U(32.W))
    when(addr(3, 0) === 0x0.U) {
      result := gpioOutReg
    }.elsewhen(addr(3, 0) === 0x4.U) {
      result := io.gpio.in
    }.elsewhen(addr(3, 0) === 0x8.U) {
      result := segHexReg
    }
    result
  }

  def hexToSeg(hex: UInt): UInt = {
    MuxLookup(hex, "hff".U(8.W))(Seq(
      "h0".U -> "hc0".U(8.W),
      "h1".U -> "hf9".U(8.W),
      "h2".U -> "ha4".U(8.W),
      "h3".U -> "hb0".U(8.W),
      "h4".U -> "h99".U(8.W),
      "h5".U -> "h92".U(8.W),
      "h6".U -> "h82".U(8.W),
      "h7".U -> "hf8".U(8.W),
      "h8".U -> "h80".U(8.W),
      "h9".U -> "h90".U(8.W),
      "ha".U -> "h88".U(8.W),
      "hb".U -> "h83".U(8.W),
      "hc".U -> "hc6".U(8.W),
      "hd".U -> "ha1".U(8.W),
      "he".U -> "h86".U(8.W),
      "hf".U -> "h8e".U(8.W)
    ))
  }
}


class APBGPIO(address: Seq[AddressSet])(implicit p: Parameters) extends LazyModule {
  val node = APBSlaveNode(Seq(APBSlavePortParameters(
    Seq(APBSlaveParameters(
      address       = address,
      executable    = true,
      supportsRead  = true,
      supportsWrite = true)),
    beatBytes  = 4)))

  lazy val module = new Impl
  class Impl extends LazyModuleImp(this) {
    val (in, _) = node.in(0)
    val gpio_bundle = IO(new GPIOIO)

    val mgpio = Module(new gpioChisel)
    mgpio.io.clock := clock
    mgpio.io.reset := reset
    mgpio.io.in <> in
    gpio_bundle <> mgpio.io.gpio
  }
}
