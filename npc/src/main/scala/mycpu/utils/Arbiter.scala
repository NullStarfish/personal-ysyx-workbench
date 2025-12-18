package mycpu.utils

import chisel3._
import chisel3.util._
import mycpu.common._
import mycpu.utils._

class SimpleAXIArbiter extends Module {
  val io = IO(new Bundle {
    val left  = Flipped(new AXI4Bundle(AXI_ID_WIDTH, XLEN, XLEN))
    val right = Flipped(new AXI4Bundle(AXI_ID_WIDTH, XLEN, XLEN))
    val out   = new AXI4Bundle(AXI_ID_WIDTH, XLEN, XLEN)
  })

  object Owner extends ChiselEnum { val None, Left, Right = Value }
  val state = RegInit(Owner.None)



  // 默认断开所有 Ready，防止误握手
  io.left.setAsSlaveInit()  // 辅助函数: ready=false, valid=false
  io.right.setAsSlaveInit()
  io.out.setAsMasterInit()  // 辅助函数: valid=false, ready=false

  // 辅助函数：手动置 Slave 输出默认值 (如果 AXI4Defs 中没有 setAsSlaveInit，请使用下面的代码)
  def setSlaveDefault(p: AXI4Bundle): Unit = {
    p.ar.ready := false.B
    p.aw.ready := false.B
    p.w.ready  := false.B
    p.r.valid  := false.B; p.r.bits := DontCare
    p.b.valid  := false.B; p.b.bits := DontCare
  }
  // def setMasterDefault ... (同理，Output valid=false)

  // 重新覆盖默认值逻辑，确保安全
  setSlaveDefault(io.left)
  setSlaveDefault(io.right)
  
  // Output 默认不发请求
  io.out.ar.valid := false.B; io.out.ar.bits := DontCare
  io.out.aw.valid := false.B; io.out.aw.bits := DontCare
  io.out.w.valid  := false.B; io.out.w.bits  := DontCare
  io.out.r.ready  := false.B
  io.out.b.ready  := false.B

  // --- 仲裁逻辑 ---
  val leftReq  = io.left.ar.valid || io.left.aw.valid
  val rightReq = io.right.ar.valid || io.right.aw.valid

  // 简单的事务结束判定
  val writeDone = io.out.b.fire
  val readDone  = io.out.r.fire // 注意：AXI4Last 处理，这里简化为 Lite 单拍

  switch(state) {
    is(Owner.None) {
      when(leftReq) { state := Owner.Left }
      .elsewhen(rightReq) { state := Owner.Right }
    }
    is(Owner.Left) {
      when(readDone || writeDone) { state := Owner.None }
    }
    is(Owner.Right) {
      when(readDone || writeDone) { state := Owner.None }
    }
  }

  // --- 物理连接 ---
  // 只有当持有锁时才接通线路
  when(state === Owner.Left || (state === Owner.None && leftReq)) {
    io.out <> io.left
  } 
  .elsewhen(state === Owner.Right || (state === Owner.None && rightReq)) {
    io.out <> io.right
  }
  
  // Debug
  when(leftReq && state === Owner.None) { Debug.log("[Arbiter] Grant Left\n") }
  when(rightReq && !leftReq && state === Owner.None) { Debug.log("[Arbiter] Grant Right\n") }
}