package mycpu.core

import HwOS.kernel._
import chisel3._
import mycpu.axi.AXI4Bundle
import mycpu.common.XLEN
import mycpu.mem.AXIMemory
import mycpu.pipeline._

class Core extends Module {
  val io = IO(new Bundle {
    val master = new AXI4Bundle(idWidth = 4, addrWidth = XLEN, dataWidth = XLEN)
  })

  val masterBus = Wire(new AXI4Bundle(idWidth = 4, addrWidth = XLEN, dataWidth = XLEN))
  masterBus.setAsMasterInit()
  io.master <> masterBus

  implicit val kernel: Kernel = new Kernel()

  object Init extends HwProcess("Init") {
    val links = new PipelineLinks

    val memory = spawn(new AXIMemory(masterBus, maxClients = 2))
    val regfile = spawn(new RegfileProcess("Regfile"))
    val tracer: TracerProcess = adopt(new TracerProcess(
      links.fetch,
      links.regfileProbe,
      links.csrProbe,
      clock,
      reset.asBool,
      localName = "Tracer",
    ))
    val fetch: FetchProcess = adopt(new FetchProcess(memory, links.decode, links.trace, "Fetch"))
    val lsu: LsuProcess = adopt(new LsuProcess(links.memory, links.writeback, links.trace, "Lsu"))
    val writeback = spawn(new WritebackProcess(fetch.api, regfile.api, tracer.api, "Writeback"))
    val execute: ExecuteProcess = adopt(new ExecuteProcess(links.lsu, links.writeback, links.hazard, links.trace, "Execute"))
    val hazard = spawn(new ControlHazardProcess(fetch.api, tracer.api, Seq(() => execute.clearExecuteReqBuffer()), "ControlHazard"))
    val decode: DecodeProcess = adopt(new DecodeProcess(links.execute, links.regfile, "Decode"))

    override def entry(): Unit = {}
  }

  Init.links.fetch.bind(Init.fetch.api)
  Init.links.trace.bind(Init.tracer.api)
  Init.links.decode.bind(Init.decode.api)
  Init.links.execute.bind(Init.execute.api)
  Init.links.regfile.bind(Init.regfile.api)
  Init.links.regfileProbe.bind(Init.regfile.probeApi)
  Init.links.csrProbe.bind(Init.execute.csrProbeApi)
  Init.links.memory.bind(Init.memory.api(1))
  Init.links.lsu.bind(Init.lsu.api)
  Init.links.writeback.bind(Init.writeback.api)
  Init.links.hazard.bind(Init.hazard.api)

  Init.tracer.build()
  Init.lsu.build()
  Init.execute.build()
  Init.decode.build()
  Init.fetch.build()
  Init.build()
}
