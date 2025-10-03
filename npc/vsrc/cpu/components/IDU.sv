// IDU.sv
// 解码指令，读取寄存器文件，并生成立即数。
// 这是一个纯组合逻辑阶段。
import cpu_types_pkg::*;
`include "riscv_opcodes.sv" // 使用您提供的头文件

module IDU (
    input  logic       clk, rst,
    // 来自 WBU 的寄存器文件写端口
    regfile_write_if.slave reg_write_port,
    // 来自 IFU 的输入
    stage_if.slave     id_in,
    // 输出到 EXU
    stage_if.master    id_out
);


    //----------------------------------------------------------------
    // 指令字段提取
    //----------------------------------------------------------------
    logic [31:0] inst = id_in.payload.inst;
    logic [6:0]  opcode = inst[6:0];
    logic [2:0]  funct3 = inst[14:12];
    logic [6:0]  funct7 = inst[31:25];
    logic [4:0]  rs1_addr = inst[19:15];
    logic [4:0]  rs2_addr = inst[24:20];
    logic [4:0]  rd_addr = inst[11:7];


    id_ex_t id_ex_payload_reg;
    logic id_in_fire = id_in.valid && id_in.ready;
    id_ex_t id_ex_payload_next;



    //----------------------------------------------------------------
    // 寄存器文件 (RegFile)
    //----------------------------------------------------------------
    logic [31:0] rs1_data;
    logic [31:0] rs2_data;

    RegFile u_regfile (
        .clk    (clk),
        .rst    (rst),
        .DataD  (reg_write_port.data),
        .AddrD  (reg_write_port.addr),
        .AddrA  (rs1_addr),
        .AddrB  (rs2_addr),
        .DataA  (rs1_data),
        .DataB  (rs2_data),
        .RegWEn (reg_write_port.wen)
    );

    //----------------------------------------------------------------
    // 立即数生成器 (ImmGen)
    //----------------------------------------------------------------
    immsel_e     imm_sel; // 用于控制 ImmGen 的选择信号
    logic [31:0] imm;     // 从 ImmGen 输出的立即数

    // 注意：您提供的 ImmGen.sv 中 `import riscv_pkg::*;` 可能需要改为 `import cpu_types_pkg::*;`
    // 以便识别在 cpu_types_pkg.sv 中定义的 immsel_e 类型。这里假设工程设置正确。
    ImmGen u_imm_gen (
        .inst_in(inst),
        .ImmSel (imm_sel),
        .imm_out(imm)
    );

    logic br_eq; // Branch Equal output
    logic br_lt; // Branch Less Than output
    logic br_un; // Control signal for unsigned branch comparison
    assign br_un = (funct3 == `FUNCT3_BLTU) || (funct3 == `FUNCT3_BGEU);

    BranchComp u_branch_comp (
        .rs1    (rs1_data),
        .rs2    (rs2_data),
        .BrUn   (br_un),
        .BrEq   (br_eq),
        .BrLT   (br_lt)
    );



    //----------------------------------------------------------------
    // 控制信号解码器 (Control Signal Decoder)
    //----------------------------------------------------------------
    always_comb begin
        // --- 控制信号的默认值 ---
        imm_sel                     = IMM_I; // 默认I类型立即数
        id_ex_payload_next.pc_rs1_sel  = 1'b0;  // 默认: ALU A 输入使用 rs1
        id_ex_payload_next.rs2_imm_sel = 1'b0;  // 默认: ALU B 输入使用 rs2
        id_ex_payload_next.reg_wen     = 1'b0;  // 默认: 不写寄存器
        id_ex_payload_next.alu_opcode  = ALU_ADD;
        id_ex_payload_next.pc_redirect = 1'b0;  // 默认: 无 PC 跳转
        id_ex_payload_next.forjal      = 1'b0;  // 默认: 不是 JAL 指令
        id_ex_payload_next.mem_en      = 1'b0;  // 默认: 无访存
        id_ex_payload_next.mem_wen     = 1'b0;  // 默认: 无内存写
        id_ex_payload_next.rd_addr     = rd_addr;
        id_ex_payload_next.funct3      = funct3;

        // --- 直通的数据 ---
        id_ex_payload_next.pc       = id_in.payload.pc;
        id_ex_payload_next.rs1_data = rs1_data;
        id_ex_payload_next.rs2_data = rs2_data;
        id_ex_payload_next.imm      = imm; // 从 ImmGen 模块获取

        // --- 根据具体指令生成控制信号 ---
        unique case (opcode)
            `OPCODE_LUI: begin
                imm_sel                     = IMM_U;
                id_ex_payload_next.reg_wen     = 1'b1;
                id_ex_payload_next.rs2_imm_sel = 1'b1;
                id_ex_payload_next.alu_opcode  = ALU_COPY_B;
            end
            `OPCODE_AUIPC: begin
                imm_sel                     = IMM_U;
                id_ex_payload_next.reg_wen     = 1'b1;
                id_ex_payload_next.pc_rs1_sel  = 1'b1;
                id_ex_payload_next.rs2_imm_sel = 1'b1;
                id_ex_payload_next.alu_opcode  = ALU_ADD;
            end
            `OPCODE_JAL: begin
                imm_sel                     = IMM_J;
                id_ex_payload_next.reg_wen     = 1'b1;
                id_ex_payload_next.pc_redirect = 1'b1;
                id_ex_payload_next.forjal      = 1'b1;
                id_ex_payload_next.pc_rs1_sel  = 1'b1;
                id_ex_payload_next.rs2_imm_sel = 1'b1;
                id_ex_payload_next.alu_opcode  = ALU_ADD;
            end
            `OPCODE_JALR: begin
                imm_sel                     = IMM_I;
                id_ex_payload_next.reg_wen     = 1'b1;
                id_ex_payload_next.pc_redirect = 1'b1;
                id_ex_payload_next.forjal      = 1'b1;
                id_ex_payload_next.rs2_imm_sel = 1'b1;
                id_ex_payload_next.alu_opcode  = ALU_ADD;
            end
            `OPCODE_BRANCH: begin
                // 对所有分支指令，都需要准备分支目标地址计算 (pc + imm)
                imm_sel                     = IMM_B;
                id_ex_payload_next.pc_rs1_sel  = 1'b1; // ALU input A = PC
                id_ex_payload_next.rs2_imm_sel = 1'b1; // ALU input B = immediate
                id_ex_payload_next.alu_opcode  = ALU_ADD;

                // --- [修改部分] 分支条件判断逻辑 ---
    

                // 1. 根据 funct3 设置比较模式 (有符号/无符号)
                // BLTU (110) 和 BGEU (111) 是无符号比较
                

                // 2. 根据 funct3 和比较结果，判断分支是否应该发生
                case (funct3)
                    `FUNCT3_BEQ:  id_ex_payload_next.pc_redirect = br_eq;      // Branch if Equal
                    `FUNCT3_BNE:  id_ex_payload_next.pc_redirect = ~br_eq;     // Branch if Not Equal
                    `FUNCT3_BLT:  id_ex_payload_next.pc_redirect = br_lt;      // Branch if Less Than (signed)
                    `FUNCT3_BGE:  id_ex_payload_next.pc_redirect = ~br_lt;     // Branch if Greater or Equal (signed)
                    `FUNCT3_BLTU: id_ex_payload_next.pc_redirect = br_lt;      // Branch if Less Than (unsigned)
                    `FUNCT3_BGEU: id_ex_payload_next.pc_redirect = ~br_lt;     // Branch if Greater or Equal (unsigned)
                    default:      id_ex_payload_next.pc_redirect = 1'b0;     // 非法分支指令，不跳转
                endcase

            end
            `OPCODE_LOAD: begin
                imm_sel                     = IMM_I;
                id_ex_payload_next.reg_wen     = 1'b1;
                id_ex_payload_next.rs2_imm_sel = 1'b1;
                id_ex_payload_next.alu_opcode  = ALU_ADD;
                id_ex_payload_next.mem_en      = 1'b1;
            end
            `OPCODE_STORE: begin
                imm_sel                     = IMM_S;
                id_ex_payload_next.rs2_imm_sel = 1'b1;
                id_ex_payload_next.alu_opcode  = ALU_ADD;
                id_ex_payload_next.mem_en      = 1'b1;
                id_ex_payload_next.mem_wen     = 1'b1;
            end
            `OPCODE_I_TYPE: begin
                imm_sel                     = IMM_I;
                id_ex_payload_next.reg_wen     = 1'b1;
                id_ex_payload_next.rs2_imm_sel = 1'b1;
                case (funct3)
                    `FUNCT3_ADDI_ADD:   id_ex_payload_next.alu_opcode = ALU_ADD;
                    `FUNCT3_SLTI_SLT:   id_ex_payload_next.alu_opcode = ALU_SLT;
                    `FUNCT3_SLTIU_SLTU: id_ex_payload_next.alu_opcode = ALU_SLTU;
                    `FUNCT3_XORI_XOR:   id_ex_payload_next.alu_opcode = ALU_XOR;
                    `FUNCT3_ORI_OR:     id_ex_payload_next.alu_opcode = ALU_OR;
                    `FUNCT3_ANDI_AND:   id_ex_payload_next.alu_opcode = ALU_AND;
                    `FUNCT3_SLLI_SLL:   id_ex_payload_next.alu_opcode = ALU_SLL;
                    `FUNCT3_SRLI_SRAI:  id_ex_payload_next.alu_opcode = (funct7[5]) ? ALU_SRA : ALU_SRL;
                    default:            id_ex_payload_next.alu_opcode = ALU_ADD;
                endcase
            end
            `OPCODE_R_TYPE: begin
                id_ex_payload_next.reg_wen = 1'b1;
                case (funct3)
                    `FUNCT3_ADDI_ADD:   id_ex_payload_next.alu_opcode = (funct7[5]) ? ALU_SUB : ALU_ADD;
                    `FUNCT3_SLLI_SLL:   id_ex_payload_next.alu_opcode = ALU_SLL;
                    `FUNCT3_SLTI_SLT:   id_ex_payload_next.alu_opcode = ALU_SLT;
                    `FUNCT3_SLTIU_SLTU: id_ex_payload_next.alu_opcode = ALU_SLTU;
                    `FUNCT3_XORI_XOR:   id_ex_payload_next.alu_opcode = ALU_XOR;
                    `FUNCT3_SRLI_SRAI:  id_ex_payload_next.alu_opcode = (funct7[5]) ? ALU_SRA : ALU_SRL;
                    `FUNCT3_ORI_OR:     id_ex_payload_next.alu_opcode = ALU_OR;
                    `FUNCT3_ANDI_AND:   id_ex_payload_next.alu_opcode = ALU_AND;
                    default:            id_ex_payload_next.alu_opcode = ALU_ADD;
                endcase
            end
            default: begin
                // 非法指令，保持默认值
            end
        endcase
    end

// [NEW] 流水线寄存器锁存逻辑
    always_ff @(posedge clk) begin
        if (rst) begin
            // 在复位时可以清零，也可以不清，取决于设计
        end else if (id_in_fire) begin
            id_ex_payload_reg <= id_ex_payload_next;
        end
    end

    //----------------------------------------------------------------
    // Moore FSM Handshake Logic
    //----------------------------------------------------------------
    typedef enum logic {S_IDLE, S_WAIT_EXU} idu_state_e; 
    idu_state_e cur_state, next_state;

    // 状态寄存器
    always_ff @(posedge clk) begin
        if (rst) begin
            cur_state <= S_IDLE;
        end else begin
            cur_state <= next_state;
        end
    end

    // 状态转移逻辑 (只计算 next_state)
    always_comb begin
        next_state = cur_state;
        unique case(cur_state)
            S_IDLE: begin
                // 在IDLE状态，如果IFU有有效数据，则接收并进入WAIT状态
                if (id_in.valid) begin
                    next_state = S_WAIT_EXU;
                end 
            end
            S_WAIT_EXU: begin
                // 在WAIT状态，如果EXU接收了数据，则返回IDLE状态
                if (id_out.ready) begin
                    next_state = S_IDLE;
                end 
            end
        endcase
    end

    // 输出逻辑 (只依赖于 cur_state)
    always_comb begin
        id_out.payload = id_ex_payload_reg; // 输出总是来自寄存器

        // --- 默认输出 ---
        id_in.ready = 1'b0;
        id_out.valid = 1'b0;

        unique case(cur_state)
            S_IDLE: begin
                // IDLE: 准备好接收IFU的数据，不向下游发送有效数据
                id_in.ready = 1'b1;
                id_out.valid = 1'b0;
            end
            S_WAIT_EXU: begin
                // WAIT_EXU: 未准备好接收新数据，正在向下游发送有效数据
                id_in.ready = 1'b0;
                id_out.valid = 1'b1;
            end
        endcase
    end

endmodule

