

import cpu_types_pkg::*;


module BoothMultiplier (
    input  logic             clk,
    input  logic             rst,

    // // --- Decoupled Input Handshake (Slave) ---
    // input  logic             in_valid,
    // input  logic [31:0]      operand_a,
    // input  logic [31:0]      operand_b,
    // input  riscv_mul_op_e    mul_op,
    // output logic             in_ready,

    // // --- Decoupled Output Handshake (Master) ---
    // output logic             out_valid,
    // output logic [31:0]      out_result,
    // input  logic             out_ready



    stage_if.slave          mul_in,
    stage_if.master         mul_out
);


    
 // --- Interface Mapping ---
    wire [31:0]      operand_a = mul_in.payload.dataA;
    wire [31:0]      operand_b = mul_in.payload.dataB;
    riscv_mul_op_e   mul_op    = mul_in.payload.opcode;
    wire             in_valid  = mul_in.valid;
    logic            in_ready;
    assign mul_in.ready = in_ready;

    logic            out_valid;
    wire [31:0]      out_result;
    wire             out_ready = mul_out.ready;
    assign mul_out.valid   = out_valid;
    assign mul_out.payload = out_result; 

    // FSM States, Registers, and Sequential block remain the same...
    typedef enum logic [2:0] { S_IDLE, S_FIND_MSB, S_PREP, S_CALC, S_DONE } state_e;
    state_e current_state, next_state;

    // --- Internal State Registers ---
    logic [65:0] product_reg;
    logic        q_minus_1_reg;
    logic [5:0]  count_reg;
    logic [5:0]  dynamic_count_reg; 
    riscv_mul_op_e mul_op_reg;
    logic [31:0]   operand_a_reg;
    logic [31:0]   operand_b_reg;
    logic [2:0]    msb_find_count_reg;
    logic [4:0]    msb_index_reg;
    // --- Wires for Next-State Values ---
    logic [65:0] product_reg_next;
    logic        q_minus_1_reg_next;
    logic [5:0]  count_reg_next;
    logic [5:0]  dynamic_count_reg_next;
    riscv_mul_op_e mul_op_reg_next;
    logic [31:0]   operand_a_reg_next;
    logic [31:0]   operand_b_reg_next;
    logic [2:0]    msb_find_count_reg_next;
    logic [4:0]    msb_index_reg_next;

    // --- Sequential Logic --- (Unchanged)
    always_ff @(posedge clk) begin
        if (rst) begin
            current_state      <= S_IDLE;
            product_reg        <= 66'd0;
            q_minus_1_reg      <= 1'b0;
            count_reg          <= 6'd0;
            dynamic_count_reg  <= 6'd0;
            mul_op_reg         <= MUL_NONE;
            operand_a_reg      <= 32'd0;
            operand_b_reg      <= 32'd0;
            msb_find_count_reg <= 3'd0;
            msb_index_reg      <= 5'd0;
        end else begin
            current_state      <= next_state;
            product_reg        <= product_reg_next;
            q_minus_1_reg      <= q_minus_1_reg_next;
            count_reg          <= count_reg_next;
            dynamic_count_reg  <= dynamic_count_reg_next;
            mul_op_reg         <= mul_op_reg_next;
            operand_a_reg      <= operand_a_reg_next;
            operand_b_reg      <= operand_b_reg_next;
            msb_find_count_reg <= msb_find_count_reg_next;
            msb_index_reg      <= msb_index_reg_next;
        end
    end

    // --- Combinational FSM Logic --- (Unchanged)
    logic [66:0] shifted_result_67b;
    always_comb begin
        next_state              = current_state;
        product_reg_next        = product_reg;
        q_minus_1_reg_next      = q_minus_1_reg;
        count_reg_next          = count_reg;
        dynamic_count_reg_next  = dynamic_count_reg;
        mul_op_reg_next         = mul_op_reg;
        operand_a_reg_next      = operand_a_reg;
        operand_b_reg_next      = operand_b_reg;
        msb_find_count_reg_next = msb_find_count_reg;
        msb_index_reg_next      = msb_index_reg;
        in_ready  = 1'b0;
        out_valid = 1'b0;
        // ... FSM case statement is identical to the previous version ...
        case (current_state)
            S_IDLE: begin
                in_ready = 1'b1;
                if (in_valid) begin
                    next_state              = S_FIND_MSB;
                    operand_a_reg_next      = operand_a;
                    operand_b_reg_next      = operand_b;
                    mul_op_reg_next         = mul_op;
                    msb_find_count_reg_next = 3'd0;
                    msb_index_reg_next      = 5'd0;
                end
            end
            S_FIND_MSB: begin
                logic [31:0] search_vector;
                search_vector = operand_b_reg ^ {32{operand_b_reg[31]}};
                msb_find_count_reg_next = msb_find_count_reg + 1;
                case (msb_find_count_reg)
                    0: begin if (search_vector[31:16] != 0) msb_index_reg_next = msb_index_reg + 16; end
                    1: begin if (search_vector[msb_index_reg + 8 +: 8] != 0) msb_index_reg_next = msb_index_reg + 8; end
                    2: begin if (search_vector[msb_index_reg + 4 +: 4] != 0) msb_index_reg_next = msb_index_reg + 4; end
                    3: begin if (search_vector[msb_index_reg + 2 +: 2] != 0) msb_index_reg_next = msb_index_reg + 2; end
                    4: begin 
                        if (search_vector[msb_index_reg + 1] != 0) begin
                            msb_index_reg_next = msb_index_reg + 1;
                        end
                        dynamic_count_reg_next = msb_index_reg_next + 2;
                        next_state             = S_PREP;
                    end
                endcase
            end
            S_PREP: begin
                next_state         = S_CALC;
                product_reg_next   = {34'b0, operand_b_reg};
                q_minus_1_reg_next = 1'b0;
                count_reg_next     = dynamic_count_reg;
            end
            S_CALC: begin
                logic [1:0]  booth_bits = {product_reg[0], q_minus_1_reg};
                logic [33:0] m_extended_34b = {{operand_a_reg[31]}, {operand_a_reg[31]}, operand_a_reg};
                logic [65:0] temp_prod_after_add = product_reg;
                case (booth_bits)
                    2'b01: temp_prod_after_add[65:32] = $signed(product_reg[65:32]) + $signed(m_extended_34b);
                    2'b10: temp_prod_after_add[65:32] = $signed(product_reg[65:32]) - $signed(m_extended_34b);
                    default:;
                endcase
                shifted_result_67b = $signed({temp_prod_after_add[65], temp_prod_after_add}) >>> 1;
                product_reg_next   = shifted_result_67b[65:0];
                q_minus_1_reg_next = product_reg[0];
                count_reg_next     = count_reg - 1'b1;
                if (count_reg == 6'd1) begin
                    next_state = S_DONE;
                end
            end
            S_DONE: begin
                out_valid = 1'b1;
                if (out_ready) begin
                    next_state = S_IDLE;
                end
            end
            default: begin
                next_state = S_IDLE;
            end
        endcase
    end
    
    // --- Final Result Calculation (Purely Combinational) ---
    logic [31:0] comb_result;
    always_comb begin
        logic [63:0] booth_raw_result;
        logic [63:0] signed_product;
        logic [63:0] final_product;
        logic [5:0]  shift_amount;
        logic [63:0] aligned_result;

        if (current_state == S_CALC && next_state == S_DONE) begin
            booth_raw_result = product_reg_next[63:0];
        end else begin
            booth_raw_result = product_reg[63:0];
        end

        shift_amount = 32 - dynamic_count_reg;
        aligned_result = $signed(booth_raw_result) >>> shift_amount;
        
        signed_product = aligned_result;
        final_product = signed_product;

        unique case (mul_op_reg)
            MULHU: begin
                logic [63:0] correction_term;
                correction_term = (operand_a_reg[31] ? {operand_b_reg, 32'b0} : 64'd0) +
                                  (operand_b_reg[31] ? {operand_a_reg, 32'b0} : 64'd0);
                // The correct formula is to ADD the correction term, not subtract it.
                final_product = signed_product + correction_term; 
            end
            // MULHSU logic is correct as is.
            // *** FIX 2: Correct MULHSU logic ***
            MULHSU: begin
                // If B is treated as unsigned and its MSB is 1, it means its signed value
                // was interpreted as negative. We correct this by adding A*2^32.
                if (operand_b_reg[31]) begin
                    final_product = signed_product + {operand_a_reg, 32'b0};
                end
            end
            default: ; 
        endcase

        unique case (mul_op_reg)
            MUL:    comb_result = final_product[31:0];
            MULH, MULHU, MULHSU: comb_result = final_product[63:32];
            default: comb_result = 32'hdeadbeef;
        endcase
    end
    
    assign out_result = comb_result;


endmodule