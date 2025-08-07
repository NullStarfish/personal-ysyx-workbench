// Loader.v
// A simple module to control writing to the ROM during program loading.

module Loader (
    input clk,
    input rst,

    // Control signals from C++ testbench
    input load_en,
    input [31:0] load_addr,
    input [31:0] load_data,

    // Outputs to the ROM's write port
    output reg rom_we,
    output reg [31:0] rom_waddr,
    output reg [31:0] rom_wdata
);

    always @(posedge clk) begin
        if (rst) begin
            rom_we <= 1'b0;
            rom_waddr <= 32'b0;
            rom_wdata <= 32'b0;
        end else if (load_en) begin
            // When loading is enabled, pass the address and data through.
            rom_we    <= 1'b1;
            rom_waddr <= load_addr;
            rom_wdata <= load_data;
        end else begin
            // When loading is disabled, de-assert the write enable.
            rom_we <= 1'b0;
        end
    end

endmodule
