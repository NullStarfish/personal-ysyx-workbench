package ysyx

import chisel3._
import chisel3.util._
import chisel3.experimental.Analog

import freechips.rocketchip.amba.axi4._
import freechips.rocketchip.amba.apb._
import org.chipsalliance.cde.config.Parameters
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.util._

class SDRAMIO extends Bundle {
  val clk = Output(Bool())
  val cke = Output(Bool())
  val cs  = Output(UInt(2.W))
  val ras = Output(Bool())
  val cas = Output(Bool())
  val we  = Output(Bool())
  val a   = Output(UInt(13.W))
  val ba  = Output(UInt(2.W))
  val dqm = Output(UInt(4.W))
  val dq  = Analog(32.W)
}

class SDRAMChipModelIO extends Bundle {
  val clk   = Input(Bool())
  val cke   = Input(Bool())
  val cs    = Input(Bool())
  val ras   = Input(Bool())
  val cas   = Input(Bool())
  val we    = Input(Bool())
  val a     = Input(UInt(13.W))
  val ba    = Input(UInt(2.W))
  val dqm   = Input(UInt(2.W))
  val dqIn  = Input(UInt(16.W))
  val dqOut = Output(UInt(16.W))
  val dqOe  = Output(Bool())
}

class sdram_top_axi extends BlackBox {
  val io = IO(new Bundle {
    val clock = Input(Clock())
    val reset = Input(Bool())
    val in = Flipped(new AXI4Bundle(AXI4BundleParameters(addrBits = 32, dataBits = 32, idBits = 4)))
    val sdram = new SDRAMIO
  })
}

class sdram_top_apb extends BlackBox {
  val io = IO(new Bundle {
    val clock = Input(Clock())
    val reset = Input(Bool())
    val in = Flipped(new APBBundle(APBBundleParameters(addrBits = 32, dataBits = 32)))
    val sdram = new SDRAMIO
  })
}

class sdram extends BlackBox {
  val io = IO(Flipped(new SDRAMIO))
}

class sdramChipCore(chipId: Int) extends RawModule {
  val io = IO(new SDRAMChipModelIO)
  val outEn = WireDefault(false.B)
  val outData = WireDefault(0.U(16.W))
  val dqIn = io.dqIn

  val readPort = Module(new dpi_sdram_read16(chipId))
  val writePort = Module(new dpi_sdram_write16(chipId))

  val BankCount = 4
  val RowWidth = 13
  val ColWidth = 9
  val BankWidth = 2
  val BurstMax = 8

  val CmdNop = "b0111".U(4.W)
  val CmdActive = "b0011".U(4.W)
  val CmdRead = "b0101".U(4.W)
  val CmdWrite = "b0100".U(4.W)
  val CmdPrecharge = "b0010".U(4.W)
  val CmdRefresh = "b0001".U(4.W)
  val CmdLoadMode = "b0000".U(4.W)

  def burstLenFromMode(code: UInt): UInt = {
    MuxLookup(code, 2.U(4.W))(Seq(
      "b000".U -> 1.U(4.W),
      "b001".U -> 2.U(4.W),
      "b010".U -> 4.U(4.W),
      "b011".U -> 8.U(4.W)
    ))
  }

  def casFromMode(code: UInt): UInt = {
    MuxLookup(code, 2.U(3.W))(Seq(
      "b010".U -> 2.U(3.W),
      "b011".U -> 3.U(3.W)
    ))
  }

  def linearHalfAddr(bank: UInt, row: UInt, col: UInt): UInt = {
    Cat(bank(BankWidth - 1, 0), row(RowWidth - 1, 0), col(ColWidth - 1, 0))
  }

  io.dqOut := outData
  io.dqOe := outEn

