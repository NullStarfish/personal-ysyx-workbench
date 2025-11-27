package mycpu.utils

import chisel3._
import chisel3.util._
import mycpu.common._
import mycpu.utils._

class SimpleAXIArbiter extends Module {
  val io = IO(new Bundle {
    val ifu = Flipped(new AXI4LiteBundle) // 来自 IFU
    val lsu = Flipped(new AXI4LiteBundle) // 来自 LSU
    val mem = new AXI4LiteBundle        // 去往 SRAM
  })

  object Owner extends ChiselEnum {
    val None, Ifu, Lsu = Value
  }
  val state = RegInit(Owner.None)

  // --- 请求检测 ---
  val lsuReq = io.lsu.ar.valid || io.lsu.aw.valid
  val ifuReq = io.ifu.ar.valid




  io.mem.setAsMaster();
  io.ifu.setAsSlave();
  io.lsu.setAsSlave();



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