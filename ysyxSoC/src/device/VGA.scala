package ysyx

import chisel3._
import chisel3.util._

import freechips.rocketchip.amba.apb._
import org.chipsalliance.cde.config.Parameters
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.util._

class VGAIO extends Bundle {
  val r = Output(UInt(8.W))
  val g = Output(UInt(8.W))
  val b = Output(UInt(8.W))
  val hsync = Output(Bool())
  val vsync = Output(Bool())
  val valid = Output(Bool())
}

class VGACtrlIO extends Bundle {
  val clock = Input(Clock())
  val reset = Input(Bool())
  val in = Flipped(new APBBundle(APBBundleParameters(addrBits = 32, dataBits = 32)))
  val vga = new VGAIO
}

class vga_top_apb extends BlackBox {
  val io = IO(new VGACtrlIO)
}

class vgaChisel extends Module {
  val io = IO(new VGACtrlIO)
  io.in.pslverr := false.B
  io.in.prdata := 0.U


  val frame_buf = SyncReadMem(640*480, UInt(24.W))
  io.in.pready := true.B

  val writeEn  = io.in.psel && io.in.penable && io.in.pwrite
  val writeMask = Cat((0 until 4).reverse.map(i => Fill(8, io.in.pstrb(i))))
  val waddr = io.in.paddr(20, 2)
  when (writeEn && waddr < (640 * 480).U) {
    frame_buf.write(waddr, io.in.pwdata & writeMask)
  }


  when(writeEn) {
  }


  val h_frontporch = 96;
  val h_active = 144;
  val h_backporch = 784;
  val h_total = 800;
  val v_frontporch = 2;
  val v_active = 35;
  val v_backporch = 515;
  val v_total = 525;

  val x_cnt = RegInit(1.U(10.W))
  val y_cnt = RegInit(1.U(10.W))

  when (x_cnt === h_total.U) {
    x_cnt := 1.U
    when (y_cnt === v_total.U) {
      y_cnt := 1.U
    } .otherwise {
      y_cnt := y_cnt + 1.U
    }
  } .otherwise {
    x_cnt := x_cnt + 1.U
  }

  io.vga.hsync := RegNext(x_cnt > h_frontporch.U) 
  io.vga.vsync := RegNext(y_cnt > v_frontporch.U)


  val h_valid = (x_cnt > h_active.U) & (x_cnt <= h_backporch.U)
  val v_valid = (y_cnt > v_active.U) & (y_cnt <= v_backporch.U)

  io.vga.valid := RegNext(h_valid & v_valid)

  val h_addr = Mux(h_valid, (x_cnt - 145.U), 0.U)
  val v_addr = Mux(v_valid, (y_cnt - 36.U), 0.U)
  val raddr = v_addr * 640.U + h_addr
  


  val pixel = frame_buf.read(raddr)

  io.vga.r := pixel(23, 16)
  io.vga.g := pixel(15, 8)
  io.vga.b := pixel(7, 0)

  

}

class APBVGA(address: Seq[AddressSet])(implicit p: Parameters) extends LazyModule {
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
    val vga_bundle = IO(new VGAIO)

    val mvga = Module(new vgaChisel)
    mvga.io.clock := clock
    mvga.io.reset := reset
    mvga.io.in <> in
    vga_bundle <> mvga.io.vga
  }
}
