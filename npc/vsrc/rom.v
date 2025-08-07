// rom.v
// Modified to be writable by a loader module.

module rom (
    input clk,          // Add clock for synchronous writes
    input [31:0] addr,
    output [31:0] data,

    // Write port for loading the program
    input we,
    input [31:0] waddr,
    input [31:0] wdata
);
    // The base address where the program is linked to run.
    localparam BASE_ADDR = 32'h80000000;
    localparam CAPACITY = 32768; // 128KB

    reg [31:0] rom_data [0:CAPACITY - 1];

    // Read logic (combinational)
    wire [31:0] read_index = (addr - BASE_ADDR) >> 2;
    assign data = (addr >= BASE_ADDR && read_index < CAPACITY) ? rom_data[read_index] : 32'h00000000;

    // Write logic (synchronous)
    // The memory is written on the positive edge of the clock if 'we' is high.
    wire [31:0] write_index = (waddr - BASE_ADDR) >> 2;
    always @(posedge clk) begin
        if (we && (waddr >= BASE_ADDR) && (write_index < CAPACITY)) begin
            rom_data[write_index] <= wdata;
        end
    end

endmodule
