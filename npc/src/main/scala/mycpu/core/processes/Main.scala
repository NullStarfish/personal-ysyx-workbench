package mycpu.core.os

import chisel3._
import chisel3.util._
import chisel3.util.experimental.BoringUtils
import mycpu.common._
import mycpu.common.Instructions._ // 导入指令位模式
import mycpu.core.bundles._
import mycpu.core.components.ImmGen
import mycpu.MemMap

class MainProcess extends HwProcess[FetchPacket, UInt]("Processor") {
  override def entry(): Unit = {
    // ==============================================================================
    // 0. 仿真探针 (DPI Probes)
    // ==============================================================================
    val probe_valid = RegInit(false.B)
    val probe_pc    = RegInit(0.U(32.W))
    val probe_inst  = RegInit(0.U(32.W))
    val probe_dnpc  = RegInit(0.U(32.W))
    val probe_skip  = RegInit(false.B)
    val probe_halt  = RegInit(false.B)

    probe_valid := false.B

    dontTouch(probe_valid); BoringUtils.addSource(probe_valid, "DPI_Commit_Valid")
    dontTouch(probe_pc);    BoringUtils.addSource(probe_pc,    "DPI_Commit_PC")
    dontTouch(probe_inst);  BoringUtils.addSource(probe_inst,  "DPI_Commit_Inst")
    dontTouch(probe_dnpc);  BoringUtils.addSource(probe_dnpc,  "DPI_Next_PC")
    dontTouch(probe_skip);  BoringUtils.addSource(probe_skip,  "DPI_Difftest_Skip")
    dontTouch(probe_halt);  BoringUtils.addSource(probe_halt,  "DPI_Sim_Ebreak")

    // ==============================================================================
    // 1. 资源句柄与组件
    // ==============================================================================
    val t = createThread("Main_Transaction")
    
    val rfFile  = sys_open("RF")(t)
    val exFile  = sys_open("EX")(t)
    val memFile = sys_open("DMEM")(t)
    val pcFile  = sys_open("PC")(t)
    val csrFile = sys_open("CSR")(t)
    
    val immGen = Module(new ImmGen)
    immGen.io := DontCare

    // ==============================================================================
    // 2. 输入监听 (Input Monitor Logic)
    // ==============================================================================
    val monitor = createLogic("Input_Monitor")

    val input_valid  = Wire(Bool())
    val input_packet = Wire(new FetchPacket)

    monitor.run {
      val (_1, _2) = sys_peek()
      input_valid := _1
      input_packet := _2
    }

    t.startWhen(input_valid)

    // ==============================================================================
    // 3. 主执行流 (Main Thread Entry)
    // ==============================================================================
    t.entry {
      t.Step("Transaction_Commit") {
        // [A] 获取指令
        val in    = input_packet
        val inst  = in.inst
        val curPc = pcFile.read(0.U)
        
        // [B] 字段解析 (Field Extraction)
        val opcode = inst(6, 0)
        val funct3 = inst(14, 12)
        val funct7 = inst(31, 25)
        val rs1    = inst(19, 15)
        val rs2    = inst(24, 20)
        val rd     = inst(11, 7)

        // [C] 译码逻辑 (Inline Decode)
        // --------------------------------------------------------------------------
        // 控制信号定义
        val S_ALU = ServiceType.ALU;   val S_MR = ServiceType.MEM_RD; val S_MW = ServiceType.MEM_WR
        val S_BR  = ServiceType.BRANCH; val S_CSR = ServiceType.CSR;   val S_NO = ServiceType.NONE
        
        val A1_R = Arg1Type.REG;  val A1_P = Arg1Type.PC;  val A1_Z = Arg1Type.ZERO; val A1_ZM = Arg1Type.ZIMM
        val A2_R = Arg2Type.REG;  val A2_I = Arg2Type.IMM; val A2_4 = Arg2Type.CONST_4
        
        val Y = true.B; val N = false.B

        val map = Array(
          // 算术 (I-Type)
          ADDI  -> List(S_ALU, ALUOp.ADD,  A1_R,  A2_I, ImmType.I, Y),
          SLTI  -> List(S_ALU, ALUOp.SLT,  A1_R,  A2_I, ImmType.I, Y),
          SLTIU -> List(S_ALU, ALUOp.SLTU, A1_R,  A2_I, ImmType.I, Y),
          XORI  -> List(S_ALU, ALUOp.XOR,  A1_R,  A2_I, ImmType.I, Y),
          ORI   -> List(S_ALU, ALUOp.OR,   A1_R,  A2_I, ImmType.I, Y),
          ANDI  -> List(S_ALU, ALUOp.AND,  A1_R,  A2_I, ImmType.I, Y),
          SLLI  -> List(S_ALU, ALUOp.SLL,  A1_R,  A2_I, ImmType.I, Y),
          SRLI  -> List(S_ALU, ALUOp.SRL,  A1_R,  A2_I, ImmType.I, Y),
          SRAI  -> List(S_ALU, ALUOp.SRA,  A1_R,  A2_I, ImmType.I, Y),

          // 算术 (R-Type)
          ADD   -> List(S_ALU, ALUOp.ADD,  A1_R,  A2_R, ImmType.Z, Y),
          SUB   -> List(S_ALU, ALUOp.SUB,  A1_R,  A2_R, ImmType.Z, Y),
          SLL   -> List(S_ALU, ALUOp.SLL,  A1_R,  A2_R, ImmType.Z, Y),
          SLT   -> List(S_ALU, ALUOp.SLT,  A1_R,  A2_R, ImmType.Z, Y),
          SLTU  -> List(S_ALU, ALUOp.SLTU, A1_R,  A2_R, ImmType.Z, Y),
          XOR   -> List(S_ALU, ALUOp.XOR,  A1_R,  A2_R, ImmType.Z, Y),
          SRL   -> List(S_ALU, ALUOp.SRL,  A1_R,  A2_R, ImmType.Z, Y),
          SRA   -> List(S_ALU, ALUOp.SRA,  A1_R,  A2_R, ImmType.Z, Y),
          OR    -> List(S_ALU, ALUOp.OR,   A1_R,  A2_R, ImmType.Z, Y),
          AND   -> List(S_ALU, ALUOp.AND,  A1_R,  A2_R, ImmType.Z, Y),

          // 其他 ALU 相关
          LUI   -> List(S_ALU, ALUOp.COPY_B, A1_Z, A2_I, ImmType.U, Y),
          AUIPC -> List(S_ALU, ALUOp.ADD,    A1_P, A2_I, ImmType.U, Y),

          // 访存 (地址计算使用 ADD)
          LW    -> List(S_MR,  ALUOp.ADD,  A1_R,  A2_I, ImmType.I, Y),
          SW    -> List(S_MW,  ALUOp.ADD,  A1_R,  A2_I, ImmType.S, N),
          LB    -> List(S_MR,  ALUOp.ADD,  A1_R,  A2_I, ImmType.I, Y),
          LH    -> List(S_MR,  ALUOp.ADD,  A1_R,  A2_I, ImmType.I, Y),
          LBU   -> List(S_MR,  ALUOp.ADD,  A1_R,  A2_I, ImmType.I, Y),
          LHU   -> List(S_MR,  ALUOp.ADD,  A1_R,  A2_I, ImmType.I, Y),
          SB    -> List(S_MW,  ALUOp.ADD,  A1_R,  A2_I, ImmType.S, N),
          SH    -> List(S_MW,  ALUOp.ADD,  A1_R,  A2_I, ImmType.S, N),

          // 跳转
          JAL   -> List(S_BR,  ALUOp.ADD,  A1_P,  A2_I, ImmType.J, Y),
          JALR  -> List(S_BR,  ALUOp.ADD,  A1_R,  A2_I, ImmType.I, Y),
          BEQ   -> List(S_BR,  ALUOp.SUB,  A1_R,  A2_R, ImmType.B, N),
          BNE   -> List(S_BR,  ALUOp.SUB,  A1_R,  A2_R, ImmType.B, N),
          BLT   -> List(S_BR,  ALUOp.SLT,  A1_R,  A2_R, ImmType.B, N),
          BGE   -> List(S_BR,  ALUOp.SLT,  A1_R,  A2_R, ImmType.B, N),
          BLTU  -> List(S_BR,  ALUOp.SLTU, A1_R,  A2_R, ImmType.B, N),
          BGEU  -> List(S_BR,  ALUOp.SLTU, A1_R,  A2_R, ImmType.B, N),

          // CSR
          CSRRW -> List(S_CSR, ALUOp.COPY_A, A1_R,  A2_R, ImmType.Z, Y),
          CSRRWI-> List(S_CSR, ALUOp.COPY_A, A1_ZM, A2_R, ImmType.Z, Y)
        )
        
        // 查表
        val decoded = ListLookup(inst, List(S_NO, ALUOp.NOP, A1_R, A2_R, ImmType.Z, N), map)
        
        val service = decoded(0).asTypeOf(ServiceType())
        val aluOp   = decoded(1).asTypeOf(ALUOp())
        val arg1Type= decoded(2).asTypeOf(Arg1Type())
        val arg2Type= decoded(3).asTypeOf(Arg2Type())
        val immType = decoded(4).asTypeOf(ImmType())
        val regWen  = decoded(5).asTypeOf(Bool())
        
        // 内存控制信号生成
        val memSize   = funct3(1, 0) // 00=Byte, 01=Half, 10=Word
        val memSigned = !funct3(2)   // Bit2=1 -> Unsigned

        // CSR 副作用配置 (直接在这里做，不再通过 DecodeLogic)
        when(opcode === "b1110011".U) {
          val csrOp = MuxLookup(funct3(1, 0), CSROp.N.asUInt)(Seq(
            1.U -> CSROp.W.asUInt,
            2.U -> CSROp.S.asUInt,
            3.U -> CSROp.C.asUInt
          ))
          csrFile.ioctl(csrOp, 0.U)
        }

        // [D] 立即数生成 & 寄存器读取
        immGen.io.inst := inst
        immGen.io.sel  := immType
        val imm = immGen.io.out
        
        val rs1Val = rfFile.read(rs1)
        val rs2Val = rfFile.read(rs2)
        val zimm   = rs1.asUInt 

        // [E] 操作数选择 (Operand Muxing)
        val arg1 = MuxLookup(arg1Type, 0.U)(Seq(
          Arg1Type.REG  -> rs1Val, Arg1Type.PC   -> curPc,
          Arg1Type.ZERO -> 0.U,    Arg1Type.ZIMM -> zimm
        ))
        val arg2 = MuxLookup(arg2Type, 0.U)(Seq(
          Arg2Type.REG  -> rs2Val, Arg2Type.IMM  -> imm,
          Arg2Type.CONST_4 -> 4.U
        ))

        // [F] 执行与写回
        val writeBackData = WireDefault(0.U(32.W))
        val nextPc        = WireDefault(curPc + 4.U)
        val memAddr       = arg1 + arg2
        val isMMIO        = MemMap.isDifftestSkip(memAddr)

        switch(service) {
          is(ServiceType.ALU)    { writeBackData := exFile.write(arg1, arg2, aluOp.asUInt) }
          is(ServiceType.MEM_RD) { writeBackData := memFile.read(memAddr, memSize, memSigned) }
          is(ServiceType.MEM_WR) { memFile.write(memAddr, rs2Val, memSize) }
          is(ServiceType.BRANCH) {
            writeBackData := curPc + 4.U
            val isTaken = BranchLogic(funct3, rs1Val, rs2Val)
            val target = (arg1 + arg2) & Mux(immType === ImmType.I, ~1.U(32.W), ~0.U(32.W))
            when(immType === ImmType.J || immType === ImmType.I || isTaken) { nextPc := target }
          }
          is(ServiceType.CSR)    { writeBackData := csrFile.write(inst(31, 20), arg1) }
        }

        // [G] 提交状态
        when(regWen) { rfFile.write(rd, writeBackData) }
        pcFile.write(0.U, nextPc)
        
        // [H] 消耗指令
        sys_consume()

        // -------------------------------------------------------
        // 更新物理探针
        // -------------------------------------------------------
        probe_valid := true.B
        probe_pc    := curPc
        probe_inst  := inst
        probe_dnpc  := nextPc
        probe_skip  := (service === ServiceType.MEM_WR || service === ServiceType.MEM_RD) && isMMIO
        probe_halt  := inst === "b000000000001_00000_000_00000_1110011".U

        t.agentPrint("COMMIT PC=%x, INST=%x, RES=%x", curPc, inst, writeBackData)
      }
    }
  }

  def BranchLogic(f3: UInt, r1: UInt, r2: UInt): Bool = {
    MuxLookup(f3, false.B)(Seq(
      0.U -> (r1 === r2), 1.U -> (r1 =/= r2),            
      4.U -> (r1.asSInt < r2.asSInt), 5.U -> (r1.asSInt >= r2.asSInt), 
      6.U -> (r1 < r2), 7.U -> (r1 >= r2)              
    ))
  }
}