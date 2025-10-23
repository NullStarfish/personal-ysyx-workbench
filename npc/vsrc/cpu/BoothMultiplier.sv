

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


    wire [31:0] operand_a = mul_in.payload.dataA;
    wire [31:0] operand_b = mul_in.payload.dataB;
    riscv_mul_op_e mul_op = mul_in.payload.opcode;
    wire in_valid         = mul_in.valid;
    logic in_ready;
    assign mul_in.ready = in_ready;

    logic out_valid;
    logic [31:0] out_result;
    wire out_ready = mul_out.ready;
    assign mul_out.valid = out_valid;
    assign mul_out.payload = out_result;





    // --- FSM States for Decoupled Handshake ---
    typedef enum logic [2:0] { S_IDLE, S_PREP, S_CALC, S_LATCH_RESULT, S_WAIT_OUTPUT } state_e;
    state_e current_state, next_state;

    // --- Internal Registers for Booth Algorithm ---
    logic [64:0] product_reg;
    logic [31:0] m_reg;           // Multiplicand (operand_a)
    logic        q_minus_1_reg;
    logic [5:0]  count_reg;
    
    // --- Registers to hold instruction info for final correction ---
    riscv_mul_op_e mul_op_reg;
    logic [31:0]   operand_a_reg;
    logic [31:0]   operand_b_reg;
    logic [31:0]   final_result_reg; // Latched final result for stable output

    // --- FSM State Register ---
    always_ff @(posedge clk) begin
        if (rst) current_state <= S_IDLE;
        else     current_state <= next_state;
    end

    // --- Datapath Registers ---
    always_ff @(posedge clk) begin
        if (rst) begin
            product_reg   <= 65'd0;
            m_reg         <= 32'd0;
            count_reg     <= 6'd0;
            mul_op_reg    <= MUL_NONE;
            q_minus_1_reg <= 1'b0;
            operand_a_reg <= 32'd0;
            operand_b_reg <= 32'd0;
            final_result_reg <= 32'd0;
        end else begin
            case (current_state)
                S_IDLE: begin
                    if (in_valid && in_ready) begin
                        // Latch original operands and operation for setup and post-correction
                        operand_a_reg <= operand_a;
                        operand_b_reg <= operand_b;
                        mul_op_reg    <= mul_op;
                    end
                end
                S_PREP: begin
                    // Initialize registers for a standard signed multiplication.
                    // Correction for unsigned/mixed types will be applied at the end.
                    m_reg         <= operand_a_reg;
                    product_reg   <= {33'b0, operand_b_reg}; // P=0, Q=Multiplier
                    q_minus_1_reg <= 1'b0;
                    count_reg     <= 6'd32;
                end
                S_CALC: begin
                    // Standard signed Booth's algorithm step
                    logic [1:0]  booth_bits = {product_reg[0], q_minus_1_reg};
                    logic [32:0] m_extended = {m_reg[31], m_reg};
                    logic [64:0] temp_prod  = product_reg;

                    case (booth_bits)
                        2'b01: temp_prod[64:32] = $signed(product_reg[64:32]) + $signed(m_extended);
                        2'b10: temp_prod[64:32] = $signed(product_reg[64:32]) - $signed(m_extended);
                        default: ; // Do nothing for 2'b00 and 2'b11
                    endcase
                    
                    q_minus_1_reg <= temp_prod[0];
                    product_reg   <= $signed(temp_prod) >>> 1; // Arithmetic shift right
                    count_reg     <= count_reg - 1'b1;
                end
                S_LATCH_RESULT: begin
                    // Latch the stable combinational result to ensure clean output timing
                    final_result_reg <= comb_result;
                end
                default: ; // S_WAIT_OUTPUT has no datapath changes
            endcase
        end
    end

    // --- Combinational Result Calculation (with correction) ---
    logic [31:0] comb_result;
    always_comb begin
        logic [63:0] corrected_product;
        
        // Start with the result of the signed multiplication
        corrected_product = product_reg[63:0]; 

        // Apply post-multiply correction for unsigned/mixed operations
        // This math converts a signed product to the desired unsigned/mixed product.
        case (mul_op_reg)
            MULHU: begin // Unsigned(A) * Unsigned(B)
                if (operand_a_reg[31]) begin
                    corrected_product = corrected_product + {operand_b_reg, 32'b0};
                end
                if (operand_b_reg[31]) begin
                    corrected_product = corrected_product + {operand_a_reg, 32'b0};
                end
            end
            MULHSU: begin // Signed(A) * Unsigned(B)
                if (operand_b_reg[31]) begin
                    corrected_product = corrected_product - {operand_a_reg, 32'b0};
                end
            end
            default: begin // No correction needed for MUL (signed*signed)
            end
        endcase

        // Select the final 32-bit output based on the operation
        case (mul_op_reg)
            MUL:    comb_result = corrected_product[31:0];
            MULH:   comb_result = corrected_product[63:32];
            MULHSU: comb_result = corrected_product[63:32];
            MULHU:  comb_result = corrected_product[63:32];
            default: comb_result = 32'h0;
        endcase
    end
    
    // --- Control Logic (FSM Next State & Handshakes) ---
    always_comb begin
        next_state  = current_state;
        in_ready    = 1'b0;
        out_valid   = 1'b0;
        
        case (current_state)
            S_IDLE: begin
                in_ready = 1'b1;
                if (in_valid) next_state = S_PREP;
            end
            S_PREP: begin
                next_state = S_CALC;
            end
            S_CALC: begin
                if (count_reg == 6'd1) next_state = S_LATCH_RESULT;
            end
            S_LATCH_RESULT: begin
                next_state = S_WAIT_OUTPUT;
            end
            S_WAIT_OUTPUT: begin
                out_valid = 1'b1;
                if (out_ready) begin
                    next_state = S_IDLE;
                end
            end
            default: next_state = S_IDLE;
        endcase
    end

    // The final output comes from the stable, latched register
    assign out_result = final_result_reg;

endmodule