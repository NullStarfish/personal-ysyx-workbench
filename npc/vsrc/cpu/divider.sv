

// =========================================
// Package Definition
// =========================================
import cpu_types_pkg::*;

// =========================================
// Optimized Non-Restoring Divider Module
// =========================================
module OptimizedDivider (
    input  logic             clk,
    input  logic             rst,

    // --- Decoupled Input Handshake (Slave) ---
    // input  logic             in_valid,
    // input  logic [31:0]      operand_a, // 被除数
    // input  logic [31:0]      operand_b, // 除数
    // input  riscv_div_op_e    div_op,
    // output logic             in_ready,

    // // --- Decoupled Output Handshake (Master) ---
    // output logic             out_valid,
    // output logic [31:0]      out_result,
    // input  logic             out_ready

    stage_if.slave          div_in,
    stage_if.master         div_out

);
    wire in_valid = div_in.valid;
    logic in_ready;
    assign div_in.ready = in_ready;

    wire [31:0] operand_a = div_in.payload.dataA;
    wire [31:0] operand_b = div_in.payload.dataB;
    riscv_div_op_e div_op = div_in.payload.opcode;



    logic out_valid;
    assign div_out.valid = out_valid;

    wire out_ready = div_out.ready;
    wire [31:0] out_result = div_out.payload;


    // --- FSM States ---
    typedef enum logic [2:0] {
        S_IDLE,
        S_PREP,          // 准备阶段：取绝对值，记录符号
        S_CALC,          // 计算阶段：32周期迭代
        S_POST_CORRECT,  // 后修正：如果余数为负，进行恢复
        S_LATCH_RESULT,  // 锁存结果：等待组合逻辑稳定
        S_WAIT_ALU       // 输出握手
    } state_e;
    state_e current_state, next_state;

    // --- Internal Registers ---
    logic signed [32:0] remainder_reg;
    logic [31:0]        quotient_reg;
    logic [31:0]        divisor_reg;      // 存放除数的绝对值
    logic [5:0]         count_reg;

    riscv_div_op_e      div_op_reg;
    logic               sign_a_reg, sign_b_reg;
    logic               is_div_by_zero_reg;
    logic [31:0]        original_dividend_reg; // 锁存原始被除数用于特殊情况判断
    logic [31:0]        original_divisor_reg;  // [修正] 锁存原始除数，防止输入中途变化导致判断错误
    logic [31:0]        final_result_reg;

    // --- FSM State Register ---
    always_ff @(posedge clk) begin
        if (rst) current_state <= S_IDLE;
        else     current_state <= next_state;
    end

    // --- Datapath Registers ---
    always_ff @(posedge clk) begin
        if (rst) begin
            remainder_reg         <= 33'd0;
            quotient_reg          <= 32'd0;
            divisor_reg           <= 32'd0;
            count_reg             <= 6'd0;
            div_op_reg            <= DIV_NONE;
            sign_a_reg            <= 1'b0;
            sign_b_reg            <= 1'b0;
            is_div_by_zero_reg    <= 1'b0;
            original_dividend_reg <= 32'd0;
            original_divisor_reg  <= 32'd0;
            final_result_reg      <= 32'd0;
        end else begin
            case (current_state)
                S_IDLE: begin
                    if (in_valid && in_ready) begin
                        // 在握手成功时刻锁存所有必要的输入信息
                        div_op_reg            <= div_op;
                        original_dividend_reg <= operand_a;
                        original_divisor_reg  <= operand_b;
                        is_div_by_zero_reg    <= (operand_b == 32'd0);
                    end
                end
                S_PREP: begin
                    // 计算绝对值并记录原始符号
                    logic is_signed = (div_op_reg == DIV || div_op_reg == REM);
                    logic [31:0] dividend_abs = is_signed ? (original_dividend_reg[31] ? -original_dividend_reg : original_dividend_reg) : original_dividend_reg;
                    logic [31:0] divisor_abs  = is_signed ? (original_divisor_reg[31]  ? -original_divisor_reg  : original_divisor_reg)  : original_divisor_reg;

                    sign_a_reg <= original_dividend_reg[31] && is_signed;
                    sign_b_reg <= original_divisor_reg[31]  && is_signed;

                    divisor_reg   <= divisor_abs;
                    remainder_reg <= 33'd0;
                    quotient_reg  <= dividend_abs;
                    count_reg     <= 6'd32;
                end
                S_CALC: begin
                    // 标准非恢复余数除法迭代步
                    logic signed [32:0] shifted_remainder;
                    logic signed [32:0] result_remainder;
                    logic [31:0]        next_quotient;

                    // 1. 左移余数和商
                    shifted_remainder = {remainder_reg[31:0], quotient_reg[31]};

                    // 2. 根据当前余数符号决定加减除数
                    if (remainder_reg[32]) begin // 余数为负，加除数
                        result_remainder = shifted_remainder + {1'b0, divisor_reg};
                    end else begin               // 余数为正，减除数
                        result_remainder = shifted_remainder - {1'b0, divisor_reg};
                    end

                    // 3. 更新商的最低位 (注意：非恢复算法中，新商位是结果余数符号的反)
                    next_quotient = {quotient_reg[30:0], ~result_remainder[32]};

                    remainder_reg <= result_remainder;
                    quotient_reg  <= next_quotient;
                    count_reg     <= count_reg - 1'b1;
                end
                S_POST_CORRECT: begin
                    // 如果最终余数为负，进行恢复（加回除数）
                    if (remainder_reg[32]) begin
                        remainder_reg <= remainder_reg + {1'b0, divisor_reg};
                    end
                end
                S_LATCH_RESULT: begin
                    // 锁存最终的组合逻辑结果
                    final_result_reg <= comb_result;
                end
                default: ; // 其他状态保持寄存器不变
            endcase
        end
    end

    // --- Combinational Result Calculation & Corner Case Handling ---
    logic [31:0] comb_result;
    always_comb begin
        logic [31:0] final_quotient;
        logic [31:0] final_remainder;
        logic is_overflow;

        final_quotient  = quotient_reg;
        final_remainder = remainder_reg[31:0];

        // 根据原始操作数符号修正结果符号
        if (div_op_reg == DIV || div_op_reg == REM) begin
            if (sign_a_reg ^ sign_b_reg) final_quotient = -final_quotient;
            // RISC-V规定：余数符号与被除数相同
            if (sign_a_reg) final_remainder = -final_remainder;
        end

        // 溢出判断：仅在有符号除法且为 INT_MIN / -1 时发生
        is_overflow = (div_op_reg == DIV) && (original_dividend_reg == 32'h80000000) && (original_divisor_reg == 32'hffffffff);

        // 最终结果选择逻辑（优先级：除零 > 溢出 > 正常）
        if (is_div_by_zero_reg) begin
            case (div_op_reg)
                DIVU, DIV: comb_result = 32'hFFFFFFFF;        // 除以0，商规定为全1
                REMU, REM: comb_result = original_dividend_reg; // 除以0，余数规定为被除数
                default:   comb_result = 32'h0;
            endcase
        end else if (is_overflow) begin
             case (div_op_reg)
                DIV: comb_result = 32'h80000000; // 溢出规定结果为 INT_MIN
                REM: comb_result = 32'd0;        // 溢出余数为0
                default: comb_result = 32'h0;
            endcase
        end else begin
            case (div_op_reg)
                DIVU, DIV: comb_result = final_quotient;
                REMU, REM: comb_result = final_remainder;
                default:   comb_result = 32'h0;
            endcase
        end
    end

    // --- Control Logic (FSM Next State & Handshakes) ---
    always_comb begin
        next_state = current_state;
        in_ready   = 1'b0;
        out_valid  = 1'b0;

        case (current_state)
            S_IDLE: begin
                in_ready = 1'b1;
                if (in_valid) next_state = S_PREP;
            end
            S_PREP: begin
                // 如果是除以零，直接跳过计算阶段
                if (is_div_by_zero_reg) next_state = S_LATCH_RESULT;
                else next_state = S_CALC;
            end
            S_CALC: begin
                if (count_reg == 6'd1) next_state = S_POST_CORRECT;
            end
            S_POST_CORRECT: begin
                next_state = S_LATCH_RESULT;
            end
            S_LATCH_RESULT: begin
                // 此状态仅停留一周期，用于等待 comb_result 稳定并被 final_result_reg 锁存
                next_state = S_WAIT_ALU;
            end
            S_WAIT_ALU: begin
                out_valid = 1'b1;
                if (out_ready) begin
                    next_state = S_IDLE;
                end
            end
            default: next_state = S_IDLE;
        endcase
    end

    assign out_result = final_result_reg;

endmodule