

import cpu_types_pkg::*;

`define BOOTH_ITER_DEBUG

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

    // ... interface mapping ...
    typedef enum logic [1:0] { S_IDLE, S_PREP, S_CALC, S_DONE } state_e;
    state_e current_state, next_state;

    // --- Internal State Registers ---
    logic [65:0] product_reg;
    logic        q_minus_1_reg;
    logic [5:0]  count_reg;
    riscv_mul_op_e mul_op_reg;
    logic [31:0]   operand_a_reg;
    logic [31:0]   operand_b_reg;

    // --- Wires for Next-State Values ---
    logic [65:0] product_reg_next;
    logic        q_minus_1_reg_next;
    logic [5:0]  count_reg_next;
    riscv_mul_op_e mul_op_reg_next;
    logic [31:0]   operand_a_reg_next;
    logic [31:0]   operand_b_reg_next;

    // --- Sequential Logic ---
    always_ff @(posedge clk) begin
        if (rst) begin
            current_state <= S_IDLE;
            product_reg   <= 66'd0;
            q_minus_1_reg <= 1'b0;
            count_reg     <= 6'd0;
            mul_op_reg    <= MUL_NONE;
            operand_a_reg <= 32'd0;
            operand_b_reg <= 32'd0;
        end else begin
            current_state <= next_state;
            product_reg   <= product_reg_next;
            q_minus_1_reg <= q_minus_1_reg_next;
            count_reg     <= count_reg_next;
            mul_op_reg    <= mul_op_reg_next;
            operand_a_reg <= operand_a_reg_next;
            operand_b_reg <= operand_b_reg_next;
        end
    end

    // --- Combinational Logic ---
    logic [66:0] shifted_result_67b;
    always_comb begin
        next_state         = current_state;
        product_reg_next   = product_reg;
        q_minus_1_reg_next = q_minus_1_reg;
        count_reg_next     = count_reg;
        mul_op_reg_next    = mul_op_reg;
        operand_a_reg_next = operand_a_reg;
        operand_b_reg_next = operand_b_reg;

        in_ready  = 1'b0;
        out_valid = 1'b0;

        case (current_state)
            S_IDLE: begin
                in_ready = 1'b1;
                if (in_valid) begin
                    next_state         = S_PREP;
                    operand_a_reg_next = operand_a;
                    operand_b_reg_next = operand_b;
                    mul_op_reg_next    = mul_op;
                end
            end
            S_PREP: begin
                next_state = S_CALC;
                product_reg_next   = {34'b0, operand_b_reg};
                q_minus_1_reg_next = 1'b0;
                count_reg_next     = 6'd32;
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
                
                q_minus_1_reg_next = product_reg[0];


                shifted_result_67b = $signed({temp_prod_after_add[65], temp_prod_after_add}) >>> 1;
                product_reg_next = shifted_result_67b[65:0];
                
                count_reg_next = count_reg - 1'b1;

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
        endcase
    end
    
        // --- Final Result Calculation (Purely Combinational) ---
    logic [31:0] comb_result;
    always_comb begin
        logic [63:0] booth_raw_result;
        logic [63:0] signed_product;
        logic [63:0] final_product;

        // When the FSM is about to enter or is in the DONE state, the final
        // iterative result is available in `product_reg_next`.
        if (next_state == S_DONE) begin
            booth_raw_result = product_reg_next[63:0];
        end else begin
            booth_raw_result = product_reg[63:0];
        end

        // *** BUG FIX IS HERE ***
        // The raw result from the iterative Booth process is the correct signed product.
        // The previous conditional correction was incorrect and has been removed.
        signed_product = booth_raw_result;

        final_product = signed_product; // Initialize final_product with the signed result

        unique case (mul_op_reg)
            MULHU: begin
                logic [63:0] correction_term;
                // Correction to get unsigned product from signed product:
                // U_prod = S_prod + (A<0 ? B<<32 : 0) + (B<0 ? A<<32 : 0)
                correction_term = (operand_a_reg[31] ? {operand_b_reg, 32'b0} : 64'd0) +
                                (operand_b_reg[31] ? {operand_a_reg, 32'b0} : 64'd0);
                final_product = signed_product + correction_term;
            end
            MULHSU: begin
                // Correction for signed * unsigned:
                // SU_prod = S_prod + (B<0 ? A<<32 : 0)
                if (operand_b_reg[31]) begin
                    final_product = signed_product + {operand_a_reg, 32'b0};
                end
            end
            default: ; // For MUL and MULH, final_product is already correct (it's signed_product)
        endcase

        unique case (mul_op_reg)
            MUL:    comb_result = final_product[31:0];
            MULH, MULHU, MULHSU: comb_result = final_product[63:32];
            default: comb_result = 32'hdeadbeef;
        endcase
    end
    
    assign out_result = comb_result;

    // --- DEBUGGING DISPLAY BLOCK ---
    always_ff @(posedge clk) begin
        if (!rst) begin
            if (current_state != next_state) begin
                $display("[MUL @ %0t] FSM: %s -> %s", $time, current_state.name(), next_state.name());
            end
            if (current_state == S_IDLE && next_state == S_PREP) begin
                $display("[MUL @ %0t] New Job: Op=%s, A=0x%h (%d), B=0x%h (%d)", $time, 
                         mul_op.name(), operand_a, $signed(operand_a), operand_b, $signed(operand_b));
            end
`ifdef BOOTH_ITER_DEBUG
            if (current_state == S_CALC) begin
                $display("---------------------------------------------------------");
                $display("[MUL @ %0t] Cycle for Iter %0d:", $time, 32 - (count_reg-1));
                $display("           In:  P=0x%h, Q=0x%h, Q-1=%b", product_reg[65:32], product_reg[31:0], q_minus_1_reg);
                $display("           Op:  Bits {Q0,Q-1}=%b", {product_reg[0], q_minus_1_reg});
                $display("           Out: P=0x%h, Q=0x%h (to be latched for next cycle)", product_reg_next[65:32], product_reg_next[31:0]);
            end
`endif
            if (current_state == S_CALC && next_state == S_DONE) begin
                $display("---------------------------------------------------------");
                $display("[MUL @ %0t] Final Raw Result (P,Q): 0x%h", $time, product_reg_next[63:0]);
                $display("[MUL @ %0t] Final Selected Result : 0x%h", $time, comb_result);
            end
        end
    end

endmodule