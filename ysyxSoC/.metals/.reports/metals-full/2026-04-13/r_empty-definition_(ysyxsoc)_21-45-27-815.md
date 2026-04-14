error id: file://<WORKSPACE>/src/device/PSRAM.scala:chisel3/package.fromBigIntToLiteral#U().
file://<WORKSPACE>/src/device/PSRAM.scala
empty definition using pc, found symbol in pc: chisel3/package.fromBigIntToLiteral#U().
empty definition using semanticdb
empty definition using fallback
non-local guesses:
	 -chisel3.
	 -chisel3#
	 -chisel3().
	 -chisel3/util.
	 -chisel3/util#
	 -chisel3/util().
	 -freechips/rocketchip/amba/apb.
	 -freechips/rocketchip/amba/apb#
	 -freechips/rocketchip/amba/apb().
	 -freechips/rocketchip/diplomacy.
	 -freechips/rocketchip/diplomacy#
	 -freechips/rocketchip/diplomacy().
	 -freechips/rocketchip/util.
	 -freechips/rocketchip/util#
	 -freechips/rocketchip/util().
	 -scala/Predef.
	 -scala/Predef#
	 -scala/Predef().
offset: 888
uri: file://<WORKSPACE>/src/device/PSRAM.scala
text:
```scala
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
  val mem = RegInit(VecInit(Seq.fill(4096)(0.U(8.W))))
  
  
  val out_en = WireDefault(false.B)

  val di = TriStateInBuf(io.dio, 0.U@@, out_en) // change this if you need

  withClockAndReset(io.sck.asClock, !io.ce_n) {
    val state = RegInit(0.U(3.W))

    val CMD = 0
    val ADDR = 1
    val WAIT = 2
    val DATA = 3
    val NOT_VALID = 4




    val counter = RegInit(0.U(10.W))
    when(io.ce_n) {
      counter := counter + 1.U
    }


    val cmd_reg = RegInit(0.U(8.W))
    val mode_reg = RegInit(false.B)
    when (state === CMD.U) {
      counter := counter + 1.U
      cmd_reg := (cmd_reg << 1) | (di(0))
      when (counter === 7.U ) {
        state := NOT_VALID.U
        counter := 0.U
        switch ((cmd_reg << 1) | di) {
          is(0xEB.U) {
            mode_reg := false.B
            state := ADDR.U
          }
          is(0x38.U) {
            mode_reg := true.B
            state := ADDR.U
          }
        }
      }
    }

    out_en := (mode_reg === false.B) && (state === DATA.U)

    val addr_reg = RegInit(0.U(12.W))
    when (state === ADDR.U) {
      counter := counter + 1.U
      addr_reg := (addr_reg << 1) | di
      when (counter === 11.U) {
        when (mode_reg === false.B) {
          state := WAIT.U
        } .otherwise {
          state := DATA.U
        }
        counter := 0.U
      }
    }


    when (state === WAIT.U) {

    }


  }



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

```


#### Short summary: 

empty definition using pc, found symbol in pc: chisel3/package.fromBigIntToLiteral#U().