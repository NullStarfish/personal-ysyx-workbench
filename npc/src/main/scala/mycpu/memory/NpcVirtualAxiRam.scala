package mycpu.memory

import chisel3._
import chisel3.util._
import chisel3.util.HasBlackBoxInline
import mycpu.utils.AXI4Parameters
import _root_.circt.stage.ChiselStage

final class NpcVirtualMemDPI extends BlackBox with HasBlackBoxInline {
  val io = IO(new Bundle {
    val clock = Input(Clock())
    val read_en = Input(Bool())
    val read_addr = Input(UInt(32.W))
    val read_data = Output(UInt(32.W))
    val write_en = Input(Bool())
    val write_addr = Input(UInt(32.W))
    val write_data = Input(UInt(32.W))
    val write_strb = Input(UInt(4.W))
  })

  setInline(
    "NpcVirtualMemDPI.sv",
    """module NpcVirtualMemDPI(
      |  input  logic        clock,
      |  input  logic        read_en,
      |  input  logic [31:0] read_addr,
      |  output logic [31:0] read_data,
      |  input  logic        write_en,
      |  input  logic [31:0] write_addr,
      |  input  logic [31:0] write_data,
      |  input  logic [3:0]  write_strb
      |);
      |  import "DPI-C" function int pmemread(input int raddr);
      |  import "DPI-C" function void pmemwrite(input int waddr, input int wdata, input byte wmask);
      |  import "DPI-C" function void clint_mtime_read(output longint mtime);
      |
      |  localparam logic [31:0] PMEM_BASE = 32'ha0000000;
      |  localparam logic [31:0] PMEM_END  = 32'ha8000000;
      |  localparam logic [31:0] UART_BASE = 32'h10000000;
      |  localparam logic [31:0] RTC_BASE  = 32'h02000000;
      |
      |  function automatic logic [31:0] virtual_read(input logic [31:0] addr);
      |    longint mtime;
      |    logic [31:0] aligned;
      |    begin
      |      virtual_read = 32'b0;
      |      mtime = 64'b0;
      |      aligned = addr & 32'hfffffffc;
      |      if (aligned >= PMEM_BASE && aligned < PMEM_END) begin
      |        virtual_read = pmemread(aligned);
      |      end else if (aligned == RTC_BASE || aligned == RTC_BASE + 32'd4) begin
      |        clint_mtime_read(mtime);
      |        virtual_read = (aligned == RTC_BASE) ? mtime[31:0] : mtime[63:32];
      |      end else if (aligned == UART_BASE + 32'd4) begin
      |        virtual_read = 32'h00002000;
      |      end
      |    end
      |  endfunction
      |
      |  task automatic virtual_write(
      |    input logic [31:0] addr,
      |    input logic [31:0] data,
      |    input logic [3:0]  strb
      |  );
      |    logic [31:0] aligned;
      |    begin
      |      aligned = addr & 32'hfffffffc;
      |      if (aligned >= PMEM_BASE && aligned < PMEM_END) begin
      |        pmemwrite(aligned, data, {4'b0, strb});
      |      end else if (aligned == UART_BASE) begin
      |        if (strb[addr[1:0]]) begin
      |          case (addr[1:0])
      |            2'd0: $write("%c", data[7:0]);
      |            2'd1: $write("%c", data[15:8]);
      |            2'd2: $write("%c", data[23:16]);
      |            default: $write("%c", data[31:24]);
      |          endcase
      |          $fflush();
      |        end
      |      end
      |    end
      |  endtask
      |
      |  always_comb begin
      |    read_data = read_en ? virtual_read(read_addr) : 32'b0;
      |  end
      |
      |  always_ff @(posedge clock) begin
      |    if (write_en) begin
      |      virtual_write(write_addr, write_data, write_strb);
      |    end
      |  end
      |endmodule
      |""".stripMargin,
  )
}

