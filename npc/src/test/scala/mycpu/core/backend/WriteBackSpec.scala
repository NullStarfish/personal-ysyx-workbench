package mycpu.core.backend

import chisel3._
import chisel3.simulator.EphemeralSimulator._
import org.scalatest.flatspec.AnyFlatSpec

class WriteBackSpec extends AnyFlatSpec {
  "WriteBack" should "drive register write only when wb.regWen is set" in {
    simulate(new WriteBack) { c =>
      c.io.in.valid.poke(true.B)
      c.io.in.bits.wbData.poke("hdeadbeef".U)
      c.io.in.bits.wb.regWen.poke(true.B)
      c.io.in.bits.wb.rd.poke(7.U)
      c.clock.step()

      c.io.regWrite.wen.expect(true.B)
      c.io.regWrite.addr.expect(7.U)
      c.io.regWrite.data.expect("hdeadbeef".U)

      c.io.in.bits.wb.regWen.poke(false.B)
      c.clock.step()
      c.io.regWrite.wen.expect(false.B)
    }
  }
}
