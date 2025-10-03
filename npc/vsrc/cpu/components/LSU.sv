// LSU.sv
// (已修改为 Moore FSM Handshake)
import cpu_types_pkg::*;

module LSU (
    input  logic       clk, rst,
    // --- Memory Interface ---
    output logic [31:0] d_addr,
    output logic [31:0] d_wdata,
    output logic [7:0]  d_wmask,
    output logic        d_wen,
    input  logic [31:0] d_rdata_raw,
    input  logic        d_ready,
    output logic        d_valid,
    // --- Pipeline Interface ---
    stage_if.slave     lsu_in,
    stage_if.master    lsu_out
);
    // [NEW] 寄存器，用于锁存来自EXU的输入
    ex_lsu_t ex_lsu_payload_reg;
    // [NEW] 寄存器，用于锁存要发往WBU的输出
    lsu_wb_t lsu_wb_payload_reg;

    wire lsu_in_fire = lsu_in.valid && lsu_in.ready;

    // [NEW] 输入流水线寄存器锁存
    always_ff @(posedge clk) begin
        if (lsu_in_fire) begin
            ex_lsu_payload_reg <= lsu_in.payload;
        end
    end

    // --- 数据路径解码器与逻辑 (基于锁存后的数据) ---
    logic [31:0] load_out;
    LOAD_Decoder u_LOAD_Decoder(
        .raw_addr 	(ex_lsu_payload_reg.mem_addr),
        .raw_data 	(d_rdata_raw),
        .funct3   	(ex_lsu_payload_reg.funct3),
        .out      	(load_out)
    );
    
    STORE_Decoder u_STORE_Decoder(
        .raw_addr 	(ex_lsu_payload_reg.mem_addr),
        .raw_data 	(ex_lsu_payload_reg.mem_wdata),
        .funct3   	(ex_lsu_payload_reg.funct3),
        .wmask    	(d_wmask),
        .wdata    	(d_wdata)
    );
    assign d_addr = ex_lsu_payload_reg.mem_addr;
    assign d_wen  = ex_lsu_payload_reg.mem_wen;
    
    // [NEW] 计算临时的 "next" 输出 payload
    lsu_wb_t lsu_wb_payload_next;
    always_comb begin
        lsu_wb_payload_next.reg_wen   = ex_lsu_payload_reg.reg_wen;
        lsu_wb_payload_next.rd_addr   = ex_lsu_payload_reg.rd_addr;
        lsu_wb_payload_next.pc_target = ex_lsu_payload_reg.pc_target;
        // 根据是否是访存指令，选择写回的数据源
        lsu_wb_payload_next.wb_data   = ex_lsu_payload_reg.mem_en ? load_out : ex_lsu_payload_reg.exu_result;
    end

    // [NEW] 输出流水线寄存器锁存
    always_ff @(posedge clk) begin
        // 对于非访存指令，数据在接收时就准备好了
        if (lsu_in_fire && !lsu_in.payload.mem_en) begin
            lsu_wb_payload_reg <= lsu_wb_payload_next;
        end
        // 对于访存指令，在内存操作完成后才准备好
        else if (cur_state == S_BUSY_MEM && d_ready) begin
            lsu_wb_payload_reg <= lsu_wb_payload_next;
        end
    end

    //----------------------------------------------------------------
    // Moore FSM Handshake Logic
    //----------------------------------------------------------------
    typedef enum logic [1:0] {
        S_IDLE, S_BUSY_MEM, S_WAIT_WBU
    } lsu_state_e;
    
    lsu_state_e cur_state, next_state;

    always_ff @(posedge clk) begin
        if (rst) begin
            cur_state <= S_IDLE;
        end else begin
            cur_state <= next_state;
        end
    end

    always_comb begin
        next_state = cur_state;
        unique case (cur_state)
            S_IDLE: begin
                if (lsu_in.valid) begin
                    // 根据是否是访存指令，决定下一个状态
                    if (lsu_in.payload.mem_en) begin
                        next_state = S_BUSY_MEM; // 需要访存，进入BUSY状态
                    end else begin
                        next_state = S_WAIT_WBU; // 无需访存，直接进入WAIT状态
                    end
                end
            end
            S_BUSY_MEM: begin
                if (d_ready) begin // 内存操作完成
                    next_state = S_WAIT_WBU;
                end
            end
            S_WAIT_WBU: begin
                if (lsu_out.ready) begin // 下游WBU已接收
                    next_state = S_IDLE;
                end
            end
        endcase
    end

    always_comb begin
        lsu_out.payload = lsu_wb_payload_reg; // 输出总是来自寄存器

        // --- 默认输出 ---
        lsu_in.ready = 1'b0;
        lsu_out.valid = 1'b0;
        d_valid = 1'b0;
        
        unique case (cur_state)
            S_IDLE: begin
                // IDLE: 准备好接收来自EXU的数据
                lsu_in.ready = 1'b1;
            end
            S_BUSY_MEM: begin
                // BUSY_MEM: 正在与内存交互
                d_valid = 1'b1;
            end
            S_WAIT_WBU: begin
                // WAIT_WBU: 数据已准备好，等待WBU接收
                lsu_out.valid = 1'b1;
            end
        endcase
    end
endmodule