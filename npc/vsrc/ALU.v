// ALU.v
// 集成了桶形移位器

`include "Opcodes.v"

module ALU
    #(parameter WIDTH = 32)   
    (
        input  [WIDTH - 1 : 0] A,        // rs1
        input  [WIDTH - 1 : 0] B,        // rs2 or immediate
        input  [3:0]           ALUSel,   // Renamed from 'Mode' and width changed
        output reg [WIDTH-1:0] Results
    );  

    // --- 实例化功能单元 ---
    wire [WIDTH - 1 : 0] adder_result;
    wire [WIDTH - 1 : 0] shifter_result;
    wire internal_zero, internal_overflow, internal_carry;

    AddAndSub #(.WIDTH(WIDTH)) adder_unit (
        .A(A), .B(B), .Cin(ALUSel == `ALU_SUB), // Cin 为1时执行减法
        .Carry(internal_carry), .Overflow(internal_overflow),
        .Result(adder_result), .zero(internal_zero)
    );
    
    BarrelShifter shifter_unit (
        .data_in(A),
        .shamt(B[4:0]), // 移位量来自 B 的低5位
        .shift_type(ALUSel[1:0]), // 使用 ALUSel 的低两位控制移位类型
        .data_out(shifter_result)
    );
    
    // --- 分支比较逻辑 ---
    wire signed_less_than   = internal_overflow ^ adder_result[WIDTH - 1];
    wire unsigned_less_than = !internal_carry;

    // --- ALU 最终结果选择 ---
    always@(*) begin
        case(ALUSel)
            `ALU_ADD:  Results = adder_result;
            `ALU_SUB:  Results = adder_result;
            `ALU_AND:  Results = A & B;
            `ALU_OR:   Results = A | B;
            `ALU_XOR:  Results = A ^ B;
            `ALU_SLT:  Results = {{31{1'b0}}, signed_less_than};
            `ALU_SLTU: Results = {{31{1'b0}}, unsigned_less_than};
            `ALU_SLL:  Results = shifter_result;
            `ALU_SRL:  Results = shifter_result;
            `ALU_SRA:  Results = shifter_result;
            default:   Results = 32'hdeadbeef;
        endcase
    end
endmodule
