// WBMux.v
// 使用 always case 语句重写

module WBMux (
    input [31:0] ALUResult, // Result from the ALU
    input [31:0] DMEMData,  // Data read from memory
    input [31:0] PCPlus4,   // PC + 4 value
    input [1:0] WBSel,      // Select signal for the multiplexer
    output reg [31:0] WBData    // Output data for write-back stage
);

    always @(*) begin
        // 默认输出 ALU 结果，这是最常见的情况
        WBData = ALUResult; 

        case (WBSel)
            2'b00:  WBData = DMEMData;  // For LW
            2'b01:  WBData = ALUResult; // For R-type, I-type
            2'b10:  WBData = PCPlus4;   // For JAL, JALR
            default: WBData = 32'hdeadbeef; // 非法状态，便于调试
        endcase
    end

endmodule
