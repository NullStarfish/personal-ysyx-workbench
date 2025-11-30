package mycpu.utils

import chisel3._
import chisel3.util._
import mycpu.common._
import mycpu.utils._

class SimpleAXIArbiter extends Module {
  val io = IO(new Bundle {
    val left = Flipped(new AXI4LiteBundle) // 来自 left
    val right = Flipped(new AXI4LiteBundle) // 来自 right
    val out = new AXI4LiteBundle        // 去往 SRAM
  })

  object Owner extends ChiselEnum {
    val None, left, right = Value
  }
  val state = RegInit(Owner.None)

  // --- 请求检测 ---
  val leftReq = io.left.ar.valid   || io.left.aw.valid
  val rightReq = io.right.ar.valid || io.right.aw.valid



  when(leftReq) {
    Debug.log("[DEBUG] [Arbiter]: leftReq received\n")
  }
  when(rightReq) {
    Debug.log("[DEBUG] [Arbiter]: rightReq received\n")
  }

  

  io.out.setAsMaster();
  io.left.setAsSlave();
  io.right.setAsSlave();


  val writeDone = io.out.b.fire
  val readDone  = io.out.r.fire

  when(writeDone) {
    Debug.log("[DEBUG] [Arbiter] write Done\n")
  }

  when(readDone) {
    Debug.log("[DEBUG] [Arbiter] read Done\n")
  }

  switch(state) {
    is(Owner.None) {
      when(leftReq) {
        state := Owner.left
      } .elsewhen(rightReq) {
        state := Owner.right
      }
    }
    is(Owner.left) {
      when(readDone || writeDone) {
        state := Owner.None
      }
    }
    is(Owner.right) {

      when(writeDone || readDone) {
        state := Owner.None
      }
    }

  }



  when (state === Owner.left || (state === Owner.None && leftReq)) {
    io.out.ar <> io.left.ar
    io.out.aw <> io.left.aw
    io.out.w  <> io.left.w
    io.left.r  <> io.out.r
    io.left.b  <> io.out.b
  } .elsewhen(state === Owner.right || (state === Owner.None && rightReq)) {
    // right <-> out
    io.out.ar <> io.right.ar
    io.out.aw <> io.right.aw
    io.out.w  <> io.right.w
    io.right.r  <> io.out.r
    io.right.b  <> io.out.b
  } 


}