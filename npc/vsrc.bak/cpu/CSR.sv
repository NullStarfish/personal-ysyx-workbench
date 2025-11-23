// CSR.sv
// Final version: Complete, NEMU-compatible M-mode CSRs.
import cpu_types_pkg::*;

module CSR (
    input  logic        clk,
    input  logic        rst,

    // --- General CSR Read/Write Ports (from EXU) ---
    input  logic [11:0] raddr,      // CSR address for reading
    output logic [31:0] rdata,      // Data read from CSR

    input  logic        wen,        // General CSR write enable
    input  csr_op_e     op,         // Type of operation
    input  logic [11:0] waddr,
    input  logic [31:0] wdata,      // This is rs1_data or immediate

    // --- Action Ports (from EXU) ---
    input  logic        is_ecall,
    input  logic        is_mret,
    input  logic [31:0] inst_pc,    // The PC of the instruction causing the action
    input  logic [31:0] ecall_a5_val, // [NEW] The value of register a5 for ecall

    // --- Output for PC redirection ---
    output logic [31:0] ecall_target, // New PC target from mtvec for ecall
    output logic [31:0] mret_target   // New PC target from mepc for mret
);

    // --- Machine-Level CSR Registers ---
    logic [31:0] mstatus /* verilator public */;
    logic [31:0] mcause  /* verilator public */;
    logic [31:0] mepc    /* verilator public */;
    logic [31:0] mtvec   /* verilator public */;

    // --- CSR Address Map ---
    localparam MSTATUS = 12'h300;
    localparam MTVEC   = 12'h305;
    localparam MEPC    = 12'h341;
    localparam MCAUSE  = 12'h342;

    // --- MSTATUS bit fields ---
    localparam MIE  = 3;
    localparam MPIE = 7;
    localparam MPP  = 11;

    // --- CSR State Update Logic (Sequential) ---
    always_ff @(posedge clk) begin
        if (rst) begin
            mstatus <= 32'h1800; // M-mode after reset (according to spec)
            mcause  <= 32'h0;
            mepc    <= 32'h0;
            mtvec   <= 32'h0;
        end else begin
            // --- Action priority: ecall > mret > general write ---
            if (is_ecall) begin
                // Behavior follows NEMU's isa_raise_intr
                mepc          <= inst_pc;
                mcause        <= ecall_a5_val;     // [FIX] Use a5 value for mcause
                mstatus[MPIE] <= mstatus[MIE];
                mstatus[MIE]  <= 1'b0;
                mstatus[MPP+1:MPP] <= 2'b11;   // Assume always from M-mode
            end
            else if (is_mret) begin
                // Behavior follows NEMU's mret logic
                //$display("mret!, mstatus[MPIE] is %d", mstatus[MPIE]);
                mstatus[MIE]       <= mstatus[MPIE];
                mstatus[MPIE]      <= 1'b1;
                mstatus[MPP+1:MPP] <= 2'b00;      // Return to U-mode
            end
            else if (wen) begin // General CSRR* instruction
                logic [31:0] old_val;
                logic [31:0] new_val;

                // Internal combinational read of the old value
                unique case (waddr)
                    MSTATUS: old_val = mstatus;
                    MTVEC:   old_val = mtvec;
                    MEPC:    old_val = mepc;
                    MCAUSE:  old_val = mcause;
                    default: old_val = 32'b0;
                endcase

                // Modify based on the operation type
                unique case (op)
                    CSR_WRITE: new_val = wdata;
                    CSR_SET:   new_val = old_val | wdata;
                    CSR_CLEAR: new_val = old_val & ~wdata;
                    default:   new_val = old_val;
                endcase

                // Write the new value back to the register
                unique case (waddr)
                    MSTATUS: mstatus <= new_val;
                    MTVEC:   mtvec   <= new_val;
                    MEPC:    mepc    <= new_val;
                    MCAUSE:  mcause  <= new_val;
                    default: begin end
                endcase
            end
        end
    end

    // --- Combinational Read and Target Logic ---
    // This part was missing and is now added.
    always_comb begin
        // Default read value is 0 for unimplemented/invalid CSRs
        rdata = 32'b0;
        unique case (raddr)
            MSTATUS: rdata = mstatus;
            MTVEC:   rdata = mtvec;
            MEPC:    rdata = mepc;
            MCAUSE:  rdata = mcause;
            default: begin end
        endcase
    end

    // The target address for an ecall is the value in mtvec
    assign ecall_target = mtvec;
    // The target address for an mret is the value in mepc
    assign mret_target = mepc;

endmodule