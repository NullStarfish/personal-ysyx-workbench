package mycpu.core.processes

import chisel3._
import chisel3.util._
import mycpu.common._
import mycpu.core.os._
import mycpu.core.kernel._
import mycpu.core.bundles._
import mycpu.core.components._
import chisel3.util.experimental.BoringUtils

// ==============================================================================
// 取指进程
// ==============================================================================
class FetchProcess(p: Option[ProcessContext], k: Kernel) extends HwProcess("Fetch")(p, k) {
  override def entry(): Unit = {
    val t = createThread("F_Thread")
    
    val pcDev    = sys_open("PC")
    val axi      = sys_open("AXI_BUS")
    val toMain   = sys_open("Fetch2Main") 
    val tokenIn  = sys_open("TokenPass")

    t.entry {
      t.Loop() 
      
      val currentPC = Reg(UInt(32.W))
      val inst      = Reg(UInt(32.W))

      t.Step("ReadPC") {
        val res = pcDev.sys_read(0.U)
        currentPC := res.value
      }
      
      t.Step("FetchIMEM") {
        val res = axi.sys_read(currentPC, 2.U)
        t.waitCondition(res.errno === Errno.ESUCCESS)
        inst := res.value
      }
      
      t.Step("Dispatch") {
        val pkt = Wire(new FetchPacket)
        pkt.inst := inst
        pkt.pc   := currentPC
        
        val res = toMain.sys_write(0.U, pkt.asUInt)
        t.waitCondition(res.errno === Errno.ESUCCESS)
      }

      t.Step("WaitToken") {
        val res = tokenIn.sys_read(0.U)
        t.waitCondition(res.errno === Errno.ESUCCESS)
      }
    }
  }
}

