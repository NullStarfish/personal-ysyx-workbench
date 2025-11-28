package mycpu.utils

import chisel3._
import chisel3.util._
import mycpu.common._
import mycpu.utils._

class SimpleAXIArbiter extends Module {
  val io = IO(new Bundle {
    val left = Flipped(new AXI4LiteBundle) // 来自 left
    val right = Flipped(new AXI4LiteBundle) // 来自 LSU
    val out = new AXI4LiteBundle        // 去往 SRAM
  })

  object Owner extends ChiselEnum {
    val None, left, Lsu = Value
  }
  val state = RegInit(Owner.None)

  // --- 请求检测 ---
  val lsuReq = io.right.ar.valid || io.right.aw.valid
  val leftReq = io.left.ar.valid




  io.out.setAsMaster();
  io.left.setAsSlave();
  io.right.setAsSlave();



  switch(state) {
    is(Owner.None) {
      when(lsuReq) {
        state := Owner.Lsu
      } .elsewhen(leftReq) {
        state := Owner.left
      }
    }
    is(Owner.Lsu) {
      val writeDone = io.out.b.fire
      val readDone  = io.out.r.fire 
      when(writeDone || readDone) {
        state := Owner.None
      }
    }
    is(Owner.left) {
      val readDone = io.out.r.fire
      when(readDone) {
        state := Owner.None
      }
    }
  }

  // 路由连接
  when(state === Owner.Lsu || (state === Owner.None && lsuReq)) {
    // LSU <-> out
    io.out.ar <> io.right.ar
    io.out.aw <> io.right.aw
    io.out.w  <> io.right.w
    io.right.r  <> io.out.r
    io.right.b  <> io.out.b
  } .elsewhen (state === Owner.left || (state === Owner.None && leftReq)) {
    // left <-> out (仅读通道)
    io.out.ar <> io.left.ar
    io.left.r  <> io.out.r
    // 注意：left 的写通道在上面已经赋了默认值 (ready=0)，所以这里不用管
  }
}