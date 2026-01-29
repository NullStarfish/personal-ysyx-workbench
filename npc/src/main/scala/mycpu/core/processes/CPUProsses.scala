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
    
    val pcDev   = sys_open("PC")
    val pcCheck = sys_open("PC") 
    val imem    = sys_open("AXI_BUS") 
    
    // [新增] 接收来自 Main 的跳转意图信号
    val mainJumpFlag = WireInit(false.B)
    BoringUtils.addSink(mainJumpFlag, "Main_Jump_Active")

    t.entry {
      t.Loop() 
      
      val currentPC = Reg(UInt(32.W))
      val inst      = Reg(UInt(32.W))
      
      t.Step("ReadPC") {
        val res = pcDev.sys_read(0.U)
        currentPC := res.value
      }
      
      t.Step("FetchIMEM") {
        val res = imem.sys_read(currentPC)
        t.waitCondition(res.isError === 0.U)
        inst := res.value
      }
      
      t.Step("DispatchQueue") {
        val pkt = Wire(new FetchPacket)
        pkt.inst := inst
        pkt.pc   := currentPC 
        
        // [关键修复] 死锁破除机制
        // 如果 Main 正在发起跳转 (mainJumpFlag=true)，说明当前 inst 是错误路径指令。
        // 此时无论队列是否已满，都直接丢弃该指令，不要尝试写入。
        // 这防止了 Fetch 阻塞在队列写操作上，从而允许 Fetch 快速流转到 ReadPC 读取新地址。
        when (!mainJumpFlag) {
            val res = stdout.sys_write(0.U, pkt.asUInt)
            t.waitCondition(res.isError === 0.U)
        }
      }

      t.Step("UpdatePC") {
        val realPC = pcCheck.sys_read(0.U)
        
        // 冲突检测逻辑：
        // 1. realPC 未变 (物理检测)
        // 2. Main 此时没有正在发起跳转 (意图检测)
        val safeToUpdate = (realPC.value === currentPC) && !mainJumpFlag

        when (safeToUpdate) {
           val res = pcDev.sys_write(0.U, currentPC + 4.U)
           // 必须等待写完成
           t.waitCondition(res.isError === 0.U)
        }
        // 如果不安全，跳过更新，下一次 ReadPC 将读取 Main 更新后的新 PC
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
    
    val rf    = sys_open("RF")
    val pcDev = sys_open("PC")
    val dmem  = sys_open("AXI_BUS")
    
    val decoder = Module(new ControlUnit)
    val immGen  = Module(new ImmGen)
    val alu     = Module(new ALU)
    
    val dpi_valid = WireInit(false.B); BoringUtils.addSource(dpi_valid, "DPI_Commit_Valid")
    val dpi_pc    = WireInit(0.U(32.W)); BoringUtils.addSource(dpi_pc, "DPI_Commit_PC")
    val dpi_inst  = WireInit(0.U(32.W)); BoringUtils.addSource(dpi_inst, "DPI_Commit_Inst")
    
    // [新增] 广播跳转意图
    val jumpActive = WireInit(false.B); BoringUtils.addSource(jumpActive, "Main_Jump_Active")

    t.entry {
      decoder.io.inst := 0.U
      immGen.io.inst  := 0.U
      immGen.io.sel   := ImmType.I
      alu.io.a        := 0.U
      alu.io.b        := 0.U
      alu.io.op       := ALUOp.ADD

      t.Loop()

      val expectedPC = RegInit(START_ADDR.U(32.W)) 
      val validInst  = Reg(Bool())

      val pkt     = Reg(new FetchPacket)
      val inst    = Wire(UInt(32.W)); inst := pkt.inst
      val pc      = Wire(UInt(32.W)); pc   := pkt.pc
      
      val rs1Val  = Reg(UInt(32.W))
      val rs2Val  = Reg(UInt(32.W))
      val immVal  = Reg(UInt(32.W))
      val aluOut  = Reg(UInt(32.W))
      val ctrl    = Reg(new CtrlSignals)
      
      val memReadVal = Reg(UInt(32.W))

      t.Step("Decode") {
        val res = stdin.sys_read(0.U)
        t.waitCondition(res.isError === 0.U)
        val rawPkt = res.value.asTypeOf(new FetchPacket)
        
        when (res.isError === 0.U) {
          when (rawPkt.pc === expectedPC) {
            pkt       := rawPkt
            validInst := true.B
            
            decoder.io.inst := rawPkt.inst
            ctrl := decoder.io.ctrl
            
            immGen.io.inst := rawPkt.inst
            immGen.io.sel  := decoder.io.ctrl.immType
            immVal := immGen.io.out
            
            val rs1Addr = rawPkt.inst(19, 15)
            val rs2Addr = rawPkt.inst(24, 20)
            rs1Val := rf.sys_read(rs1Addr).value
            rs2Val := rf.sys_read(rs2Addr).value
          } .otherwise {
            validInst := false.B 
          }
        }
      }

      t.Step("Execute") {
        when(validInst) {
          val src1 = WireDefault(0.U(32.W))
          val src2 = WireDefault(0.U(32.W))

          switch(ctrl.arg1) {
            is(Arg1Type.REG) { src1 := rs1Val }
            is(Arg1Type.PC)  { src1 := pc }
            is(Arg1Type.ZERO){ src1 := 0.U }
          }
          
          switch(ctrl.arg2) {
            is(Arg2Type.REG)    { src2 := rs2Val }
            is(Arg2Type.IMM)    { src2 := immVal }
            is(Arg2Type.CONST_4){ src2 := 4.U }
          }

          alu.io.op := ctrl.aluOp
          alu.io.a  := src1
          alu.io.b  := src2
          aluOut    := alu.io.out
        }
      }

      t.Step("Mem") {
        when(validInst) {
           when(ctrl.service === ServiceType.MEM_RD) {
             val res = dmem.sys_read(aluOut, ctrl.memSize)
             t.waitCondition(res.isError === 0.U)
             memReadVal := res.value
           } 
           .elsewhen(ctrl.service === ServiceType.MEM_WR) {
             val res = dmem.sys_write(aluOut, rs2Val, ctrl.memSize)
             t.waitCondition(res.isError === 0.U)
           }
        }
      }

      t.Step("WB") {
        when(validInst) {
          val finalData = WireDefault(aluOut)
          
          when(ctrl.service === ServiceType.MEM_RD) { finalData := memReadVal }
          .elsewhen(ctrl.service === ServiceType.JUMP) { finalData := pc + 4.U }

          val rfReady = WireDefault(true.B)
          val pcReady = WireDefault(true.B)

          // 1. 写寄存器堆
          when(ctrl.regWen) {
            val rd = inst(11, 7)
            val res = rf.sys_write(rd, finalData)
            t.waitCondition(res.isError === 0.U)
            rfReady := (res.isError === 0.U)
          }

          val nextPC = WireDefault(pc + 4.U)
          val branchTaken = Wire(Bool())
          branchTaken := false.B
          
          when(ctrl.service === ServiceType.BRANCH) {
             val eq  = (aluOut === 0.U) 
             val lt  = (aluOut =/= 0.U)
             val funct3 = inst(14, 12)
             switch(funct3) {
               is("b000".U) { branchTaken := eq }
               is("b001".U) { branchTaken := !eq }
               is("b100".U) { branchTaken := lt }
               is("b101".U) { branchTaken := !lt }
               is("b110".U) { branchTaken := lt }
               is("b111".U) { branchTaken := !lt }
             }
             when (branchTaken) { nextPC := pc + immVal }
          }
          .elsewhen(ctrl.service === ServiceType.JUMP) {
             branchTaken := true.B
             nextPC := aluOut
          }
          
          // [新增] 驱动跳转意图信号
          jumpActive := branchTaken

          // 2. 写 PC
          if (true) {
              when(branchTaken) {
                 val res = pcDev.sys_write(0.U, nextPC)
                 t.waitCondition(res.isError === 0.U)
                 pcReady := (res.isError === 0.U)
                 
                 when (res.isError === 0.U) {
                    expectedPC := nextPC
                 }
              } .otherwise {
                 expectedPC := nextPC
              }

              // 3. Commit
              when (rfReady && pcReady) {
                  dpi_valid := true.B
                  dpi_pc    := pc
                  dpi_inst  := inst
              }
          }
        }
      }
    }
  }
}