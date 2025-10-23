// EXU.sv
// Executes ALU/CSR operations and calculates next PC.
// Integrates a single-cycle CSR module.
import cpu_types_pkg::*;
import exu_types_pkg::*;

module EXU (
    input  logic      clk, rst,
    stage_if.slave    ex_in,
    stage_if.master   ex_out
);
    // Import the ebreak function for DPI-C call
    import "DPI-C" function void ebreak();

    // --- Pipeline Registers ---
    // EXU's input register, latches the payload from IDU
    id_ex_t  id_ex_payload_reg;
    // EXU's output register, latches the result for LSU
    ex_lsu_t ex_lsu_payload_reg;

    wire ex_in_fire = ex_in.valid && ex_in.ready;

    // Latch the input payload from IDU when a valid handshake occurs
    always_ff @(posedge clk) begin
        if (rst) begin
            id_ex_payload_reg.valid <= 1'b0;
        end else if (ex_in_fire) begin
            id_ex_payload_reg <= ex_in.payload;
        end else if (cur_state == S_WAIT_LSU && ex_out.ready) begin
            // Invalidate the instruction after it has been sent downstream
            id_ex_payload_reg.valid <= 1'b0;
        end
    end

    // --- ALU Datapath ---


    stage_if #(exu_alu_t) exu_alu_if();
    stage_if              alu_exu_if();//默认logic [31:0]输出

    assign exu_alu_if.payload.dataA = id_ex_payload_reg.pc_rs1_sel ? id_ex_payload_reg.pc : id_ex_payload_reg.rs1_data;
    assign exu_alu_if.payload.dataB = id_ex_payload_reg.rs2_imm_sel ? id_ex_payload_reg.imm : id_ex_payload_reg.rs2_data;
    assign exu_alu_if.payload.opcode = id_ex_payload_reg.alu_opcode;

    // logic [31:0] alu_in_a, alu_in_b;
    // logic [31:0] alu_exu_if.payload;
    // logic        alu_ready;
    // logic        alu_valid; // Controlled by the FSM

    // assign alu_in_a = id_ex_payload_reg.pc_rs1_sel ? id_ex_payload_reg.pc : id_ex_payload_reg.rs1_data;
    // assign alu_in_b = id_ex_payload_reg.rs2_imm_sel ? id_ex_payload_reg.imm : id_ex_payload_reg.rs2_data;





    ALU u_alu (
        .clk     (clk),
        .rst     (rst),
        .alu_in  (exu_alu_if.slave),
        .alu_out (alu_exu_if.master)
    );

    // --- CSR Datapath ---
    logic [31:0] csr_rdata;
    logic [31:0] ecall_target_pc;
    logic [31:0] mret_target_pc;

    // Instantiate the CSR module
    CSR u_csr (
        .clk        (clk),
        .rst        (rst),

        .wen        (id_ex_payload_reg.csr_op != CSR_NONE && is_first_busy_cycle), // Write if it's any CSR op
        .op         (id_ex_payload_reg.csr_op),
        .waddr      (id_ex_payload_reg.csr_addr),
        .wdata      (id_ex_payload_reg.rs1_or_imm),

        .raddr      (id_ex_payload_reg.csr_addr),
        .rdata      (csr_rdata),

        // Action Ports
        .is_ecall   (id_ex_payload_reg.is_ecall && is_first_busy_cycle),
        .is_mret    (id_ex_payload_reg.is_mret && is_first_busy_cycle),
        .inst_pc    (id_ex_payload_reg.pc),

        .ecall_a5_val(id_ex_payload_reg.a5_data),

        // PC Redirection Outputs
        .ecall_target (ecall_target_pc),
        .mret_target  (mret_target_pc)
    );

    logic [31:0] pc_plus_4;
    assign pc_plus_4 = id_ex_payload_reg.pc + 32'd4;
    // --- Output Payload Calculation (Combinational Logic) ---
    ex_lsu_t ex_lsu_payload_next;
    always_comb begin
        // The valid bit is passed through from the latched input
        ex_lsu_payload_next.valid = id_ex_payload_reg.valid;

        // 1. Calculate PC Target
        
        if (id_ex_payload_reg.is_ecall) begin
            ex_lsu_payload_next.pc_target = ecall_target_pc;
        end else if (id_ex_payload_reg.is_mret) begin
            ex_lsu_payload_next.pc_target = mret_target_pc;
        end else if (id_ex_payload_reg.pc_redirect) begin
            ex_lsu_payload_next.pc_target = alu_exu_if.payload; // For branch/JAL/JALR
        end else begin
            ex_lsu_payload_next.pc_target = pc_plus_4;  // Default sequential execution
        end

        // 2. Select Register Write-Back Data (exu_result)
        if (id_ex_payload_reg.forjal) begin
            ex_lsu_payload_next.exu_result = pc_plus_4;
        end else if (id_ex_payload_reg.csr_op != CSR_NONE) begin
            ex_lsu_payload_next.exu_result = csr_rdata; // CSR instructions write back the old value
        end else begin
            ex_lsu_payload_next.exu_result = alu_exu_if.payload;
        end
        
        // 3. Pass-through other control and data signals to LSU
        ex_lsu_payload_next.reg_wen   = id_ex_payload_reg.reg_wen;
        ex_lsu_payload_next.rd_addr   = id_ex_payload_reg.rd_addr;
        ex_lsu_payload_next.mem_en    = id_ex_payload_reg.mem_en;
        ex_lsu_payload_next.mem_wen   = id_ex_payload_reg.mem_wen;
        ex_lsu_payload_next.mem_wdata = id_ex_payload_reg.rs2_data;
        ex_lsu_payload_next.mem_addr  = alu_exu_if.payload;
        ex_lsu_payload_next.funct3    = id_ex_payload_reg.funct3;
    end

    // Latch the calculated output payload when the ALU/CSR operation is complete
    always_ff @(posedge clk) begin
        if (rst) begin
            ex_lsu_payload_reg.valid <= 1'b0;
        end else if (cur_state == S_CALC && next_state == S_WAIT_LSU) begin
            ex_lsu_payload_reg <= ex_lsu_payload_next;
        end
    end

    // --- Action Logic (EBREAK) ---
    // This is a side effect and should be in a clocked block
    always_ff @(posedge clk) begin
        if (!rst && id_ex_payload_reg.valid && id_ex_payload_reg.is_ebreak) begin
            ebreak();
        end
    end
    
    //----------------------------------------------------------------
    // Moore FSM Handshake Logic (remains the same)
    //----------------------------------------------------------------
    typedef enum logic [1:0] { S_IDLE, S_WAIT_ALU, S_CALC, S_WAIT_LSU } ex_state_e;
    ex_state_e cur_state, next_state;

    always_ff @(posedge clk) begin
        if (rst) begin cur_state <= S_IDLE; end
        else     begin cur_state <= next_state; end
    end


    logic is_first_busy_cycle;
    assign is_first_busy_cycle = (cur_state == S_WAIT_ALU) && (prev_state == S_IDLE);

    ex_state_e prev_state;
    always_ff @(posedge clk) begin
        if (rst) begin
            prev_state <= S_IDLE;
        end else begin
            prev_state <= cur_state;
        end
    end



    always_comb begin
        next_state = cur_state;
        unique case (cur_state)
            S_IDLE:     if (ex_in.valid)  begin 
                    next_state = S_WAIT_ALU;     
            end
            S_WAIT_ALU: if (exu_alu_if.fire) next_state = S_CALC;
            S_CALC:     if (alu_exu_if.fire) next_state = S_WAIT_LSU;
            S_WAIT_LSU: if (ex_out.ready) begin next_state = S_IDLE;      end
        endcase
    end
    
    always_comb begin
        ex_out.payload = ex_lsu_payload_reg;
        ex_in.ready  = 1'b0;
        ex_out.valid = 1'b0;
        exu_alu_if.valid    = 1'b0;
        alu_exu_if.ready    = 1'b0;

        unique case (cur_state)
            S_IDLE:     ex_in.ready = 1'b1;
            S_WAIT_ALU:     exu_alu_if.valid = 1'b1;
            S_CALC:         alu_exu_if.ready = 1'b1;
            S_WAIT_LSU: ex_out.valid  = 1'b1;
        endcase
    end
    
endmodule