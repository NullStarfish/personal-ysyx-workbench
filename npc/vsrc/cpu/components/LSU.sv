// LSU.sv
// (已修正时序竞争问题)
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

    ex_lsu_t ex_lsu_payload_reg;
    lsu_wb_t lsu_wb_payload_reg;

    wire lsu_in_fire = lsu_in.valid && lsu_in.ready;

    always_ff @(posedge clk) begin
        if (rst) begin
            // [FIX] Reset the valid bit to ensure a clean start
            ex_lsu_payload_reg.valid <= 1'b0;
        end else if (lsu_in_fire) begin
            ex_lsu_payload_reg <= lsu_in.payload;
        end else if (cur_state == S_WAIT_WBU && lsu_out.ready) begin
            // [FIX] Invalidate the instruction after it has been accepted
            ex_lsu_payload_reg.valid <= 1'b0;
        end
    end

    lsu_wb_t lsu_wb_payload_next;
    always_comb begin
        // This calculation is now always based on a stable, registered input
        lsu_wb_payload_next.valid     = ex_lsu_payload_reg.valid;
        lsu_wb_payload_next.reg_wen   = ex_lsu_payload_reg.reg_wen;
        lsu_wb_payload_next.rd_addr   = ex_lsu_payload_reg.rd_addr;
        lsu_wb_payload_next.pc_target = ex_lsu_payload_reg.pc_target;
        lsu_wb_payload_next.wb_data   = ex_lsu_payload_reg.mem_en ? load_out : ex_lsu_payload_reg.exu_result;
    end

    always_ff @(posedge clk) begin
        if (rst) begin
            // [FIX] Reset the valid bit to ensure a clean start
            lsu_wb_payload_reg.valid <= 1'b0;
        end 
        // [MODIFIED] Latch the output register only when the calculation/data is ready
        else if ( (cur_state == S_PROCESSING) || (cur_state == S_BUSY_MEM && d_ready) ) begin
            lsu_wb_payload_reg <= lsu_wb_payload_next;
        end
    end

    //----------------------------------------------------------------
    // Moore FSM Handshake Logic
    //----------------------------------------------------------------
    // [MODIFIED] Added a new state for processing
    typedef enum logic [1:0] {
        S_IDLE, S_PROCESSING, S_BUSY_MEM, S_WAIT_WBU
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
                    // After receiving, always go to a processing state
                    next_state = S_PROCESSING;
                end
            end
            S_PROCESSING: begin // [NEW] State to decide the next step
                // Now, ex_lsu_payload_reg holds stable data
                if (ex_lsu_payload_reg.mem_en) begin
                    next_state = S_BUSY_MEM; // It's a memory op
                end else begin
                    next_state = S_WAIT_WBU;  // It's a pass-through op
                end
            end
            S_BUSY_MEM: begin
                if (d_ready) begin
                    next_state = S_WAIT_WBU;
                end
            end
            S_WAIT_WBU: begin
                if (lsu_out.ready) begin
                    next_state = S_IDLE;
                end
            end
        endcase
    end

    always_comb begin
        lsu_out.payload = lsu_wb_payload_reg;
        lsu_in.ready = 1'b0;
        lsu_out.valid = 1'b0;
        d_valid = 1'b0;
        
        unique case (cur_state)
            S_IDLE: begin
                lsu_in.ready = 1'b1;
            end
            S_BUSY_MEM: begin
                d_valid = 1'b1;
            end
            S_WAIT_WBU: begin
                lsu_out.valid = 1'b1;
            end
            // S_PROCESSING state has no external signals
            default: begin
                // Do nothing, just wait
            end
        endcase
    end
endmodule