// IMEM.v
// Modified to pass write signals to the internal ROM.

module IMEM (
    input  clk,         // Add clk for ROM writes
    input  [31:0] addr,
    output [31:0] inst,
    output [4:0]  rd,
    output [4:0]  rs1,
    output [4:0]  rs2,

    // Ports for writing to the internal ROM
    input rom_we,
    input [31:0] rom_waddr,
    input [31:0] rom_wdata
);
    // Instantiate the ROM, passing through the write port
    rom rom0 (
        .clk(clk),
        .addr(addr),
        .data(inst),
        .we(rom_we),
        .waddr(rom_waddr),
        .wdata(rom_wdata)
    );
   
    assign rd  = inst[11:7];
    assign rs1 = inst[19:15];
    assign rs2 = inst[24:20];
endmodule
