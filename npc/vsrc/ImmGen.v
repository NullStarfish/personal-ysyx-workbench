// ImmGen.v
// 使用 always case 语句重写

module ImmGen (
    input  [31:7] inst_in,    // 25 bits of instruction
    input  [2:0]  ImmSel,     // Immediate select
    output reg [31:0] imm_out // 32 bits immediate value output
);

    always @(*) begin
        // 默认输出为 0，防止生成锁存器
        imm_out = 32'h00000000; 

        case (ImmSel)
            // U-type: lui, auipc
            3'b000: imm_out = {inst_in[31:12], 12'b0};
            
            // I-type: jalr, load, addi, etc.
            3'b001: imm_out = {{20{inst_in[31]}}, inst_in[31:20]};
            
            // S-type: store
            3'b010: imm_out = {{20{inst_in[31]}}, inst_in[31:25], inst_in[11:7]};
            
            // B-type: branch
            3'b011: imm_out = {{20{inst_in[31]}}, inst_in[7], inst_in[30:25], inst_in[11:8], 1'b0};
            
            // J-type: jal
            3'b100: imm_out = {{12{inst_in[31]}}, inst_in[19:12], inst_in[20], inst_in[30:21], 1'b0};
            
            // Zimm-type (for CSR instructions, if needed)
            3'b101: imm_out = {27'b0, inst_in[19:15]};

            default: imm_out = 32'h00000000;
        endcase
    end

endmodule
