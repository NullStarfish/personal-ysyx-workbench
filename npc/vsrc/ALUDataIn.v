module ALUDataIn (
    input [31:0] RegRs1,
    input [31:0] RegRs2,
    input [31:0] Imm,
    input [31:0] PC,
    input Asel,
    input Bsel,
    output [31:0] A,
    output [31:0] B
);
    MuxKey  #(2, 1, 32) i0 (
        .out(A),
        .key(Asel),
        .lut({
            {1'b0, RegRs1}, // Asel = 0, A = RegRs1
            {1'b1, PC}     // Asel = 1, A = PC
        })
    );
    MuxKey #(2, 1, 32) i1 (
        .out(B),
        .key(Bsel),
        .lut({
            {1'b0, RegRs2}, // Bsel = 0, B = RegRs2
            {1'b1, Imm}     // Bsel = 1, B = Imm
        })
    );
    
endmodule