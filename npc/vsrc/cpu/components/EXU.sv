// EXU.sv
// (已修改为 Moore FSM Handshake)
import cpu_types_pkg::*;

module EXU (
    input  logic      clk, rst,
    stage_if.slave    ex_in,
    stage_if.master   ex_out
);
    // [NEW] 寄存器，用于锁存来自IDU的输入
    id_ex_t id_ex_payload_reg;
    // [NEW] 寄存器，用于锁存要发往LSU的输出
    ex_lsu_t ex_lsu_payload_reg;

    wire ex_in_fire = ex_in.valid && ex_in.ready;

    // [NEW] 输入流水线寄存器锁存逻辑
    always_ff @(posedge clk) begin
        if (ex_in_fire) begin
            id_ex_payload_reg <= ex_in.payload;
        end
    end

    // --- 数据路径计算 (组合逻辑) ---
    logic [31:0] alu_in_a, alu_in_b;
    assign alu_in_a = id_ex_payload_reg.pc_rs1_sel ? id_ex_payload_reg.pc : id_ex_payload_reg.rs1_data;
    assign alu_in_b = id_ex_payload_reg.rs2_imm_sel ? id_ex_payload_reg.imm : id_ex_payload_reg.rs2_data;

    logic [31:0] alu_result;
    logic        alu_ready;
    logic        alu_valid; // 由状态机控制
    ALU alu_unit (
        .valid(alu_valid),
        .ready(alu_ready),
        .clk(clk),
        .rst(rst),
        .A(alu_in_a),
        .B(alu_in_b),
        .ALUSel(id_ex_payload_reg.alu_opcode),
        .Results(alu_result)
    );

    // [NEW] 计算临时的 "next" 输出 payload
    ex_lsu_t ex_lsu_payload_next;
    always_comb begin
        logic [31:0] pc_plus_4 = id_ex_payload_reg.pc + 32'd4;
        
        ex_lsu_payload_next.pc_target = id_ex_payload_reg.pc_redirect ? alu_result : pc_plus_4;
        ex_lsu_payload_next.exu_result = id_ex_payload_reg.forjal ? pc_plus_4 : alu_result;
        ex_lsu_payload_next.reg_wen   = id_ex_payload_reg.reg_wen;
        ex_lsu_payload_next.rd_addr   = id_ex_payload_reg.rd_addr;
        ex_lsu_payload_next.mem_en    = id_ex_payload_reg.mem_en;
        ex_lsu_payload_next.mem_wen   = id_ex_payload_reg.mem_wen;
        ex_lsu_payload_next.mem_wdata = id_ex_payload_reg.rs2_data;
        ex_lsu_payload_next.mem_addr  = alu_result;
        ex_lsu_payload_next.funct3    = id_ex_payload_reg.funct3;
    end

    // [NEW] 输出流水线寄存器锁存逻辑
    always_ff @(posedge clk) begin
        // 当ALU完成计算时，锁存结果
        if (cur_state == S_BUSY && alu_ready) begin
            ex_lsu_payload_reg <= ex_lsu_payload_next;
        end
    end
    
    //----------------------------------------------------------------
    // Moore FSM Handshake Logic
    //----------------------------------------------------------------
    typedef enum logic [1:0] {
        S_IDLE, S_BUSY, S_WAIT_LSU
    } ex_state_e;
    ex_state_e cur_state, next_state;

    // 状态寄存器
    always_ff @(posedge clk) begin
        if (rst) begin
            cur_state <= S_IDLE;
        end else begin
            cur_state <= next_state;
        end
    end

    // 状态转移逻辑
    always_comb begin
        next_state = cur_state;
        unique case (cur_state)
            S_IDLE: begin
                // IDLE: 如果IDU有数据，接收并开始计算
                if (ex_in.valid) begin
                    next_state = S_BUSY;
                end
            end
            S_BUSY: begin
                // BUSY: 如果ALU计算完成，进入等待状态
                if (alu_ready) begin
                    next_state = S_WAIT_LSU;
                end
            end
            S_WAIT_LSU: begin
                // WAIT_LSU: 如果LSU接收了数据，返回IDLE
                if (ex_out.ready) begin
                    next_state = S_IDLE;
                end
            end
        endcase
    end
    
    // 输出逻辑
    always_comb begin
        ex_out.payload = ex_lsu_payload_reg; // 输出总是来自寄存器

        // --- 默认输出 ---
        ex_in.ready  = 1'b0;
        ex_out.valid = 1'b0;
        alu_valid    = 1'b0;

        unique case (cur_state)
            S_IDLE: begin
                // IDLE: 准备好接收数据，ALU和输出无效
                ex_in.ready = 1'b1;
            end
            S_BUSY: begin
                // BUSY: 正在计算，启动ALU
                alu_valid = 1'b1;
            end
            S_WAIT_LSU: begin
                // WAIT_LSU: 计算完成，向下游发送有效数据
                ex_out.valid = 1'b1;
            end
        endcase
    end
    
endmodule