package ysyx

import chisel3._
import chisel3.util._
import chisel3.experimental.Analog

import freechips.rocketchip.amba.apb._
import org.chipsalliance.cde.config.Parameters
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.util._

class QSPIIO extends Bundle {
  val sck = Output(Bool())
  val ce_n = Output(Bool())
  val dio = Analog(4.W)
}

class psram_top_apb extends BlackBox {
  val io = IO(new Bundle {
    val clock = Input(Clock())
    val reset = Input(Reset())
    val in = Flipped(new APBBundle(APBBundleParameters(addrBits = 32, dataBits = 32)))
    val qspi = new QSPIIO
  })
}

class psram extends BlackBox {
  val io = IO(Flipped(new QSPIIO))
}

class psramChisel extends RawModule {
  val io = IO(Flipped(new QSPIIO))
  val out_en = WireDefault(false.B)
  val out_data = WireDefault(0.U(4.W))
  val di = TriStateInBuf(io.dio, out_data, out_en)

  val readPort = Module(new dpi_sram)
  val writePort = Module(new dpi_sram)

  val readAddr = WireDefault(0.U(24.W))
  val readKick = WireDefault(false.B)
  val writeKick = WireDefault(false.B)
  val writeAddr = WireDefault(0.U(24.W))
  val writeData = WireDefault(0.U(8.W))

  readPort.io.clk := io.sck.asClock
  readPort.io.addr := readAddr
  readPort.io.wdata := 0.U
  readPort.io.wen := false.B
  readPort.io.valid := readKick

  writePort.io.clk := io.sck.asClock
  writePort.io.addr := writeAddr
  writePort.io.wdata := writeData
  writePort.io.wen := writeKick
  writePort.io.valid := writeKick

  val stateCmd :: stateAddr :: stateDummy :: stateRead :: stateWrite :: Nil = Enum(5)
  val negSck = (!io.sck.asUInt.asBool).asClock

  val desiredOutEn = WireDefault(false.B)
  val desiredOutData = WireDefault(0.U(4.W))
  val enterQpi = WireDefault(false.B)
  val exitQpi = WireDefault(false.B)

  val qpiMode = withClockAndReset(io.sck.asClock, false.B.asAsyncReset) {
    val reg = RegInit(false.B)
    when(enterQpi) { reg := true.B }
    when(exitQpi) { reg := false.B }
    reg
  }

