package mycpu.memory

import chisel3._
import chisel3.util._

class SimpleAXIArbiter extends Module {
  val io = IO(new Bundle {
    val ifu = Flipped(new AXI4LiteMasterIO) // 来自 IFU
    val lsu = Flipped(new AXI4LiteMasterIO) // 来自 LSU
    val mem = new AXI4LiteMasterIO        // 去往 SRAM
  })

  object Owner extends ChiselEnum {
    val None, Ifu, Lsu = Value
  }
  val state = RegInit(Owner.None)

  // --- 请求检测 ---
  val lsuReq = io.lsu.ar.valid || io.lsu.aw.valid
  val ifuReq = io.ifu.ar.valid

  // --- 默认输出初始化 (防止 latch 或悬空) ---
  
  // 1. MEM (Master) 侧默认值
  io.mem.ar.valid := false.B
  io.mem.ar.bits  := DontCare // bits 在 valid=0 时可以不关心，但最好赋 0
  io.mem.aw.valid := false.B
  io.mem.aw.bits  := DontCare
  io.mem.w.valid  := false.B
  io.mem.w.bits   := DontCare
  io.mem.r.ready  := false.B
  io.mem.b.ready  := false.B

  // 2. IFU (Slave) 侧默认值
  io.ifu.ar.ready := false.B
  io.ifu.r.valid  := false.B
  io.ifu.r.bits   := DontCare
  
  // [修复] IFU 虽然只读，但 Arbiter 必须驱动写通道的反馈信号
  io.ifu.aw.ready    := false.B
  io.ifu.w.ready     := false.B
  io.ifu.b.valid     := false.B
  io.ifu.b.bits.resp := 0.U

  // 3. LSU (Slave) 侧默认值
  io.lsu.ar.ready := false.B
  io.lsu.aw.ready := false.B
  io.lsu.w.ready  := false.B
  io.lsu.r.valid  := false.B
  io.lsu.r.bits   := DontCare
  io.lsu.b.valid  := false.B
  io.lsu.b.bits   := DontCare

  // --- 状态机与路由逻辑 ---

  switch(state) {
    is(Owner.None) {
      when(lsuReq) {
        state := Owner.Lsu
      } .elsewhen(ifuReq) {
        state := Owner.Ifu
      }
    }
    is(Owner.Lsu) {
      val writeDone = io.mem.b.fire
      val readDone  = io.mem.r.fire 
      when(writeDone || readDone) {
        state := Owner.None
      }
    }
    is(Owner.Ifu) {
      val readDone = io.mem.r.fire
      when(readDone) {
        state := Owner.None
      }
    }
  }

  // 路由连接
  when(state === Owner.Lsu || (state === Owner.None && lsuReq)) {
    // LSU <-> MEM
    io.mem.ar <> io.lsu.ar
    io.mem.aw <> io.lsu.aw
    io.mem.w  <> io.lsu.w
    io.lsu.r  <> io.mem.r
    io.lsu.b  <> io.mem.b
  } .elsewhen (state === Owner.Ifu || (state === Owner.None && ifuReq)) {
    // IFU <-> MEM (仅读通道)
    io.mem.ar <> io.ifu.ar
    io.ifu.r  <> io.mem.r
    // 注意：IFU 的写通道在上面已经赋了默认值 (ready=0)，所以这里不用管
  }
}