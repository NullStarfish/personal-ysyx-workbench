// ALUDataIn.v
// 使用 always 块重写

module ALUDataIn (
    input [31:0] RegRs1,
    input [31:0] RegRs2,
    input [31:0] Imm,
    input [31:0] PC,
    input Asel,
    input Bsel,
    output reg [31:0] A,
    output reg [31:0] B
);

    // 为 ALU 操作数 A 选择输入
    always @(*) begin
        if (Asel) begin
            A = PC;
        end else begin
            A = RegRs1;
        end
    end

    // 为 ALU 操作数 B 选择输入
    always @(*) begin
        if (Bsel) begin
            B = Imm;
        end else begin
            B = RegRs2;
        end
    end
    
endmodule
