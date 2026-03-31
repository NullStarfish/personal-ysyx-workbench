package mycpu.mem

import HwOS.kernel.process.HwProcess
import HwOS.kernel.system.{Kernel, SysCall}
import chisel3._
import chisel3.simulator.EphemeralSimulator._
import chisel3.util._
import mycpu.axi._
import mycpu.common._
import org.scalatest.flatspec.AnyFlatSpec

class MemoryReadHarness extends Module {
  val io = IO(new Bundle {
    val firstDone = Output(Bool())
    val secondDone = Output(Bool())
    val allDone = Output(Bool())
    val firstData = Output(UInt(XLEN.W))
    val secondData = Output(UInt(XLEN.W))
  })

  implicit val kernel: Kernel = new Kernel()

  val bus = Wire(new AXI4Bundle(AXI_ID_WIDTH, XLEN, XLEN))
  bus.setAsMasterInit()

  io.firstDone := DontCare
  io.secondDone := DontCare
  io.allDone := DontCare
  io.firstData := DontCare
  io.secondData := DontCare

  object Init extends HwProcess("Init") {
    val memory = spawn(new Memory(bus, maxClients = 2))
    val reader0 = createThread("Reader0")
    val reader1 = createThread("Reader1")
    val main = createThread("Main")

    val firstDoneReg = RegInit(false.B)
    val secondDoneReg = RegInit(false.B)
    val firstDataReg = RegInit(0.U(XLEN.W))
    val secondDataReg = RegInit(0.U(XLEN.W))

    override def entry(): Unit = {
      reader0.entry {
        val api = SysCall.Inline(memory.RequestMemoryApi(0))
        val value = SysCall.Inline(api.read_once(1.U, 2.U))
        reader0.Prev.edge.add {
          firstDoneReg := true.B
          firstDataReg := value
          printf(p"read1 get value: $value\n")
        }
        SysCall.Return()
      }

      reader1.entry {
        val api = SysCall.Inline(memory.RequestMemoryApi(1))
        val value = SysCall.Inline(api.read_once(2.U, 2.U))
        reader1.Prev.edge.add {
          secondDataReg := value
          secondDoneReg := true.B
          printf(p"read2 get value: $value\n")
        }
        SysCall.Return()
      }

      main.entry {
        main.Step("StartBoth") {
          SysCall.Inline(SysCall.start(reader0))
          SysCall.Inline(SysCall.start(reader1))
        }
        main.Step("WaitBoth") {
          main.waitCondition(reader0.done && reader1.done)
          when(reader0.done && reader1.done) { main.hijack(main.Next) }
        }
        main.Step("Finish") {}
        SysCall.Return()
      }

      val daemon = createLogic("Daemon")
      daemon.run {
        when(!main.active && !main.done) {
          SysCall.Inline(SysCall.start(main))
        }
        io.firstDone := firstDoneReg
        io.secondDone := secondDoneReg
        io.firstData := firstDataReg
        io.secondData := secondDataReg
        io.allDone := main.done
      }
    }
  }

  Init.build()

  val readPending = RegInit(false.B)
  val readAddr = RegInit(0.U(XLEN.W))
  val readDelay = RegInit(0.U(2.W))
  val readData = MuxLookup(readAddr, 0.U(XLEN.W))(
    Seq(
      1.U -> "h11112222".U(XLEN.W),
      2.U -> "h33334444".U(XLEN.W),
    )
  )

  bus.aw.ready := false.B
  bus.w.ready := false.B
  bus.b.valid := false.B
  bus.b.bits := DontCare

  bus.ar.ready := !readPending
  bus.r.valid := false.B
  bus.r.bits := DontCare

  when(bus.ar.valid && bus.ar.ready) {
    readPending := true.B
    readAddr := bus.ar.bits.addr
    readDelay := 0.U
  }

  when(readPending) {
    readDelay := readDelay + 1.U
    when(readDelay >= 1.U) {
      bus.r.valid := true.B
      bus.r.bits.id := 0.U
      bus.r.bits.data := readData
      bus.r.bits.resp := AXI4Parameters.RESP_OKAY
      bus.r.bits.last := true.B
      when(bus.r.ready) {
        readPending := false.B
      }
    }
  }
}

class MemorySpec extends AnyFlatSpec {
  "Memory" should "serialize two clients through the mutex-backed AXI bus service" in {
    simulate(new MemoryReadHarness) { c =>
      c.reset.poke(true.B)
      c.clock.step()
      c.reset.poke(false.B)

      var firstPhaseCycles = 0
      while (c.io.firstDone.peek().litValue == 0 && firstPhaseCycles < 30) {
        c.clock.step()
        firstPhaseCycles += 1
      }

      c.io.firstDone.expect(true.B)
      c.io.secondDone.expect(false.B)

      var cycles = 0
      while (c.io.allDone.peek().litValue == 0 && cycles < 30) {
        c.clock.step()
        cycles += 1
      }

      c.io.allDone.expect(true.B)
      c.io.firstData.expect("h11112222".U)
      c.io.secondData.expect("h33334444".U)
      c.io.firstDone.expect(true.B)
      c.io.secondDone.expect(true.B)
    }
  }
}
