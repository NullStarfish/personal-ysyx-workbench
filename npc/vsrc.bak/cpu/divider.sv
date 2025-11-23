

// =========================================
// Package Definition
// =========================================
import exu_types_pkg::*;

// =========================================
// Optimized Non-Restoring Divider Module (Corrected for Interfaces)
// =========================================
module OptimizedDivider (
    input  logic             clk,
    input  logic             rst,
    stage_if.slave          div_in,
    stage_if.master         div_out
);

    // --- Interface Connections ---
    // Create local wires from the input interface for easier use in existing code
    wire             in_valid  = div_in.valid;
    wire [31:0]      operand_a = div_in.payload.dataA;
    wire [31:0]      operand_b = div_in.payload.dataB;
    wire riscv_div_op_e div_op    = div_in.payload.opcode;
    wire             out_ready = div_out.ready;

    // Declare internal logic signals that will drive the output interface
    logic            in_ready;
    logic            out_valid;

    // FSM States, Internal Registers, and Next State Wires remain the same
    typedef enum logic [3:0] { 
        S_IDLE, S_ANALYZE, S_FIND_MSB, S_PREP, S_CALC, 
        S_POST_CORRECT, S_LATCH_RESULT, S_WAIT_ALU 
    } state_e;
    state_e             current_state, next_state;
    logic signed [32:0] remainder_reg;
    logic [31:0]        quotient_reg;
    logic [31:0]        divisor_reg;
    logic [5:0]         count_reg;
    logic [5:0]         dynamic_count_reg;
    riscv_div_op_e      div_op_reg;
    logic               sign_a_reg, sign_b_reg;
    logic               is_div_by_zero_reg;
    logic [31:0]        original_dividend_reg, original_divisor_reg, final_result_reg;
    logic [31:0]        dividend_abs_reg;
    logic [2:0]         msb_find_count_reg;
    logic [4:0]         msb_index_reg;
    logic signed [32:0] remainder_reg_next;
    logic [31:0]        quotient_reg_next, divisor_reg_next;
    logic [5:0]         count_reg_next, dynamic_count_reg_next;
    riscv_div_op_e      div_op_reg_next;
    logic               sign_a_reg_next, sign_b_reg_next;
    logic               is_div_by_zero_reg_next;
    logic [31:0]        original_dividend_reg_next, original_divisor_reg_next, final_result_reg_next;
    logic [31:0]        dividend_abs_reg_next;
    logic [2:0]         msb_find_count_reg_next;
    logic [4:0]         msb_index_reg_next;

    // Sequential Block is unchanged
    always_ff @(posedge clk) begin
        if (rst) begin
            current_state         <= S_IDLE;
            remainder_reg         <= 33'd0;
            quotient_reg          <= 32'd0;
            divisor_reg           <= 32'd0;
            count_reg             <= 6'd0;
            dynamic_count_reg     <= 6'd0;
            div_op_reg            <= DIV_NONE;
            sign_a_reg            <= 1'b0;
            sign_b_reg            <= 1'b0;
            is_div_by_zero_reg    <= 1'b0;
            original_dividend_reg <= 32'd0;
            original_divisor_reg  <= 32'd0;
            final_result_reg      <= 32'd0;
            dividend_abs_reg      <= 32'd0;
            msb_find_count_reg    <= 3'd0;
            msb_index_reg         <= 5'd0;
        end else begin
            current_state         <= next_state;
            remainder_reg         <= remainder_reg_next;
            quotient_reg          <= quotient_reg_next;
            divisor_reg           <= divisor_reg_next;
            count_reg             <= count_reg_next;
            dynamic_count_reg     <= dynamic_count_reg_next;
            div_op_reg            <= div_op_reg_next;
            sign_a_reg            <= sign_a_reg_next;
            sign_b_reg            <= sign_b_reg_next;
            is_div_by_zero_reg    <= is_div_by_zero_reg_next;
            original_dividend_reg <= original_dividend_reg_next;
            original_divisor_reg  <= original_divisor_reg_next;
            final_result_reg      <= final_result_reg_next;
            dividend_abs_reg      <= dividend_abs_reg_next;
            msb_find_count_reg    <= msb_find_count_reg_next;
            msb_index_reg         <= msb_index_reg_next;
        end
    end

    // Combinational Block
    always_comb begin
        logic is_signed;
        logic [31:0] dividend_abs;
        logic [31:0] divisor_abs;
        logic signed [32:0] shifted_remainder;
        logic signed [32:0] result_remainder;

        // Defaults
        next_state = current_state;
        remainder_reg_next = remainder_reg;
        quotient_reg_next = quotient_reg;
        divisor_reg_next = divisor_reg;
        count_reg_next = count_reg;
        dynamic_count_reg_next = dynamic_count_reg;
        div_op_reg_next = div_op_reg;
        sign_a_reg_next = sign_a_reg;
        sign_b_reg_next = sign_b_reg;
        is_div_by_zero_reg_next = is_div_by_zero_reg;
        original_dividend_reg_next = original_dividend_reg;
        original_divisor_reg_next = original_divisor_reg;
        final_result_reg_next = final_result_reg;
        dividend_abs_reg_next = dividend_abs_reg;
        msb_find_count_reg_next = msb_find_count_reg;
        msb_index_reg_next = msb_index_reg;
        in_ready = 1'b0;
        out_valid = 1'b0;
        
        is_signed = 1'b0;
        dividend_abs = 32'b0;
        divisor_abs = 32'b0;
        shifted_remainder = 33'b0;
        result_remainder = 33'b0;
        
        case (current_state)
            S_IDLE: begin
                in_ready = 1'b1;
                if (in_valid) begin
                    next_state = S_ANALYZE;
                    div_op_reg_next = div_op;
                    original_dividend_reg_next = operand_a;
                    original_divisor_reg_next = operand_b;
                    is_div_by_zero_reg_next = (operand_b == 32'd0);
                end
            end

            S_ANALYZE: begin
                is_signed = (div_op_reg == DIV || div_op_reg == REM);
                dividend_abs = is_signed ? (original_dividend_reg[31] ? -original_dividend_reg : original_dividend_reg) : original_dividend_reg;

                if (dividend_abs == 32'd0) begin
                    // 对于被除数为0的特殊情况，在这里直接设置周期数
                    dynamic_count_reg_next = 1;
                    next_state = S_PREP;
                end else begin
                    dividend_abs_reg_next = dividend_abs;
                    msb_find_count_reg_next = 0;
                    msb_index_reg_next = 0;
                    next_state = S_FIND_MSB;
                end
            end
            
            S_FIND_MSB: begin
                msb_find_count_reg_next = msb_find_count_reg + 1;
                
                case (msb_find_count_reg)
                    0: begin
                        if (dividend_abs_reg[31:16] != 0) msb_index_reg_next = msb_index_reg + 16;
                    end
                    1: begin
                        if (dividend_abs_reg[msb_index_reg + 8 +: 8] != 0) msb_index_reg_next = msb_index_reg + 8;
                    end
                    2: begin
                        if (dividend_abs_reg[msb_index_reg + 4 +: 4] != 0) msb_index_reg_next = msb_index_reg + 4;
                    end
                    3: begin
                        if (dividend_abs_reg[msb_index_reg + 2 +: 2] != 0) msb_index_reg_next = msb_index_reg + 2;
                    end
                    4: begin
                        if (dividend_abs_reg[msb_index_reg + 1] != 0) begin
                            msb_index_reg_next = msb_index_reg + 1;
                        end
                        // *** 逻辑修正 ***
                        // 在 S_FIND_MSB 的最后一个周期，计算并设置下一个周期的 dynamic_count_reg
                        dynamic_count_reg_next = msb_index_reg_next + 1;
                        next_state = S_PREP;
                    end
                endcase
            end

            S_PREP: begin
                // *** 逻辑修正 ***
                // 简化 S_PREP 状态，它现在直接使用 dynamic_count_reg 的值，
                // 这个值已由前一个状态 (S_ANALYZE 或 S_FIND_MSB) 正确设置。
                if (is_div_by_zero_reg) begin
                    next_state = S_LATCH_RESULT;
                end else begin
                    next_state = S_CALC;
                    is_signed = (div_op_reg == DIV || div_op_reg == REM);
                    dividend_abs = is_signed ? (original_dividend_reg[31] ? -original_dividend_reg : original_dividend_reg) : original_dividend_reg;
                    divisor_abs  = is_signed ? (original_divisor_reg[31]  ? -original_divisor_reg  : original_divisor_reg)  : original_divisor_reg;

                    sign_a_reg_next = original_dividend_reg[31] && is_signed;
                    sign_b_reg_next = original_divisor_reg[31]  && is_signed;
                    divisor_reg_next = divisor_abs;
                    remainder_reg_next = 33'd0;
                    
                    // 使用已在上一周期计算好的 dynamic_count_reg 进行预移位
                    quotient_reg_next = dividend_abs << (32 - dynamic_count_reg);
                    // 初始化计算周期的计数器
                    count_reg_next = dynamic_count_reg;
                end
            end

            S_CALC: begin
                shifted_remainder = (remainder_reg << 1) | {32'b0, quotient_reg[31]};

                if (remainder_reg[32])  result_remainder = shifted_remainder + signed'({1'b0, divisor_reg});
                else                    result_remainder = shifted_remainder - signed'({1'b0, divisor_reg});
                
                remainder_reg_next = result_remainder;
                quotient_reg_next  = {quotient_reg[30:0], ~result_remainder[32]};
                count_reg_next     = count_reg - 1'b1;
                
                if (count_reg == 6'd1) begin
                    next_state = S_POST_CORRECT;
                end
            end
            
            S_POST_CORRECT: begin
                next_state = S_LATCH_RESULT;
                if (remainder_reg[32]) begin
                    remainder_reg_next = remainder_reg + signed'({1'b0, divisor_reg});
                end
            end
            
            S_LATCH_RESULT: begin
                next_state = S_WAIT_ALU;
                final_result_reg_next = comb_result;
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

    // Combinational Result Logic and Debug Display Logic are unchanged
    logic [31:0] comb_result;
    always_comb begin
        logic [31:0] final_quotient;
        logic [31:0] final_remainder;
        logic is_overflow;

        logic signed [32:0] corrected_remainder_val = remainder_reg;
        if (current_state == S_POST_CORRECT && remainder_reg[32]) begin
             corrected_remainder_val = remainder_reg + signed'({1'b0, divisor_reg});
        end

        final_quotient  = quotient_reg;
        final_remainder = corrected_remainder_val[31:0];

        if (div_op_reg == DIV || div_op_reg == REM) begin
            if (sign_a_reg ^ sign_b_reg) final_quotient = -final_quotient;
            if (sign_a_reg) final_remainder = -final_remainder;
        end
        is_overflow = (div_op_reg == DIV) && (original_dividend_reg == 32'h80000000) && (original_divisor_reg == 32'hffffffff);

        if (is_div_by_zero_reg) begin
            case (div_op_reg)
                DIVU, DIV: comb_result = 32'hFFFFFFFF;
                REMU, REM: comb_result = original_dividend_reg;
                default:   comb_result = 32'hdeadbeef;
            endcase
        end else if (is_overflow) begin
             case (div_op_reg)
                DIV: comb_result = 32'h80000000;
                REM: comb_result = 32'd0;
                default: comb_result = 32'hdeadbeef;
            endcase
        end else begin
            case (div_op_reg)
                DIVU, DIV: comb_result = final_quotient;
                REMU, REM: comb_result = final_remainder;
                default:   comb_result = 32'hdeadbeef;
            endcase
        end
    end



    // --- Final Interface Assignments ---
    assign div_in.ready    = in_ready;
    assign div_out.valid   = out_valid;
    assign div_out.payload = final_result_reg;

endmodule