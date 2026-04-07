package mycpu.axi

import HwOS.kernel.process.HwProcess
import HwOS.kernel.system.{Kernel, SysCall}
import chisel3._
import chisel3.simulator.EphemeralSimulator._
import mycpu.common._
import org.scalatest.flatspec.AnyFlatSpec

class AxiSlaveReadHarness extends Module {
  val io = IO(new Bundle {
    val start = Input(Bool())
    val addr = Input(UInt(XLEN.W))
    val masterDone = Output(Bool())
    val masterData = Output(UInt(XLEN.W))
    val slaveSeenAddr = Output(UInt(XLEN.W))
    val slaveSeenSize = Output(UInt(3.W))
  })

  implicit val kernel: Kernel = new Kernel()

  val bus = Wire(new AXI4Bundle(AXI_ID_WIDTH, XLEN, XLEN))
  bus.setAsMasterInit()
  bus.setAsSlaveInit()

  io.masterDone := DontCare
  io.masterData := DontCare
  io.slaveSeenAddr := DontCare
  io.slaveSeenSize := DontCare

  object Init extends HwProcess("Init") {
    private val master = createThread("Master")
    private val slave = createThread("Slave")
    private val daemon = createLogic("Daemon")

    private val addrReg = RegInit(0.U(XLEN.W))
    private val masterDoneReg = RegInit(false.B)
    private val masterDataReg = RegInit(0.U(XLEN.W))
    private val slaveAddrReg = RegInit(0.U(XLEN.W))
    private val slaveSizeReg = RegInit(0.U(3.W))

    override def entry(): Unit = {
      master.entry {
        val value = SysCall.Inline(AXI4Api.axi_read_once(bus, 0.U, addrReg, 2.U))
        master.Prev.edge.add {
          masterDataReg := value
          masterDoneReg := true.B
        }
        SysCall.Return()
      }

      slave.entry {
        val req = SysCall.Inline(AXI4SlaveApi.axi_listen_read_addr_once(bus))
        slave.Prev.edge.add {
          slaveAddrReg := req.addr
          slaveSizeReg := req.size
        }
        SysCall.Inline(AXI4SlaveApi.axi_send_read_data_once(bus, req.id, "h55667788".U(XLEN.W)))
        SysCall.Return()
      }

      daemon.run {
        when(io.start && !master.active && !slave.active) {
          addrReg := io.addr
          masterDoneReg := false.B
          SysCall.Inline(SysCall.start(slave))
          SysCall.Inline(SysCall.start(master))
        }

        io.masterDone := masterDoneReg
        io.masterData := masterDataReg
        io.slaveSeenAddr := slaveAddrReg
        io.slaveSeenSize := slaveSizeReg
      }
    }
  }

  Init.build()
}

class AxiSlaveWriteHarness extends Module {
  val io = IO(new Bundle {
    val start = Input(Bool())
    val addr = Input(UInt(XLEN.W))
    val dataIn = Input(UInt(XLEN.W))
    val strb = Input(UInt((XLEN / 8).W))
    val masterDone = Output(Bool())
    val slaveSeenAddr = Output(UInt(XLEN.W))
    val slaveSeenData = Output(UInt(XLEN.W))
    val slaveSeenStrb = Output(UInt((XLEN / 8).W))
  })

  implicit val kernel: Kernel = new Kernel()

  val bus = Wire(new AXI4Bundle(AXI_ID_WIDTH, XLEN, XLEN))
  bus.setAsMasterInit()
  bus.setAsSlaveInit()

  io.masterDone := DontCare
  io.slaveSeenAddr := DontCare
  io.slaveSeenData := DontCare
  io.slaveSeenStrb := DontCare

  object Init extends HwProcess("Init") {
    private val master = createThread("Master")
    private val addrListener = createThread("AddrListener")
    private val dataListener = createThread("DataListener")
    private val respSender = createThread("RespSender")
    private val daemon = createLogic("Daemon")

    private val addrReg = RegInit(0.U(XLEN.W))
    private val dataReg = RegInit(0.U(XLEN.W))
    private val strbReg = RegInit(0.U((XLEN / 8).W))
    private val masterDoneReg = RegInit(false.B)
    private val slaveAddrReg = RegInit(0.U(XLEN.W))
    private val slaveIdReg = RegInit(0.U(AXI_ID_WIDTH.W))
    private val slaveDataReg = RegInit(0.U(XLEN.W))
    private val slaveStrbReg = RegInit(0.U((XLEN / 8).W))
    private val addrDoneReg = RegInit(false.B)
    private val dataDoneReg = RegInit(false.B)
    private val respDoneReg = RegInit(false.B)

