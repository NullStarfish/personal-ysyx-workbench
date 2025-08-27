// Memory.v
// A unified memory module for both instruction fetch and data access.
// This module is purely combinational and uses DPI-C.

module Memory (
    // Instruction Fetch Port
    input  [31:0] i_addr,
    output [31:0] i_rdata,

    // Data Access Port
    input  [31:0] d_addr,
    input  [7:0] wmask, // Write mask for byte-level writes
    output [31:0] d_rdata,

    input  d_wen,
    input  [31:0] d_wdata
    // Note: For simplicity, we assume word-aligned writes for now.
    // wmask can be added later if needed.
);

    // Import the C functions that will perform the actual memory operations.
    import "DPI-C" function int pmem_read(input int raddr);
    import "DPI-C" function void pmem_write(input int waddr, input int wdata, input byte wmask);

    // Instruction Read: Always reads from the address provided by the PC.
    // This is a combinational read.
    assign i_rdata = pmem_read(i_addr);

    // Data Read: Always reads from the address provided by the ALU.
    // This is also a combinational read.
    assign d_rdata = pmem_read(d_addr);

    // Data Write: This block is sensitive to any input change.
    // The pmem_write function is only called when d_wen (Data Write Enable) is high.
    always @(*) begin
        if (d_wen) begin
            pmem_write(d_addr, d_wdata, wmask);
        end
    end

endmodule
