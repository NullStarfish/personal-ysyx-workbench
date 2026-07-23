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

/** 仿真专用的 PS/2 原子 ILA 采样器；仅在 NPC_ENABLE_ILA=1 时实例化。 */
class PS2IlaProbeDPI extends BlackBox with HasBlackBoxInline {
  val io = IO(new Bundle {
    val clock = Input(Clock())
    val reset = Input(Bool())
    val sampleData = Input(UInt(128.W))
  })

  setInline(
    "PS2IlaProbeDPI.sv",
    """module PS2IlaProbeDPI (
      |  input logic clock,
      |  input logic reset,
      |  input logic [127:0] sampleData
      |);
      |  bit [31:0] sampleWords [0:3];
      |
      |  import "DPI-C" function int ila_source_allocate(input string source_name, input string schema, input int packed_width);
      |  import "DPI-C" function void ila_sample(input int id, input bit [31:0] sample_words[]);
      |
      |  genvar wordIndex;
      |  generate
      |    for (wordIndex = 0; wordIndex < 4; wordIndex = wordIndex + 1) begin : gen_sample_words
      |      assign sampleWords[wordIndex] = sampleData[wordIndex * 32 +: 32];
      |    end
      |  endgenerate
      |
      |  integer id;
      |  initial id = ila_source_allocate(
      |    "ps2Chisel",
      |    "io_ps2_clk:1,io_ps2_data:1,clk_buf:1,clk_buf_past:1,data_buf:1,sampleEdge:1,cnt:4,row_buffer:8,fifo_io_enq_valid:1,_fifo_io_enq_ready:1,overflow:1,_fifo_io_count:3,_fifo_io_deq_valid:1,fifo_io_deq_ready:1,_fifo_io_deq_bits:8,io_in_psel:1,io_in_penable:1,io_in_pready:1,io_in_pslverr:1,io_in_paddr:32,io_in_prdata:32",
      |    128
      |  );
      |  always_ff @(posedge clock) begin
      |    if (!reset) ila_sample(id, sampleWords);
      |  end
      |endmodule
      |""".stripMargin,
  )
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
  val sampleEdge = clk_buf && !clk_buf_past


  when (sampleEdge) {
    printf(p"ps2: $data_buf\n")
  }
  when (fifo.io.enq.valid) {
    printf(p"enq: ${fifo.io.enq.bits}\n")
  }

  when (sampleEdge) {
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


  
  if (sys.env.get("NPC_ENABLE_ILA").contains("1")) {
    val ila = Module(new PS2IlaProbeDPI)
    ila.io.clock := clock
    ila.io.reset := reset.asBool
    ila.io.sampleData := Cat(Seq(
      io.ps2.clk.asUInt,
      io.ps2.data.asUInt,
      clk_buf.asUInt,
      clk_buf_past.asUInt,
      data_buf.asUInt,
      sampleEdge.asUInt,
      cnt,
      row_buffer,
      fifo.io.enq.valid.asUInt,
      fifo.io.enq.ready.asUInt,
      overflow.asUInt,
      fifo.io.count,
      fifo.io.deq.valid.asUInt,
      fifo.io.deq.ready.asUInt,
      fifo.io.deq.bits,
      io.in.psel.asUInt,
      io.in.penable.asUInt,
      io.in.pready.asUInt,
      io.in.pslverr.asUInt,
      io.in.paddr,
      io.in.prdata,
    ).reverse).pad(128)
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
