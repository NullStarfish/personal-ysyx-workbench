// rom.v

// DO NOT export here at the file scope.

module rom (
    input clk,
    input [31:0] addr,
    output [31:0] data,
    input we,
    input [31:0] waddr,
    input [31:0] wdata
);
    localparam BASE_ADDR = 32'h80000000;
    localparam CAPACITY = 32768; // 128KB

    reg [31:0] rom_data [0:CAPACITY - 1];

    wire [31:0] read_index = (addr - BASE_ADDR) >> 2;
    assign data = (addr >= BASE_ADDR && read_index < CAPACITY) ? rom_data[read_index] : 32'h00000000;

    wire [31:0] write_index = (waddr - BASE_ADDR) >> 2;

    always @(posedge clk) begin
        if (we && (waddr >= BASE_ADDR) && (write_index < CAPACITY)) begin
            rom_data[write_index] <= wdata;
        end
    end

    // ================== CORRECTION ==================
    // The export statement MUST be inside the module scope,
    // along with the function it is exporting.
    export "DPI-C" function pmem_read;

    function automatic int pmem_read(input int read_addr);
        if (read_addr >= BASE_ADDR && ((read_addr - BASE_ADDR) >> 2) < CAPACITY) begin
            return rom_data[(read_addr - BASE_ADDR) >> 2];
        end else begin
            return 0;
        end
    endfunction
    // ===============================================

endmodule