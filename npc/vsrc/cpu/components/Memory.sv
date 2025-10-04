
// Memory.v
// A unified memory module for both instruction fetch and data access.
// This module is purely combinational and uses DPI-C.

module Memory (
    // Instruction Fetch Port
    input logic clk,
    input logic rst,
    input  logic [31:0] i_addr,
    input logic i_addr_valid,
    output logic [31:0] i_rdata,
    output logic i_rdata_valid,

    // Data Access Port
    input  logic [31:0] d_addr,
    input  logic [7:0] wmask, // Write mask for byte-level writes
    output logic [31:0] d_rdata,

    input  logic d_wen,
    input  logic [31:0] d_wdata,
    input logic d_valid,
    output logic d_ready
    // Note: For simplicity, we assume word-aligned writes for now.
    // wmask can be added later if needed.
);

    // Import the C functions that will perform the actual memory operations.
    import "DPI-C" function int pmem_read(input int raddr);
    import "DPI-C" function void pmem_write(input int waddr, input int wdata, input byte wmask);

    // Instruction Read: Always reads from the address provided by the PC.
    // This is a combinational read.

    always_ff @(posedge clk) begin
        if (rst) begin
            i_rdata <= 32'b0;
            i_rdata_valid <= 1'b0;
        end else if (i_addr_valid) begin//valid由ifu控制，接受到之后ifu自会把valid拉低
            i_rdata <= pmem_read(i_addr);
            i_rdata_valid <= 1'b1; // Indicate that instruction data is valid
        end else begin
            i_rdata_valid <= 1'b0; // No valid instruction data
        end
    end

    // Data Read: Always reads from the address provided by the ALU.
    // This is also a combinational read.
    always_ff @(posedge clk) begin
        if (rst) begin
            d_rdata <= 32'b0;
            d_ready <= 1'b0;
        end else if (d_valid) begin//valid由lsu控制，接受到之后lsu自会把valid拉低
            d_rdata <= pmem_read(d_addr);
            if (d_wen) begin
                pmem_write(d_addr, d_wdata, wmask);
            end
            d_ready <= 1'b1; // Indicate that data is ready
        end else begin
            d_ready <= 1'b0;
        end
    end


endmodule
