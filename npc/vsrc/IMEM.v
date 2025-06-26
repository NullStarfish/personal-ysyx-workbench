module IMEM (
    input [31:0] addr, //input address
    output [7:0] opcode,
    output [2:0] funct3,
    output [6:0] funct7,
    output [4:0] rs1,
    output [4:0] rs2,
    output [4:0] rd,
    output [24:0] imm, //immediate value;
    output [31:0] inst //instruction output
);
    rom rom0 (
        .addr(addr),
        .data(inst) // Read instruction from ROM based on address
    );
    
    assign opcode = inst[6:0];
    assign funct3 = inst[14:12];
    assign funct7 = inst[31:25];
    assign rs1 = inst[19:15];
    assign rs2 = inst[24:20];
    assign rd = inst[11:7];
    assign imm = inst[31:7]; // 25 bits immediate value
endmodule