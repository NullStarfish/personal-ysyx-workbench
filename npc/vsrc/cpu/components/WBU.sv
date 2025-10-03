// WBU.sv
// (已修改为 Moore FSM Handshake)
import cpu_types_pkg::*;

module WBU (
    input  logic       clk, rst,
    regfile_write_if.master reg_write_port,
    pc_redirect_if.master   pc_redirect_port,
    stage_if.slave     wb_in
);
    // [NEW] 寄存器，用于锁存来自LSU的输入
    lsu_wb_t lsu_wb_payload_reg;
    
    wire wb_in_fire = wb_in.valid && wb_in.ready;

    // [NEW] 输入流水线寄存器锁存
    always_ff @(posedge clk) begin
        if (wb_in_fire) begin
            lsu_wb_payload_reg <= wb_in.payload;
        end
    end

    // [MODIFIED] 输出数据路径总是读取锁存后的数据
    assign reg_write_port.addr = lsu_wb_payload_reg.rd_addr;
    assign reg_write_port.data = lsu_wb_payload_reg.wb_data;
    assign pc_redirect_port.target = lsu_wb_payload_reg.pc_target;

    //----------------------------------------------------------------
    // Moore FSM Handshake Logic
    //----------------------------------------------------------------
    typedef enum logic {S_IDLE, S_COMMIT} wbu_state_e;
    wbu_state_e cur_state, next_state;

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
                // 如果LSU有数据，接收并进入COMMIT状态
                if (wb_in.valid) begin
                    next_state = S_COMMIT;
                end
            end
            S_COMMIT: begin
                // COMMIT状态只持续一个周期，然后无条件返回IDLE
                next_state = S_IDLE;
            end
        endcase
    end
    
    always_comb begin
        // --- 默认输出 ---
        wb_in.ready = 1'b0;
        pc_redirect_port.valid = 1'b0;
        reg_write_port.wen = 1'b0;

        unique case (cur_state)
            S_IDLE: begin
                // IDLE: 准备好接收来自LSU的数据
                wb_in.ready = 1'b1;
            end
            S_COMMIT: begin
                // COMMIT: 正在执行写回，此时不再接收新数据。
                // 根据锁存的控制信号，产生最终的写使能和PC重定向信号。
                reg_write_port.wen = lsu_wb_payload_reg.reg_wen;
                pc_redirect_port.valid = 1'b1; // 总是向IFU发出信号，让IFU决定是否启动下一次取指
            end
        endcase
    end
endmodule