  withClockAndReset(io.sck.asClock, io.ce_n.asAsyncReset) {
    val state = RegInit(stateCmd)
    val bitCounter = RegInit(0.U(4.W))
    val nibbleCounter = RegInit(0.U(4.W))
    val byteCounter = RegInit(0.U(3.W))
    val cmdReg = RegInit(0.U(8.W))
    val addrReg = RegInit(0.U(24.W))
    val readLineReg = RegInit(0.U(32.W))
    val writeByteReg = RegInit(0.U(8.W))

    when(state === stateDummy && nibbleCounter >= 1.U && nibbleCounter <= 4.U) {
      readAddr := addrReg + (nibbleCounter - 1.U)
      readKick := true.B
    }

    when(state === stateCmd) {
      when(qpiMode) {
        cmdReg := Cat(cmdReg(3, 0), di)
        bitCounter := bitCounter + 1.U
        when(bitCounter === 1.U) {
          bitCounter := 0.U
          addrReg := 0.U
          switch(Cat(cmdReg(3, 0), di)) {
            is("hEB".U) { state := stateAddr }
            is("h38".U) { state := stateAddr }
            is("hF5".U) { exitQpi := true.B }
          }
        }
      } .otherwise {
        cmdReg := (cmdReg << 1) | di(0)
        bitCounter := bitCounter + 1.U
        when(bitCounter === 7.U) {
          bitCounter := 0.U
          addrReg := 0.U
          switch((cmdReg << 1) | di(0)) {
            is("hEB".U) { state := stateAddr }
            is("h38".U) { state := stateAddr }
            is("h35".U) { enterQpi := true.B }
          }
        }
      }
    }

    when(state === stateAddr) {
      addrReg := Cat(addrReg(19, 0), di)
      nibbleCounter := nibbleCounter + 1.U
      when(nibbleCounter === 5.U) {
        nibbleCounter := 0.U
        byteCounter := 0.U
        when(cmdReg === "hEB".U) {
          state := stateDummy
        } .otherwise {
          state := stateWrite
        }
      }
    }

    when(state === stateDummy) {
      nibbleCounter := nibbleCounter + 1.U
      switch(nibbleCounter) {
        is(1.U) {
          readLineReg := Cat(readLineReg(31, 8), readPort.io.rdata)
        }
        is(2.U) {
          readLineReg := Cat(readLineReg(31, 16), readPort.io.rdata, readLineReg(7, 0))
        }
        is(3.U) {
          readLineReg := Cat(readLineReg(31, 24), readPort.io.rdata, readLineReg(15, 0))
        }
        is(4.U) {
          readLineReg := Cat(readPort.io.rdata, readLineReg(23, 0))
        }
      }
      when(nibbleCounter === 5.U) {
        nibbleCounter := 0.U
        state := stateRead
      }
    }

    when(state === stateRead) {
      when(nibbleCounter === 7.U) {
        nibbleCounter := 0.U
        state := stateCmd
      } .otherwise {
        nibbleCounter := nibbleCounter + 1.U
      }
    }

    when(state === stateWrite) {
      writeByteReg := Cat(writeByteReg(3, 0), di)
      nibbleCounter := nibbleCounter + 1.U
      when(nibbleCounter(0)) {
        writeKick := true.B
        writeAddr := addrReg + byteCounter
        writeData := Cat(writeByteReg(3, 0), di)
        byteCounter := byteCounter + 1.U
      }
    }

    desiredOutEn := state === stateRead

    when(state === stateRead) {
      switch(nibbleCounter) {
        is(0.U) { desiredOutData := readLineReg(7, 4) }
        is(1.U) { desiredOutData := readLineReg(3, 0) }
        is(2.U) { desiredOutData := readLineReg(15, 12) }
        is(3.U) { desiredOutData := readLineReg(11, 8) }
        is(4.U) { desiredOutData := readLineReg(23, 20) }
        is(5.U) { desiredOutData := readLineReg(19, 16) }
        is(6.U) { desiredOutData := readLineReg(31, 28) }
        is(7.U) { desiredOutData := readLineReg(27, 24) }
      }
    }

  }

  withClockAndReset(negSck, io.ce_n.asAsyncReset) {
    val outEnReg = RegInit(false.B)
    val outDataReg = RegInit(0.U(4.W))
    outEnReg := desiredOutEn
    outDataReg := desiredOutData
    out_en := outEnReg
    out_data := outDataReg

  }
}



class dpi_sram extends BlackBox with HasBlackBoxInline {
  val io = IO(new Bundle {
    val clk = Input(Clock())
    val addr = Input(UInt(24.W))
    val rdata = Output(UInt(8.W))
    val wdata = Input(UInt(8.W))
    val wen = Input(Bool())
    val valid = Input(Bool())
  })

  setInline(
    "dpi_sram.sv",
    """module dpi_sram(
      |   input  logic        clk,
      |   input  logic [23:0] addr,
      |   output logic [7:0] rdata,
      |   input  logic [7:0]  wdata,
      |   input  logic        wen,
      |   input  logic        valid
      |);
      |
      |  import "DPI-C" function void psram_read_byte(input int addr, output byte data);
      |  import "DPI-C" function void psram_write_byte(input int addr, input byte data);
      |  logic [7:0] rdata_reg;
      |
      |  always_ff @(negedge clk) begin
      |    if (valid) begin
      |      if (wen) psram_write_byte({8'h00, addr}, wdata);
      |      else     psram_read_byte({8'h00, addr}, rdata_reg);
      |    end
      |  end
      |
      |  assign rdata = rdata_reg;
      |endmodule
      |""".stripMargin
  )

}

class APBPSRAM(address: Seq[AddressSet])(implicit p: Parameters) extends LazyModule {
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
    val qspi_bundle = IO(new QSPIIO)

    val mpsram = Module(new psram_top_apb)
    mpsram.io.clock := clock
    mpsram.io.reset := reset
    mpsram.io.in <> in
    qspi_bundle <> mpsram.io.qspi
  }
}
