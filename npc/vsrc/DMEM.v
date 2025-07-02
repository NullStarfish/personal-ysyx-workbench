module DMEM (
    input clk,               // Clock signal
    input rst,               // Reset signal
    input [31:0] addr,       // Address for memory access
    input [31:0] wdata,      // Data to write to memory
    input we,                // Write enable signal
    output reg [31:0] rdata  // Data read from memory
);

    reg [31:0] mem [0:1023]; // Memory array of 1024 words (32 bits each)

    always @(posedge clk or posedge rst) begin
        if (rst) begin
            rdata <= 32'b0; // Reset read data to zero
        end else if (we) begin
            mem[addr[11:2]] <= wdata; // Write data to memory at the specified address
        end else begin
            rdata <= mem[addr[11:2]]; // Read data from memory at the specified address
        end
    end
endmodule