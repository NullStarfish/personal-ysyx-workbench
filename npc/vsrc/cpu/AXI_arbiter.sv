// AXI_Pipelined_Bridge_Arbiter_Unified.sv
// The ultimate concise version, as specified by the user.
// A single AXI4_Lite typed variable, `pipe_reg`, holds the entire state
// of the pipeline bridge, perfectly capturing the concept of a transaction snapshot.

import cpu_types_pkg::*;

module AXI_Pipelined_Bridge_Arbiter (
    input logic clk,
    input logic rst,

    // Slave interfaces facing the masters (IFU, LSU)
    AXI4_Lite.slave slave0_if,
    AXI4_Lite.slave slave1_if,

    // Master interface facing the slave (SRAM)
    AXI4_Lite.master master_if
);

    // --- The SINGLE AXI4_Lite variable holding the entire pipeline state ---
    // It stores the latched request AND the latched response.
    AXI4_Lite pipe_reg();

    // --- Arbitration FSM and State ---
    typedef enum { S_IDLE, S_GRANT_S0, S_GRANT_S1 } state_e;
    state_e cur_state, next_state;
    logic lru_grant;
    logic granted_is_read_reg;

    // Fire signals based on master's interaction with our registered response
    wire s0_r_fire = pipe_reg.rvalid && slave0_if.rready && (cur_state == S_GRANT_S0);
    wire s0_b_fire = pipe_reg.bvalid && slave0_if.bready && (cur_state == S_GRANT_S0);
    wire s1_r_fire = pipe_reg.rvalid && slave1_if.rready && (cur_state == S_GRANT_S1);
    wire s1_b_fire = pipe_reg.bvalid && slave1_if.bready && (cur_state == S_GRANT_S1);

    // FSM Logic (unchanged)
    always_ff @(posedge clk) begin
        if (rst) begin cur_state <= S_IDLE; lru_grant <= 1'b1; end
        else begin
            cur_state <= next_state;
            if (cur_state == S_IDLE && next_state != S_IDLE)
                lru_grant <= (next_state == S_GRANT_S1);
        end
    end

    always_comb begin
        next_state = cur_state;
        unique case (cur_state)
            S_IDLE:
                if (lru_grant) begin
                    if (slave0_if.arvalid || slave0_if.awvalid) next_state = S_GRANT_S0;
                    else if (slave1_if.arvalid || slave1_if.awvalid) next_state = S_GRANT_S1;
                end else begin
                    if (slave1_if.arvalid || slave1_if.awvalid) next_state = S_GRANT_S1;
                    else if (slave0_if.arvalid || slave0_if.awvalid) next_state = S_GRANT_S0;
                end
            S_GRANT_S0:
                if ((granted_is_read_reg && s0_r_fire) || (!granted_is_read_reg && s0_b_fire))
                    next_state = S_IDLE;
            S_GRANT_S1:
                if ((granted_is_read_reg && s1_r_fire) || (!granted_is_read_reg && s1_b_fire))
                    next_state = S_IDLE;
        endcase
    end

    // --- Latching Logic: Populating the single pipe_reg ---
    always_ff @(posedge clk) begin
        // --- Latch the FORWARD path signals ONCE on grant ---
        if (cur_state == S_IDLE && next_state != S_IDLE) begin
            if (next_state == S_GRANT_S0) begin
                granted_is_read_reg <= slave0_if.arvalid;
                // Snapshot the request part of the interface
                {pipe_reg.araddr, pipe_reg.arvalid, pipe_reg.awaddr, pipe_reg.awvalid,
                 pipe_reg.wdata, pipe_reg.wstrb, pipe_reg.wvalid} =
                {slave0_if.araddr, slave0_if.arvalid, slave0_if.awaddr, slave0_if.awvalid,
                 slave0_if.wdata, slave0_if.wstrb, slave0_if.wvalid};
            end else begin // S_GRANT_S1
                granted_is_read_reg <= slave1_if.arvalid;
                {pipe_reg.araddr, pipe_reg.arvalid, pipe_reg.awaddr, pipe_reg.awvalid,
                 pipe_reg.wdata, pipe_reg.wstrb, pipe_reg.wvalid} =
                {slave1_if.araddr, slave1_if.arvalid, slave1_if.awaddr, slave1_if.awvalid,
                 slave1_if.wdata, slave1_if.wstrb, slave1_if.wvalid};
            end
        end

        // --- Latch the master's readiness CONTINUOUSLY during grant ---
        if (cur_state == S_GRANT_S0) begin
            pipe_reg.rready <= slave0_if.rready;
            pipe_reg.bready <= slave0_if.bready;
        end else if (cur_state == S_GRANT_S1) begin
            pipe_reg.rready <= slave1_if.rready;
            pipe_reg.bready <= slave1_if.bready;
        end

        // --- Latch the BACKWARD path signals CONTINUOUSLY ---
        {pipe_reg.rdata, pipe_reg.rresp, pipe_reg.rvalid, pipe_reg.bresp, pipe_reg.bvalid,
         pipe_reg.arready, pipe_reg.awready, pipe_reg.wready} =
        {master_if.rdata, master_if.rresp, master_if.rvalid, master_if.bresp, master_if.bvalid,
         master_if.arready, master_if.awready, master_if.wready};
    end

    // --- Combinational Output Logic ---
    always_comb begin
        // --- Drive Master Port (to SRAM) from pipe_reg ---
        {master_if.araddr, master_if.awaddr, master_if.wdata, master_if.wstrb} =
        {pipe_reg.araddr, pipe_reg.awaddr, pipe_reg.wdata, pipe_reg.wstrb};

        // Gate valid signals by grant state
        master_if.arvalid = (cur_state != S_IDLE) && pipe_reg.arvalid;
        master_if.awvalid = (cur_state != S_IDLE) && pipe_reg.awvalid;
        master_if.wvalid  = (cur_state != S_IDLE) && pipe_reg.wvalid;
        master_if.rready  = pipe_reg.rready;
        master_if.bready  = pipe_reg.bready;

        // --- Drive Slave Ports (to IFU/LSU) from pipe_reg ---
        {slave0_if.rdata, slave0_if.rresp, slave0_if.bresp,
         slave1_if.rdata, slave1_if.rresp, slave1_if.bresp} =
        {pipe_reg.rdata, pipe_reg.rresp, pipe_reg.bresp,
         pipe_reg.rdata, pipe_reg.rresp, pipe_reg.bresp};

        // Drive readiness back to masters
        slave0_if.arready = (cur_state == S_IDLE) || ((cur_state == S_GRANT_S0) && pipe_reg.arready);
        slave0_if.awready = (cur_state == S_IDLE) || ((cur_state == S_GRANT_S0) && pipe_reg.awready);
        slave0_if.wready  = (cur_state == S_IDLE) || ((cur_state == S_GRANT_S0) && pipe_reg.wready);
        slave1_if.arready = (cur_state == S_IDLE) || ((cur_state == S_GRANT_S1) && pipe_reg.arready);
        slave1_if.awready = (cur_state == S_IDLE) || ((cur_state == S_GRANT_S1) && pipe_reg.awready);
        slave1_if.wready  = (cur_state == S_IDLE) || ((cur_state == S_GRANT_S1) && pipe_reg.wready);

        // Route valid signals from registered response to the correct granted master
        slave0_if.rvalid = (cur_state == S_GRANT_S0) && pipe_reg.rvalid;
        slave0_if.bvalid = (cur_state == S_GRANT_S0) && pipe_reg.bvalid;
        slave1_if.rvalid = (cur_state == S_GRANT_S1) && pipe_reg.rvalid;
        slave1_if.bvalid = (cur_state == S_GRANT_S1) && pipe_reg.bvalid;
    end

endmodule