// ==============================================================================
// 主控进程
// ==============================================================================
class MainProcess(p: Option[ProcessContext], k: Kernel) extends HwProcess("Main")(p, k) {
  override def entry(): Unit = {
    val t = createThread("M_Thread")
    
    val rf      = sys_open("RF")
    val pcDev   = sys_open("PC")
    val dmem    = sys_open("AXI_BUS")
    val fromFet = sys_open("Fetch2Main")
    val tokenOut= sys_open("TokenPass")
    
    val decoder = Module(new ControlUnit)
    val immGen  = Module(new ImmGen)
    val alu     = Module(new ALU)

    val pkt     = RegInit(0.U.asTypeOf(new FetchPacket))
    val ctrl    = Reg(new CtrlSignals)
    val rs1Val  = Reg(UInt(32.W))
    val rs2Val  = Reg(UInt(32.W))
    val immVal  = Reg(UInt(32.W))
    val aluOut  = Reg(UInt(32.W))
    val memVal  = Reg(UInt(32.W))

    // === [修复] 默认驱动赋值，防止 "not fully initialized" 错误 ===
    // 即使 Thread 没运行，这些 Sink 也会被驱动
    decoder.io.inst := pkt.inst
    immGen.io.inst  := pkt.inst
    immGen.io.sel   := ctrl.immType
    alu.io.a        := 0.U
    alu.io.b        := 0.U
    alu.io.op       := ctrl.aluOp
    
    val dpi_valid = WireInit(false.B); BoringUtils.addSource(dpi_valid, "DPI_Commit_Valid")
    val dpi_pc    = WireInit(0.U(32.W)); BoringUtils.addSource(dpi_pc, "DPI_Commit_PC")
    val dpi_inst  = WireInit(0.U(32.W)); BoringUtils.addSource(dpi_inst, "DPI_Commit_Inst")

    t.entry {
      t.Loop()

      t.Step("FetchPkt") {
        val res = fromFet.sys_read(0.U)
        t.waitCondition(res.errno === Errno.ESUCCESS)
        when(res.errno === Errno.ESUCCESS) {
          val rawPkt = res.value.asTypeOf(new FetchPacket)
          pkt := rawPkt
          
          // 立即更新译码和立即数（此时 PC 处于 FetchPkt 步）
          decoder.io.inst := rawPkt.inst
          ctrl := decoder.io.ctrl
          
          immGen.io.inst := rawPkt.inst
          immGen.io.sel  := decoder.io.ctrl.immType
          immVal := immGen.io.out
        }
      }

      t.Step("ReadRS1") {
        val rs1 = rf.sys_read(pkt.inst(19, 15))
        rs1Val := rs1.value
      }

      t.Step("ReadRS2") {
        val rs2 = rf.sys_read(pkt.inst(24, 20))
        rs2Val := rs2.value
      }

      t.Step("Execute") {
        val src1 = WireDefault(0.U(32.W))
        val src2 = WireDefault(0.U(32.W))

        switch(ctrl.arg1) {
          is(Arg1Type.REG) { src1 := rs1Val }
          is(Arg1Type.PC)  { src1 := pkt.pc }
          is(Arg1Type.ZERO){ src1 := 0.U }
        }
        
        switch(ctrl.arg2) {
          is(Arg2Type.REG)    { src2 := rs2Val }
          is(Arg2Type.IMM)    { src2 := immVal }
          is(Arg2Type.CONST_4){ src2 := 4.U }
        }

        alu.io.a  := src1
        alu.io.b  := src2
        aluOut    := alu.io.out
      }

      t.Step("Mem") {
        when(ctrl.service === ServiceType.MEM_RD) {
           printf("[DEBUG] Main MEM_RD Start: addr=0x%x\n", aluOut)
           val res = dmem.sys_read(aluOut, ctrl.memSize)
           t.waitCondition(res.errno === Errno.ESUCCESS)
           memVal := res.value
        } 
        .elsewhen(ctrl.service === ServiceType.MEM_WR) {
           printf("[DEBUG] Main MEM_WR Start: addr=0x%x\n", aluOut)
           val res = dmem.sys_write(aluOut, rs2Val, ctrl.memSize)
           t.waitCondition(res.errno === Errno.ESUCCESS)
        }
      }

      t.Step("Commit") {
        // --- Load 数据对齐处理 (针对 LBU 等) ---
        val addrOffset = aluOut(1, 0)
        val shiftAmount = Cat(addrOffset, 0.U(3.W)) // offset * 8
        val shiftedData = memVal >> shiftAmount
        
        val memFormatted = MuxLookup(ctrl.memSize, shiftedData)(Seq(
          0.U -> Mux(ctrl.memSigned, shiftedData(7, 0).asSInt.asUInt, shiftedData(7, 0)),
          1.U -> Mux(ctrl.memSigned, shiftedData(15, 0).asSInt.asUInt, shiftedData(15, 0)),
          2.U -> shiftedData(31, 0)
        ))

        val finalData = WireDefault(aluOut)
        when(ctrl.service === ServiceType.MEM_RD) { finalData := memFormatted }
        .elsewhen(ctrl.service === ServiceType.JUMP) { finalData := pkt.pc + 4.U }

        val rfDone = WireDefault(true.B)
        when(ctrl.regWen) {
          val res = rf.sys_write(pkt.inst(11, 7), finalData)
          t.waitCondition(res.errno === Errno.ESUCCESS)
          rfDone := (res.errno === Errno.ESUCCESS)
        }

        val nextPC = WireDefault(pkt.pc + 4.U)
        when(ctrl.service === ServiceType.BRANCH) {
           val eq = (aluOut === 0.U)
           val lt = (aluOut =/= 0.U)
           val funct3 = pkt.inst(14, 12)
           val branchTaken = MuxLookup(funct3, false.B)(Seq(
             "b000".U -> eq,     // BEQ
             "b001".U -> !eq,    // BNE
             "b100".U -> lt,     // BLT
             "b101".U -> !lt,    // BGE
             "b110".U -> lt,     // BLTU
             "b111".U -> !lt     // BGEU
           ))
           when(branchTaken) { nextPC := pkt.pc + immVal }
        } .elsewhen(ctrl.service === ServiceType.JUMP) {
           nextPC := aluOut
        }

        when (rfDone) {
          val res = pcDev.sys_write(0.U, nextPC)
          t.waitCondition(res.errno === Errno.ESUCCESS)
          
          when(res.errno === Errno.ESUCCESS) {
            dpi_valid := true.B
            dpi_pc    := pkt.pc
            dpi_inst  := pkt.inst
            
            // 发送令牌唤醒 Fetch
            val tRes = tokenOut.sys_write(0.U, 1.U)
            t.waitCondition(tRes.errno === Errno.ESUCCESS)
          }
        }
      }
    }
  }
}