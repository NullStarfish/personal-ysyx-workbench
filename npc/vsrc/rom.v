// rom.v
// Modified to contain a test program.

module rom (
    input [31:0] addr,
    output [31:0] data //output data
);
    // ROM with 256 entries of 32 bits each [cite: 41]
    reg [31:0] rom_data [0:255]; 

    initial begin
        // Initialize ROM with the test program.
        // Default all memory to NOP (addi x0, x0, 0)
        for (integer i = 0; i < 256; i = i + 1) begin
            rom_data[i] = 32'h00000013; // NOP
        end

        // Test Program
        // Address | Machine Code | Instruction
        rom_data[0]  = 32'hAAAAA2B7; // 0x00: lui x5, 0xAAAAA  (x5 = 0xAAAAA000)
        rom_data[1]  = 32'h55528293; // 0x04: addi x5, x5, 0x555 (x5 = 0xAAAAA555)
        rom_data[2]  = 32'h06400313; // 0x08: addi x6, x0, 100   (x6 = 100, for memory base address)
        rom_data[3]  = 32'h00532623; // 0x0C: sw x5, 12(x6)      (mem[100+12] = x5)
        rom_data[4]  = 32'h00C32383; // 0x10: lw x7, 12(x6)      (x7 = mem[112])
        rom_data[5]  = 32'h00728863; // 0x14: beq x5, x7, +8     (Branch to 0x1C if x5 == x7)
        rom_data[6]  = 32'h00100113; // 0x18: addi x2, x0, 1     (This instruction should be SKIPPED)
        rom_data[7]  = 32'h00200113; // 0x1C: addi x2, x0, 2     (Branch TARGET, x2 should become 2)
        rom_data[8]  = 32'b00000000000100000000000001110011; // ebreak
    end

    // Read data from ROM based on address (word-aligned) [cite: 45]
    assign data = rom_data[addr[31:2]]; 
endmodule