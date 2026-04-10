package mycpu.core.backend

import chisel3._
import chisel3.util._
import mycpu.common._
import mycpu.core.bundles._
import mycpu.utils._

class LSU extends Module {
  val io = IO(new Bundle {
    val in  = Flipped(Decoupled(new ExecutePacket))
    val out = Decoupled(new MemoryPacket)
    val axi = new AXI4LiteBundle(XLEN, XLEN) // 统一接口
    val pendingLoad = Output(Bool())
    val pendingRd = Output(UInt(5.W))
  })

//=======================================================
//1. 处理输入，输出的数据
//=======================================================

  val reqReg = Reg(new ExecutePacket)
















  val readBridge  = Module(new AXI4ReadBridge(XLEN, XLEN))
  val writeBridge = Module(new AXI4WriteBridge(XLEN, XLEN))


  val AXI4Split(rBus, wBus) = io.axi
  readBridge.io.axi  <> rBus
  writeBridge.io.axi <> wBus


  

  //我们同时拥有：
  //非对齐地址，wstrb，size，三种寻址方式，没有问题


  val inAddrOffset = io.in.bits.result(1, 0)
  val inWstrb = WireDefault(0.U(4.W))
  val inWdata = WireDefault(0.U(XLEN.W))
  val size    = WireDefault(0.U(3.W))
  switch(io.in.bits.mem.subop) {
    is(ExecSubop.Byte) { inWstrb := "b0001".U << inAddrOffset; inWdata := io.in.bits.rhs(7,0) << (inAddrOffset << 3);  size := 0.U }
    is(ExecSubop.Half) { inWstrb := "b0011".U << inAddrOffset; inWdata := io.in.bits.rhs(15,0) << (inAddrOffset << 3); size := 1.U }
    is(ExecSubop.Word) { inWstrb := "b1111".U;                  inWdata := io.in.bits.rhs;                              size := 2.U }
  }








  val memReq = WireDefault(new AXI4BundleA(AXI_ID_WIDTH, XLEN), {
    val w = Wire(new AXI4BundleA(AXI_ID_WIDTH, XLEN))
    // 这里设置通用的“安全”默认值
    w.id    := 1.U
    w.addr  := io.in.bits.result
    //w.addr  := Cat(io.in.bits.aluResult(XLEN -1, 2), "b00".U(2.W)) // 地址对齐
    w.len   := 0.U
    w.size  := size
    w.burst := 0.U
    w.lock  := false.B
    w.cache := 0.U
    w.prot  := 0.U
    w.qos   := 0.U
    w // 返回这个 wire
  })

  
  val storePack = WireDefault(new AXI4BundleW(XLEN), {
    val w = Wire(new AXI4BundleW(XLEN))
    w.data := inWdata
    w.strb := inWstrb
    w.last := 1.B
    w
  })


  val loadPack = Wire(new AXI4BundleR(AXI_ID_WIDTH, XLEN))

  val bPack = Wire(new AXI4BundleB(AXI_ID_WIDTH))


  readBridge.io.rReq.bits        := memReq
  loadPack                       := readBridge.io.rStream.bits
  //readBridge.io.req.bits.addr   := Cat(io.in.bits.aluResult(XLEN - 1, 2), "b00".U(2.W))

  //writeBridge.io.req.bits.addr  := Cat(io.in.bits.aluResult(XLEN -1, 2), "b00".U(2.W))
  writeBridge.io.wReq.bits       := memReq
  writeBridge.io.wStream.bits    := storePack
  bPack                          := writeBridge.io.bResp.bits



  //Bridge接口
  val isMemRead  = io.in.bits.mem.valid && !io.in.bits.mem.write
  val isMemWrite = io.in.bits.mem.valid && io.in.bits.mem.write
  val isNonMem   = !io.in.bits.mem.valid


  val rawReadData = loadPack.data
  val finalLoadData = Wire(UInt(32.W))
  val shiftedData = rawReadData >> (reqReg.result(1, 0) << 3)
  finalLoadData := 0.U




  switch(reqReg.mem.subop) {
    is(ExecSubop.Byte) {
      finalLoadData := Mux(reqReg.mem.unsigned, shiftedData(7, 0), Cat(Fill(24, shiftedData(7)), shiftedData(7, 0)))
    }
    is(ExecSubop.Half) {
      finalLoadData := Mux(reqReg.mem.unsigned, shiftedData(15, 0), Cat(Fill(16, shiftedData(15)), shiftedData(15, 0)))
    }
    is(ExecSubop.Word) { finalLoadData := rawReadData }
  }

  io.out.bits.wb.rd := reqReg.wb.rd
  io.out.bits.wb.regWen := reqReg.wb.regWen
  io.out.bits.wbData := Mux(reqReg.mem.valid && !reqReg.mem.write, finalLoadData, reqReg.result)




  // 输出处理 (读取数据对齐等)


  //io.in.ready
  io.in.ready  := false.B
  io.out.valid := false.B
  readBridge.io.rReq.valid       := false.B
  writeBridge.io.wReq.valid      := false.B
  readBridge.io.rStream.ready    := false.B
  writeBridge.io.wStream.valid   := false.B
  writeBridge.io.bResp.ready     := false.B

  object State extends ChiselEnum { val sIdle, sWaitResp = Value }
  val state = RegInit(State.sIdle)

  io.pendingLoad := state === State.sWaitResp && reqReg.mem.valid && !reqReg.mem.write
  io.pendingRd := reqReg.wb.rd

  io.in.ready := (state === State.sIdle) && (
    (isNonMem) || 
    (isMemRead && readBridge.io.rReq.ready) || 
    (isMemWrite && writeBridge.io.wReq.ready)
  )

  // read和write的Req valid
  // read 和write 的Stream ready


  switch(state) {
    is(State.sIdle) {
      when (io.in.valid) {
        when(io.in.ready) {
           reqReg := io.in.bits
           state  := State.sWaitResp
        }
        when (isMemRead) { readBridge.io.rReq.valid := true.B }
        .elsewhen (isMemWrite) { writeBridge.io.wReq.valid := true.B; writeBridge.io.wStream.valid := true.B}
      }
    }
    is(State.sWaitResp) {
      when (io.out.fire) { state := State.sIdle }
    }
  }

  when (state === State.sWaitResp) {
    when (reqReg.mem.valid && !reqReg.mem.write) {
      io.out.valid := readBridge.io.rStream.valid
      readBridge.io.rStream.ready := io.out.ready
    } .elsewhen (reqReg.mem.valid && reqReg.mem.write) {
      io.out.valid := writeBridge.io.bResp.valid
      writeBridge.io.bResp.ready := io.out.ready
    } .otherwise {
      io.out.valid := true.B
    }
  }



  // 握手逻辑


  when(readBridge.io.rReq.fire) {
    Debug.log("[DEBUG] [LSU]  read req: addr: %x, size: %x\n", memReq.addr, memReq.size)
  }

  when(readBridge.io.rStream.fire) {
    Debug.log("[DEBUG] [LSU]  read stream: rdata: %x, resp: %x\n", loadPack.data, loadPack.resp)
  }
  when(writeBridge.io.wReq.fire) {
    Debug.log("[DEBUG] [LSU]  write req: addr: %x, size: %x\n", memReq.addr, memReq.size)
  }
  when(writeBridge.io.wStream.fire) {
    Debug.log("[DEBUG] [LSU]: write stream: data: %x, wstrb: %x\n", storePack.data, storePack.strb)
  }








}
