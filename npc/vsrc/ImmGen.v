// ImmGen.v - Final Corrected Version
`include "Opcodes.v"

module ImmGen (
    input  [31:0] inst_in,    // 接收完整的32位指令
    input  [2:0]  ImmSel,     // 立即数类型选择
    output reg [31:0] imm_out // 输出32位立即数
);

    always @(*) begin
        // 使用一个特殊值以便于调试
        imm_out = 32'hdeadbeef; 

        case (ImmSel)
            `IMM_U: // U-type: lui, auipc
                // { inst[31:12], 12'b0 }
                imm_out = {inst_in[31:12], 12'b0};

            `IMM_I: // I-type: jalr, load, addi, etc.
                // SignExtend( inst[31:20] )
                imm_out = {{20{inst_in[31]}}, inst_in[31:20]};

            `IMM_S: // S-type: store
                // SignExtend( {inst[31:25], inst[11:7]} )
                imm_out = {{20{inst_in[31]}}, inst_in[31:25], inst_in[11:7]};

            `IMM_B: // B-type: branch
                // SignExtend( {inst[31], inst[7], inst[30:25], inst[11:8], 1'b0} )
                imm_out = {{20{inst_in[31]}}, inst_in[7], inst_in[30:25], inst_in[11:8], 1'b0};

            `IMM_J: // J-type: jal
                // SignExtend( {inst[31], inst[19:12], inst[20], inst[30:21], 1'b0} )
                imm_out = {{12{inst_in[31]}}, inst_in[19:12], inst_in[20], inst_in[30:21], 1'b0};

            default: imm_out = 32'hdeadbeef;
        endcase
    end

endmodule
