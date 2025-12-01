package mycpu.peripherals
import chisel3._
import chisel3.util._
import mycpu._
import mycpu.common._
import mycpu.peripherals._
import mycpu.utils._
import mycpu.MemMap.getIndex

class CLINT extends Peripheral(MemMap.devices(getIndex("CLINT"))) {
    io.bus.setAsSlave()
    val AXI4Split(rBus, wBus) = io.bus
    val readBridge  = Module(new AXI4LiteReadSlaveBridge(XLEN, XLEN))
    val writeBridge = Module(new AXI4LiteWriteSlaveBridge(XLEN, XLEN))
    readBridge.io.axi <> rBus
    writeBridge.io.axi <> wBus



    writeBridge.io.req.ready:= true.B
    val wValid = RegNext(writeBridge.io.req.valid, init = false.B)

    writeBridge.io.resp.valid := wValid
    writeBridge.io.resp.bits.isError := false.B




    readBridge.io.req.ready := true.B


    val rValid = RegNext(readBridge.io.req.valid, false.B)
    readBridge.io.resp.valid := rValid
    readBridge.io.resp.bits.isError := false.B



    val reqAddrOffset = getReadOffset



    val mtime = RegInit(UInt((2*XLEN).W))
    val mtimeVec = mtime.asTypeOf(Vec(2, UInt(XLEN.W)))
    mtime := mtime + 1.U

    readBridge.io.resp.bits.rdata := mtimeVec(reqAddrOffset(localAddrWidth))//只有一位决定



}