  withClockAndReset(io.clk.asClock, (!io.cke).asAsyncReset) {
    val cmd = WireDefault(CmdNop)
    when(!io.cs) {
      cmd := Cat(io.cs, io.ras, io.cas, io.we)
    }

    val rowOpen = RegInit(VecInit(Seq.fill(BankCount)(false.B)))
    val openRow = RegInit(VecInit(Seq.fill(BankCount)(0.U(RowWidth.W))))

    val modeBurst = RegInit(2.U(4.W))
    val modeCas = RegInit(2.U(3.W))

    val readActive = RegInit(false.B)
    val readDelay = RegInit(0.U(3.W))
    val readBank = RegInit(0.U(BankWidth.W))
    val readRow = RegInit(0.U(RowWidth.W))
    val readCol = RegInit(0.U(ColWidth.W))
    val readRemain = RegInit(0.U(4.W))

    val writeActive = RegInit(false.B)
    val writeBank = RegInit(0.U(BankWidth.W))
    val writeRow = RegInit(0.U(RowWidth.W))
    val writeCol = RegInit(0.U(ColWidth.W))
    val writeRemain = RegInit(0.U(4.W))
    val stagedWriteValid = RegInit(false.B)
    val stagedWriteAddr = RegInit(0.U((BankWidth + RowWidth + ColWidth).W))
    val stagedWriteData = RegInit(0.U(16.W))
    val stagedWriteMask = RegInit(0.U(2.W))

    val outEnReg = RegInit(false.B)
    val outDataReg = RegInit(0.U(16.W))
    outEn := outEnReg
    outData := outDataReg

    val currentReadAddr = Wire(UInt((BankWidth + RowWidth + ColWidth).W))
    currentReadAddr := linearHalfAddr(readBank, readRow, readCol)
    readPort.io.addr := currentReadAddr

    writePort.io.valid := stagedWriteValid
    writePort.io.clk := io.clk.asClock
    writePort.io.addr := stagedWriteAddr
    writePort.io.wdata := stagedWriteData
    writePort.io.wmask := stagedWriteMask

    outEnReg := false.B
    stagedWriteValid := false.B

    when(readActive) {
      when(readDelay =/= 0.U) {
        readDelay := readDelay - 1.U
      } .otherwise {
        outEnReg := true.B
        outDataReg := readPort.io.rdata
        when(readRemain === 1.U) {
          readActive := false.B
        } .otherwise {
          readRemain := readRemain - 1.U
          readCol := readCol + 1.U
        }
      }
    }

    when(writeActive) {
      stagedWriteValid := true.B
      stagedWriteAddr := linearHalfAddr(writeBank, writeRow, writeCol)
      stagedWriteData := dqIn
      stagedWriteMask := ~io.dqm
      when(writeRemain === 1.U) {
        writeActive := false.B
      } .otherwise {
        writeRemain := writeRemain - 1.U
        writeCol := writeCol + 1.U
      }
    }

    when(!io.cs) {
      switch(cmd) {
        is(CmdActive) {
          rowOpen(io.ba) := true.B
          openRow(io.ba) := io.a
        }

        is(CmdLoadMode) {
          modeBurst := burstLenFromMode(io.a(2, 0))
          modeCas := casFromMode(io.a(6, 4))
        }

        is(CmdRead) {
          when(rowOpen(io.ba)) {
            readActive := true.B
            readBank := io.ba
            readRow := openRow(io.ba)
            readCol := io.a(ColWidth - 1, 0)
            readRemain := modeBurst
            readDelay := Mux(modeCas > 1.U, modeCas - 2.U, 0.U)
          }
        }

        is(CmdWrite) {
          when(rowOpen(io.ba)) {
            val cmdWriteAddr = linearHalfAddr(io.ba, openRow(io.ba), io.a(ColWidth - 1, 0))
            stagedWriteValid := true.B
            stagedWriteAddr := cmdWriteAddr
            stagedWriteData := dqIn
            stagedWriteMask := ~io.dqm
            when(modeBurst === 1.U) {
              writeActive := false.B
            } .otherwise {
              writeActive := true.B
              writeBank := io.ba
              writeRow := openRow(io.ba)
              writeCol := io.a(ColWidth - 1, 0) + 1.U
              writeRemain := modeBurst - 1.U
            }
          }
        }

        is(CmdPrecharge) {
          when(io.a(10)) {
            rowOpen.foreach(_ := false.B)
          } .otherwise {
            rowOpen(io.ba) := false.B
          }
        }

        is(CmdRefresh) {
        }
      }
    }
  }
}

class sdramChisel extends RawModule {
  val io = IO(Flipped(new SDRAMIO))

  val outEn = WireDefault(false.B)
  val outData = WireDefault(0.U(32.W))
  val dqIn = TriStateInBuf(io.dq, outData, outEn)

  val rank0Lower = Module(new sdramChipCore(0))
  val rank0Upper = Module(new sdramChipCore(1))
  val rank1Lower = Module(new sdramChipCore(2))
  val rank1Upper = Module(new sdramChipCore(3))

  val allChips = Seq(rank0Lower, rank0Upper, rank1Lower, rank1Upper)
  for (chip <- allChips) {
    chip.io.clk := io.clk
    chip.io.cke := io.cke
    chip.io.ras := io.ras
    chip.io.cas := io.cas
    chip.io.we := io.we
    chip.io.a := io.a
    chip.io.ba := io.ba
    chip.io.dqIn := 0.U
    chip.io.dqm := 0.U
  }