    override def entry(): Unit = {
      master.entry {
        SysCall.Inline(AXI4Api.axi_write_once(bus, 0.U, addrReg, 2.U, dataReg, strbReg))
        master.Prev.edge.add {
          masterDoneReg := true.B
        }
        SysCall.Return()
      }

      addrListener.entry {
        val reqA = SysCall.Inline(AXI4SlaveApi.axi_listen_write_addr_once(bus))
        addrListener.Prev.edge.add {
          slaveAddrReg := reqA.addr
          slaveIdReg := reqA.id
          addrDoneReg := true.B
        }
        SysCall.Return()
      }

      dataListener.entry {
        val reqW = SysCall.Inline(AXI4SlaveApi.axi_listen_write_data_once(bus))
        dataListener.Prev.edge.add {
          slaveDataReg := reqW.data
          slaveStrbReg := reqW.strb
          dataDoneReg := true.B
        }
        SysCall.Return()
      }

      respSender.entry {
        respSender.Step("WaitReq") {
          respSender.waitCondition(addrDoneReg && dataDoneReg)
        }
        SysCall.Inline(AXI4SlaveApi.axi_send_write_resp_once(bus, slaveIdReg))
        respSender.Prev.edge.add {
          respDoneReg := true.B
        }
        SysCall.Return()
      }

      daemon.run {
        when(io.start && !master.active && !addrListener.active && !dataListener.active && !respSender.active) {
          addrReg := io.addr
          dataReg := io.dataIn
          strbReg := io.strb
          masterDoneReg := false.B
          addrDoneReg := false.B
          dataDoneReg := false.B
          respDoneReg := false.B
          SysCall.Inline(SysCall.start(addrListener))
          SysCall.Inline(SysCall.start(dataListener))
          SysCall.Inline(SysCall.start(respSender))
          SysCall.Inline(SysCall.start(master))
        }

        io.masterDone := masterDoneReg
        io.slaveSeenAddr := slaveAddrReg
        io.slaveSeenData := slaveDataReg
        io.slaveSeenStrb := slaveStrbReg
      }
    }
  }

  Init.build()
}

class AXI4SlaveApiSpec extends AnyFlatSpec {
  "axi_listen_read_addr_once + axi_send_read_data_once" should "let a slave thread receive a read request and respond with data" in {
    simulate(new AxiSlaveReadHarness) { c =>
      c.reset.poke(true.B)
      c.io.start.poke(false.B)
      c.io.addr.poke(0.U)
      c.clock.step()

      c.reset.poke(false.B)
      c.io.addr.poke(12.U)
      c.io.start.poke(true.B)
      c.clock.step()
      c.io.start.poke(false.B)

      var cycles = 0
      while (c.io.masterDone.peek().litValue == 0 && cycles < 20) {
        c.clock.step()
        cycles += 1
      }

      c.io.masterDone.expect(true.B)
      c.clock.step(2)
      c.io.masterData.expect("h55667788".U)
      c.io.slaveSeenAddr.expect(12.U)
      c.io.slaveSeenSize.expect(2.U)
    }
  }

  "axi_listen_write_addr_once + axi_listen_write_data_once + axi_send_write_resp_once" should "let slave-side listener threads receive a write request and acknowledge it" in {
    simulate(new AxiSlaveWriteHarness) { c =>
      c.reset.poke(true.B)
      c.io.start.poke(false.B)
      c.io.addr.poke(0.U)
      c.io.dataIn.poke(0.U)
      c.io.strb.poke(0.U)
      c.clock.step()

      c.reset.poke(false.B)
      c.io.addr.poke(8.U)
      c.io.dataIn.poke("hAABBCCDD".U)
      c.io.strb.poke("b1111".U)
      c.io.start.poke(true.B)
      c.clock.step()
      c.io.start.poke(false.B)

      var cycles = 0
      while (c.io.masterDone.peek().litValue == 0 && cycles < 20) {
        c.clock.step()
        cycles += 1
      }

      c.io.masterDone.expect(true.B)
      c.clock.step(2)
      c.io.slaveSeenAddr.expect(8.U)
      c.io.slaveSeenData.expect("hAABBCCDD".U)
      c.io.slaveSeenStrb.expect("b1111".U)
    }
  }
}
