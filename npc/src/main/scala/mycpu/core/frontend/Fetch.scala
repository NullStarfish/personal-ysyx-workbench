package mycpu.core.frontend

import chisel3._
import chisel3.util._
import mycpu.common._
import mycpu.core.bundles._
import mycpu.memory.AXI4LiteMasterIO

class Fetch extends Module {
  val io = IO(new Bundle {
    val axi      = new AXI4LiteMasterIO()
    
    // [修改] 移除 redirect，改为接收后端确定的 Next PC
    val next_pc       = Input(UInt(XLEN.W))
    val pc_update_en  = Input(Bool()) // 来自 WB 阶段的握手信号

    val out      = Decoupled(new FetchPacket()) 
  })

  // 状态机：等待启动 -> 发起读请求 -> 等待数据 -> 发送数据 -> 等待后端完成
  object State extends ChiselEnum {
    val sIdle, sReadReq, sReadData, sOutput, sWaitBackend = Value
  }
  val state = RegInit(State.sIdle)

  val pc = RegInit(START_ADDR.U(XLEN.W))
  
  // [关键] 启动逻辑：
  // 1. 复位后，自动启动第一次 (first_fetch)
  // 2. 之后，必须等待 io.pc_update_en
  val first_fetch = RegInit(true.B)

  // 更新 PC 的逻辑
  when (io.pc_update_en) {
    pc := io.next_pc
  }

  // AXI 默认值
  io.axi.ar.valid := false.B
  io.axi.ar.bits.addr := pc
  io.axi.ar.bits.prot := 0.U
  
  io.axi.r.ready := false.B

  io.axi.aw.valid := false.B; io.axi.aw.bits := DontCare
  io.axi.w.valid  := false.B; io.axi.w.bits  := DontCare
  io.axi.b.ready  := false.B

  // Output 默认值
  io.out.valid := false.B
  io.out.bits  := DontCare
  
  // 保存取到的指令
  val inst_reg = Reg(UInt(32.W))

  switch (state) {
    is (State.sIdle) {
      // 如果是刚复位，或者后端通知更新了 PC，则开始取指
      when (first_fetch || io.pc_update_en) {
        state := State.sReadReq
        first_fetch := false.B // 消耗掉首次启动标志
      }
    }

    is (State.sReadReq) {
      io.axi.ar.valid := true.B
      when (io.axi.ar.fire) {
        state := State.sReadData
      }
    }

    is (State.sReadData) {
      io.axi.r.ready := true.B
      when (io.axi.r.fire) {
        inst_reg := io.axi.r.bits.data
        state := State.sOutput
      }
    }

    is (State.sOutput) {
      io.out.valid := true.B
      io.out.bits.inst := inst_reg
      io.out.bits.pc   := pc
      // 这里 dnpc 暂时设为 pc+4 传给后端，但后端(Execute)会重算真正的 dnpc
      io.out.bits.dnpc := pc + 4.U 
      io.out.bits.isException := false.B

      when (io.out.fire) {
        // 输出成功后，进入等待状态，直到 WB 阶段完成并更新 PC
        state := State.sWaitBackend 
        printf("Fetch: Sent PC=%x, Inst=%x. Waiting for WB...\n", pc, inst_reg)
      }
    }

    is (State.sWaitBackend) {
      // 这里的逻辑其实隐含在 sIdle 中了。
      // 当 io.pc_update_en 到来时，在 Always 块开头会更新 PC。
      // 我们只需要跳转回 sReadReq 即可（或者回到 sIdle 等待，逻辑是一样的）
      when (io.pc_update_en) {
        state := State.sReadReq
      }
    }
  }
}