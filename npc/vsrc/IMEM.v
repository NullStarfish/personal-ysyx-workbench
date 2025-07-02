// IMEM_updated.v
// Fetches instructions and extracts raw fields.
module IMEM (
    input  [31:0] addr,   // Input address from PC
    output [31:0] inst,   // Raw instruction output
    output [4:0]  rd,
    output [4:0]  rs1,
    output [4:0]  rs2,

);
    // Instantiate the ROM to get the instruction data
    rom rom0 (
        .addr(addr),
        .data(inst)
    );
   
    // Extract raw fields from the instruction word.
    // The controller is responsible for interpreting these fields.
    
    assign rd     = inst[11:7];
    
    assign rs1    = inst[19:15];
    assign rs2    = inst[24:20];
    

endmodule
