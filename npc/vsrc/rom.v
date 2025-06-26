module rom (
    input [31:0] addr,
    output [31:0] data //output data
);
    reg [31:0] rom_data [0:255]; //ROM with 256 entries of 32 bits each

    initial begin
        // Initialize ROM with some values (example)
        rom_data[0] = 32'h00000000; // NOP
        rom_data[1] = 32'h00000001; // Example instruction
        rom_data[2] = 32'h00000002; // Example instruction
        // Add more instructions as needed
    end

    assign data = rom_data[addr[31:2]]; // Read data from ROM based on address (word-aligned)
endmodule           