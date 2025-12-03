package mycpu.peripherals

import chisel3._
import chisel3.util._
import mycpu._
import mycpu.common._
import mycpu.peripherals._
import mycpu.utils._
import mycpu.MemMap.getIndex

class CLINT extends Peripheral(MemMap.devices(getIndex("CLINT"))) {
  
  val AXI4Split(rBus, wBus) = io.bus
  
  val readBridge  = Module(new AXI4LiteReadSlaveBridge(XLEN, XLEN))
  val writeBridge = Module(new AXI4LiteWriteSlaveBridge(XLEN, XLEN))
  
  readBridge.io.axi <> rBus
  writeBridge.io.axi <> wBus

  // --- Write Logic (Stub: Always success) ---
  writeBridge.io.req.ready := true.B
  val wValid = RegNext(writeBridge.io.req.valid, init = false.B)
  writeBridge.io.resp.valid        := wValid
  writeBridge.io.resp.bits.isError := false.B

  // --- Read Logic ---
  // MTIME 寄存器逻辑
  val mtime = RegInit(0.U((2 * XLEN).W))
  mtime := mtime + 1.U
  
  // 将 64位 mtime 分割为两个 32位
  val mtimeVec = Wire(Vec(2, UInt(XLEN.W)))
  mtimeVec(0) := mtime(31, 0)
  mtimeVec(1) := mtime(63, 32)

  readBridge.io.req.ready := true.B
  val reqAddr = readBridge.io.req.bits.addr
  
  // 计算 Word 索引 (bit 2): 0x...0 -> Low(0), 0x...4 -> High(1)
  // 假设 CLINT 基地址对齐，我们只关心 offset
  val isHighWord = reqAddr(2) 
  
  val rValid = RegNext(readBridge.io.req.valid, false.B)
  val rData  = RegEnable(mtimeVec(isHighWord), readBridge.io.req.valid)

  readBridge.io.resp.valid      := rValid
  readBridge.io.resp.bits.rdata := rData
  readBridge.io.resp.bits.isError := false.B

  when(readBridge.io.req.fire) {
    Debug.log("[CLINT] Req: addr=%x (HighWord=%d)\n", reqAddr, isHighWord)
  }
}