  rank0Lower.io.cs := io.cs(0)
  rank0Upper.io.cs := io.cs(0)
  rank1Lower.io.cs := io.cs(1)
  rank1Upper.io.cs := io.cs(1)

  rank0Lower.io.dqm := io.dqm(1, 0)
  rank0Upper.io.dqm := io.dqm(3, 2)
  rank1Lower.io.dqm := io.dqm(1, 0)
  rank1Upper.io.dqm := io.dqm(3, 2)

  rank0Lower.io.dqIn := dqIn(15, 0)
  rank0Upper.io.dqIn := dqIn(31, 16)
  rank1Lower.io.dqIn := dqIn(15, 0)
  rank1Upper.io.dqIn := dqIn(31, 16)

  val rank0Oe = rank0Lower.io.dqOe || rank0Upper.io.dqOe
  val rank1Oe = rank1Lower.io.dqOe || rank1Upper.io.dqOe

  outEn := rank0Oe || rank1Oe
  outData := Mux(rank1Oe,
    Cat(rank1Upper.io.dqOut, rank1Lower.io.dqOut),
    Cat(rank0Upper.io.dqOut, rank0Lower.io.dqOut)
  )
}

class dpi_sdram_read16(chipId: Int) extends BlackBox(Map("CHIP_ID" -> chipId)) with HasBlackBoxInline {
  val io = IO(new Bundle {
    val addr = Input(UInt(24.W))
    val rdata = Output(UInt(16.W))
  })

  setInline(
    "dpi_sdram_read16.sv",
    """module dpi_sdram_read16 #(
      |  parameter int CHIP_ID = 0
      |)(
      |  input  logic [23:0] addr,
      |  output logic [15:0] rdata
      |);
      |  import "DPI-C" function void sdram_read_halfword_chip(input int chip, input int addr, output shortint unsigned data);
      |  logic [15:0] rdata_reg;
      |  always @(*) begin
      |    sdram_read_halfword_chip(CHIP_ID, {8'h00, addr}, rdata_reg);
      |  end
      |  assign rdata = rdata_reg;
      |endmodule
      |""".stripMargin
  )
}

class dpi_sdram_write16(chipId: Int) extends BlackBox(Map("CHIP_ID" -> chipId)) with HasBlackBoxInline {
  val io = IO(new Bundle {
    val clk = Input(Clock())
    val valid = Input(Bool())
    val addr = Input(UInt(24.W))
    val wdata = Input(UInt(16.W))
    val wmask = Input(UInt(2.W))
  })

  setInline(
    "dpi_sdram_write16.sv",
    """module dpi_sdram_write16 #(
      |  parameter int CHIP_ID = 0
      |)(
      |  input logic        clk,
      |  input logic        valid,
      |  input logic [23:0] addr,
      |  input logic [15:0] wdata,
      |  input logic [1:0]  wmask
      |);
      |  import "DPI-C" function void sdram_write_halfword_chip(input int chip, input int addr, input shortint unsigned data, input byte unsigned mask);
      |  always_ff @(posedge clk) begin
      |    if (valid) begin
      |      sdram_write_halfword_chip(CHIP_ID, {8'h00, addr}, wdata, {6'b0, wmask});
      |    end
      |  end
      |endmodule
      |""".stripMargin
  )
}

class AXI4SDRAM(address: Seq[AddressSet])(implicit p: Parameters) extends LazyModule {
  val beatBytes = 4
  val node = AXI4SlaveNode(Seq(AXI4SlavePortParameters(
    Seq(AXI4SlaveParameters(
        address       = address,
        executable    = true,
        supportsWrite = TransferSizes(1, beatBytes),
        supportsRead  = TransferSizes(1, beatBytes),
        interleavedId = Some(0))
    ),
    beatBytes  = beatBytes)))

  lazy val module = new Impl
  class Impl extends LazyModuleImp(this) {
    val (in, _) = node.in(0)
    val sdram_bundle = IO(new SDRAMIO)

    val msdram = Module(new sdram_top_axi)
    msdram.io.clock := clock
    msdram.io.reset := reset.asBool
    msdram.io.in <> in
    sdram_bundle <> msdram.io.sdram
  }
}

class APBSDRAM(address: Seq[AddressSet])(implicit p: Parameters) extends LazyModule {
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
    val sdram_bundle = IO(new SDRAMIO)

    val msdram = Module(new sdram_top_apb)
    msdram.io.clock := clock
    msdram.io.reset := reset.asBool
    msdram.io.in <> in
    sdram_bundle <> msdram.io.sdram
  }
}