class NpcVirtualAxiRam(
    readLatency: Int = 1,
    writeLatency: Int = 1,
) extends Module {
  val io = IO(new Bundle {
    val aw_valid = Input(Bool())
    val aw_ready = Output(Bool())
    val aw_addr = Input(UInt(32.W))
    val aw_id = Input(UInt(4.W))
    val aw_len = Input(UInt(8.W))
    val aw_size = Input(UInt(3.W))
    val aw_burst = Input(UInt(2.W))

    val w_valid = Input(Bool())
    val w_ready = Output(Bool())
    val w_data = Input(UInt(32.W))
    val w_strb = Input(UInt(4.W))
    val w_last = Input(Bool())

    val b_valid = Output(Bool())
    val b_ready = Input(Bool())
    val b_resp = Output(UInt(2.W))
    val b_id = Output(UInt(4.W))

    val ar_valid = Input(Bool())
    val ar_ready = Output(Bool())
    val ar_addr = Input(UInt(32.W))
    val ar_id = Input(UInt(4.W))
    val ar_len = Input(UInt(8.W))
    val ar_size = Input(UInt(3.W))
    val ar_burst = Input(UInt(2.W))

    val r_valid = Output(Bool())
    val r_ready = Input(Bool())
    val r_resp = Output(UInt(2.W))
    val r_data = Output(UInt(32.W))
    val r_last = Output(Bool())
    val r_id = Output(UInt(4.W))
  })

  val dpi = Module(new NpcVirtualMemDPI)
  dpi.io.clock := clock
  dpi.io.read_en := false.B
  dpi.io.read_addr := 0.U
  dpi.io.write_en := false.B
  dpi.io.write_addr := 0.U
  dpi.io.write_data := 0.U
  dpi.io.write_strb := 0.U

  def latencyValue(latency: Int): UInt = {
    require(latency >= 1, "virtual AXI RAM latency must be at least one cycle")
    (latency - 1).U(32.W)
  }

  def nextAddr(addr: UInt, size: UInt, burst: UInt): UInt = {
    val step = 1.U(32.W) << size
    Mux(burst === AXI4Parameters.BURST_FIXED, addr, addr + step)
  }

  val readIdle :: readDelay :: readResp :: Nil = Enum(3)
  val readState = RegInit(readIdle)
  val readAddr = RegInit(0.U(32.W))
  val readId = RegInit(0.U(4.W))
  val readLen = RegInit(0.U(8.W))
  val readSize = RegInit(0.U(3.W))
  val readBurst = RegInit(AXI4Parameters.BURST_INCR)
  val readBeat = RegInit(0.U(8.W))
  val readDelayCnt = RegInit(0.U(32.W))

  val writeIdle :: writeData :: writeDelay :: writeResp :: Nil = Enum(4)
  val writeState = RegInit(writeIdle)
  val writeAddr = RegInit(0.U(32.W))
  val writeId = RegInit(0.U(4.W))
  val writeLen = RegInit(0.U(8.W))
  val writeSize = RegInit(0.U(3.W))
  val writeBurst = RegInit(AXI4Parameters.BURST_INCR)
  val writeBeat = RegInit(0.U(8.W))
  val writeDelayCnt = RegInit(0.U(32.W))

  io.ar_ready := readState === readIdle
  io.r_valid := readState === readResp
  io.r_resp := AXI4Parameters.RESP_OKAY
  io.r_id := readId
  io.r_last := readBeat === readLen
  io.r_data := dpi.io.read_data

  dpi.io.read_en := readState === readResp
  dpi.io.read_addr := readAddr

  when(io.ar_valid && io.ar_ready) {
    readAddr := io.ar_addr
    readId := io.ar_id
    readLen := io.ar_len
    readSize := io.ar_size
    readBurst := io.ar_burst
    readBeat := 0.U
    readDelayCnt := latencyValue(readLatency)
    readState := readDelay
  }

  when(readState === readDelay) {
    when(readDelayCnt === 0.U) {
      readState := readResp
    }.otherwise {
      readDelayCnt := readDelayCnt - 1.U
    }
  }

  when(io.r_valid && io.r_ready) {
    when(io.r_last) {
      readState := readIdle
    }.otherwise {
      readAddr := nextAddr(readAddr, readSize, readBurst)
      readBeat := readBeat + 1.U
      readDelayCnt := latencyValue(readLatency)
      readState := readDelay
    }
  }

  io.aw_ready := writeState === writeIdle
  io.w_ready := writeState === writeData
  io.b_valid := writeState === writeResp
  io.b_resp := AXI4Parameters.RESP_OKAY
  io.b_id := writeId

  when(io.aw_valid && io.aw_ready) {
    writeAddr := io.aw_addr
    writeId := io.aw_id
    writeLen := io.aw_len
    writeSize := io.aw_size
    writeBurst := io.aw_burst
    writeBeat := 0.U
    writeState := writeData
  }

  when(io.w_valid && io.w_ready) {
    dpi.io.write_en := true.B
    dpi.io.write_addr := writeAddr
    dpi.io.write_data := io.w_data
    dpi.io.write_strb := io.w_strb

    val isLastBeat = writeBeat === writeLen || io.w_last
    when(isLastBeat) {
      writeDelayCnt := latencyValue(writeLatency)
      writeState := writeDelay
    }.otherwise {
      writeAddr := nextAddr(writeAddr, writeSize, writeBurst)
      writeBeat := writeBeat + 1.U
    }
  }

  when(writeState === writeDelay) {
    when(writeDelayCnt === 0.U) {
      writeState := writeResp
    }.otherwise {
      writeDelayCnt := writeDelayCnt - 1.U
    }
  }

  when(io.b_valid && io.b_ready) {
    writeState := writeIdle
  }
}

object GenNpcVirtualAxiRam extends App {
  ChiselStage.emitSystemVerilogFile(
    new NpcVirtualAxiRam,
    args = Array("--target-dir", sys.env.getOrElse("NPC_RTL_DIR", "build/rtl/Core")),
    firtoolOpts = Array(
      "--disable-all-randomization",
      "--lowering-options=disallowLocalVariables",
    ),
  )
}
