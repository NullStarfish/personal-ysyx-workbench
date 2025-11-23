// Memory.v
// A unified memory module for both instruction fetch and data access.
// Modified to work with Chisel generated Verilog (Flattened Ports).

module SRAM (
    input logic clk,
    input logic rst,

    // --- AXI4-Lite Slave Interface (Flattened) ---
    
    // Write Address Channel
    input  logic [31:0] sram_axi_if_awaddr,
    input  logic        sram_axi_if_awvalid,
    output logic        sram_axi_if_awready,

    // Write Data Channel
    input  logic [31:0] sram_axi_if_wdata,
    input  logic [3:0]  sram_axi_if_wstrb,
    input  logic        sram_axi_if_wvalid,
    output logic        sram_axi_if_wready,

    // Write Response Channel
    output logic [1:0]  sram_axi_if_bresp,
    output logic        sram_axi_if_bvalid,
    input  logic        sram_axi_if_bready,

    // Read Address Channel
    input  logic [31:0] sram_axi_if_araddr,
    input  logic        sram_axi_if_arvalid,
    output logic        sram_axi_if_arready,

    // Read Data Channel
    output logic [31:0] sram_axi_if_rdata,
    output logic [1:0]  sram_axi_if_rresp,
    output logic        sram_axi_if_rvalid,
    input  logic        sram_axi_if_rready,

    // --- Fire Signals (Provided by Chisel Logic) ---
    // Chisel BlackBox pass these down so we don't need to recalc valid && ready
    input  logic        sram_axi_if_aw_fire,
    input  logic        sram_axi_if_w_fire,
    input  logic        sram_axi_if_b_fire,
    input  logic        sram_axi_if_ar_fire,
    input  logic        sram_axi_if_r_fire
);

    // Import the C functions that will perform the actual memory operations.
    import "DPI-C" function int pmem_read(input int raddr);
    import "DPI-C" function void pmem_write(input int waddr, input int wdata, input byte wmask);

    typedef enum logic [1:0] {
        S_IDLE, S_R_WAIT_CPU, S_W_WAIT_CPU, S_B_WAIT_CPU
    } state_e;

    state_e cur_state, next_state;

    logic [31:0] rdata_reg;
    logic [31:0] awaddr_reg;

    always_ff @(posedge clk) begin
        if (rst) begin
            cur_state <= S_IDLE;
            rdata_reg <= 0;
            awaddr_reg <= 0;
        end else 
            cur_state <= next_state;
    end

    // Logic to call DPI functions
    always_ff @(posedge clk) begin
        // Read Operation
        if (cur_state == S_IDLE && next_state == S_R_WAIT_CPU) begin
            rdata_reg <= pmem_read(sram_axi_if_araddr);
        end 

        // Write Operation (Direct)
        if (cur_state == S_IDLE && next_state == S_B_WAIT_CPU) begin
            pmem_write(sram_axi_if_awaddr, sram_axi_if_wdata, {4'b0, sram_axi_if_wstrb});
        end

        // Write Address Latch
        if (cur_state == S_IDLE && next_state == S_W_WAIT_CPU) begin
            awaddr_reg <= sram_axi_if_awaddr;
        end

        // Write Data Arrival (after address)
        if (cur_state == S_W_WAIT_CPU && next_state == S_B_WAIT_CPU) begin
            pmem_write(awaddr_reg, sram_axi_if_wdata, {4'b0, sram_axi_if_wstrb});
        end
    end

    // Next State Logic
    always_comb begin
        next_state = cur_state;
        case (cur_state) 
            S_IDLE: begin
                if (sram_axi_if_ar_fire) begin
                    next_state = S_R_WAIT_CPU;
                end
                else if (sram_axi_if_aw_fire) begin
                    if (sram_axi_if_w_fire)
                        next_state = S_B_WAIT_CPU;
                    else
                        next_state = S_W_WAIT_CPU;
                end
            end

            S_R_WAIT_CPU: begin
                if (sram_axi_if_r_fire) begin
                    next_state = S_IDLE;
                end
            end

            S_W_WAIT_CPU:
                if (sram_axi_if_w_fire)
                    next_state = S_B_WAIT_CPU;
            
            S_B_WAIT_CPU:
                if (sram_axi_if_b_fire)
                    next_state = S_IDLE;

            default: begin end
        endcase
    end

    // Output Logic
    always_comb begin
        // Defaults
        sram_axi_if_awready = 1'b0;
        sram_axi_if_wready  = 1'b0;
        sram_axi_if_bvalid  = 1'b0;
        sram_axi_if_bresp   = 2'b00; // OKAY

        sram_axi_if_arready = 1'b0;
        sram_axi_if_rvalid  = 1'b0;
        sram_axi_if_rdata   = rdata_reg;
        sram_axi_if_rresp   = 2'b00; // OKAY

        unique case (cur_state)
            S_IDLE: begin
                sram_axi_if_arready = 1'b1;
                sram_axi_if_awready = 1'b1;
                sram_axi_if_wready  = 1'b1;
            end
            S_R_WAIT_CPU: begin
                sram_axi_if_rvalid = 1'b1;
            end
            S_W_WAIT_CPU: begin
                sram_axi_if_wready = 1'b1;
            end
            S_B_WAIT_CPU: begin
                sram_axi_if_bvalid = 1'b1;
            end
        endcase
    end

endmodule