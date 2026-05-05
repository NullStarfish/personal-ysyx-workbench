package ysyx

import chisel3._
import chisel3.util._

import freechips.rocketchip.amba.apb._
import org.chipsalliance.cde.config.Parameters
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.util._

import chisel3.testers._
class PS2IO extends Bundle {
  val clk = Input(Bool())
  val data = Input(Bool())
}

class PS2CtrlIO extends Bundle {
  val clock = Input(Clock())
  val reset = Input(Bool())
  val in = Flipped(new APBBundle(APBBundleParameters(addrBits = 32, dataBits = 32)))
  val ps2 = new PS2IO
}

class ps2_top_apb extends BlackBox {
  val io = IO(new PS2CtrlIO)
}

class ps2Chisel extends Module {
  val io = IO(new PS2CtrlIO)
  
  val fifo = Module(new Queue(UInt(8.W), entries = 4))
  val overflow = fifo.io.enq.valid && !fifo.io.enq.ready
  when (overflow) {
    printf("ps2 overflow!!\n")
  }
  io.in.pready := true.B
  io.in.pslverr := false.B
  io.in.prdata := 0.U  
  when (fifo.io.deq.fire) {
    io.in.prdata := Cat(0.U(24.W), fifo.io.deq.bits)
  }

  fifo.io.deq.ready := (io.in.psel && io.in.penable) && (io.in.paddr(3, 0) === 0x0.U)

  when(fifo.io.deq.ready) {
    when (fifo.io.deq.fire) {
      printf(p"ps2 enable. data: ${io.in.prdata}\n");
    }
  }

  val row_buffer = Reg(UInt(8.W))
  val cnt = RegInit(0.U(4.W))

  fifo.io.enq.bits := row_buffer
  fifo.io.enq.valid := false.B

  val clk_buf = RegNext(RegNext(io.ps2.clk))
  val clk_buf_past = RegNext(clk_buf)
  val data_buf = RegNext(RegNext(io.ps2.data))


  when (clk_buf && !clk_buf_past) {
    printf(p"ps2: $data_buf\n")
  }
  when (fifo.io.enq.valid) {
    printf(p"enq: ${fifo.io.enq.bits}\n")
  }

  when (clk_buf && !clk_buf_past) {
    when (cnt === 0.U) {
      when (data_buf === 0.U) {
        cnt := cnt + 1.U
      } .otherwise {
        //nothing，do not have valid start bit
      }
    } .elsewhen(cnt === 10.U) {
      when (data_buf === 1.U) {
        //valid stop bit: write to queue
        //reset precedure
        fifo.io.enq.valid := true.B
        cnt := 0.U
      }
    } .elsewhen(cnt === 9.U) {
      when (row_buffer.xorR ^ data_buf) {
        //parity ok
        cnt := cnt + 1.U
      } .otherwise {
        cnt := 0.U
      }
    } .otherwise {
      row_buffer := Cat(data_buf, row_buffer(7,1))
      cnt := cnt + 1.U
    }
  }


  
}

class APBKeyboard(address: Seq[AddressSet])(implicit p: Parameters) extends LazyModule {
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
    val ps2_bundle = IO(new PS2IO)

    val mps2 = Module(new ps2Chisel)
    mps2.io.clock := clock
    mps2.io.reset := reset
    mps2.io.in <> in
    ps2_bundle <> mps2.io.ps2
  }
}
