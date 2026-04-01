package mycpu.core.frontend

import HwOS.kernel.process.HwProcess
import HwOS.kernel.system.{Kernel, SysCall}
import chisel3._
import chisel3.util._
import mycpu.axi._
import mycpu.common._
import mycpu.compatibility.DecoupledApi
import mycpu.core.bundles._
import mycpu.mem.Memory

class Fetch extends Module {
  val io = IO(new Bundle {
    val axi          = new AXI4LiteBundle(XLEN, XLEN)
    val next_pc      = Input(UInt(XLEN.W))
    val pc_update_en = Input(Bool())
    val out          = Decoupled(new FetchPacket())
  })

  io.axi.setAsMasterInit()
  io.out.valid := false.B
  io.out.bits := DontCare

  val pc = RegInit(START_ADDR.U(XLEN.W))
  val fetchIssued = RegInit(false.B)

  implicit val kernel: Kernel = new Kernel()

  object Init extends HwProcess("Init") {
    private val memory = spawn(new Memory(io.axi, maxClients = 1))
    private val fetchWorker = createThread("Fetch")
    private val daemon = createLogic("Daemon")
    private val fetchAddrReg = RegInit(0.U(XLEN.W))
    private val fetchedInstReg = RegInit(0.U(32.W))

    override def entry(): Unit = {
      fetchWorker.entry {
        val api = SysCall.Inline(memory.RequestMemoryApi(0))
        val fetchedInst = SysCall.Inline(api.read_once(fetchAddrReg, 2.U))
        fetchWorker.Prev.edge.add {
          fetchedInstReg := fetchedInst
        }

        fetchWorker.Step("PushInst") {
          val payload = Wire(new FetchPacket)
          payload.inst := fetchedInstReg
          payload.pc := fetchAddrReg
          payload.dnpc := fetchAddrReg + 4.U
          payload.isException := false.B
          SysCall.Inline(DecoupledApi.push(io.out, payload))
          fetchIssued := true.B
          printf("[FETCH] push pc : %x, inst: %x\n", fetchAddrReg, fetchedInstReg)
        }
        SysCall.Return()
      }

      daemon.run {
        when(io.pc_update_en) {
          pc := io.next_pc
          fetchIssued := false.B
        }.elsewhen(!fetchWorker.active && !fetchIssued) {
          fetchAddrReg := pc
          SysCall.Inline(SysCall.start(fetchWorker))
        }
      }
    }
  }

  Init.build()
}
