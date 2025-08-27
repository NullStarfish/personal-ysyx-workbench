module STORE_Decoder (////如果store到不对齐的内存，有很大风险，这里先假定对齐
    input [31:0] raw_addr,
    input [2:0] funct3,
    output reg [7:0] wmask
);
    always @(*) begin
        case (funct3)
            3'b000: begin // SB
                wmask = 8'b00000001 << raw_addr[1:0]; // 根据地址最低两位选择字节
            end
            3'b001: begin // SH

                wmask = 8'b00000011 << raw_addr[1]; // 根据地址最低两位选择半字
            end
            3'b010: begin // SW

                wmask = 8'b00001111;
            end
            default: begin // Undefined funct3
                wmask = 8'b00000000;
            end
        endcase
    end
endmodule