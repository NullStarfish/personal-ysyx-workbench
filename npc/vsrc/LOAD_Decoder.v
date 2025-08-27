module LOAD_Decoder (
    input [31:0] raw_addr,
    input [31:0] raw_data,
    input [2:0] funct3,
    output reg [31:0] out
);
    wire [1:0] addr_offset = raw_addr[1:0];
    wire [4:0] bit_offset = {addr_offset, 3'b000}; // 乘以8得到位偏移
    reg [31:0] data; // 用于存放截取的数据

    always @(*) begin
        data = 32'b0; // 默认赋值，避免 latch
        case (funct3)
            3'b000: begin // LB
                data = (raw_data >> bit_offset) & 32'hFF; // 只取低8位
                out = {{24{data[7]}}, data[7:0]}; // 符号扩展
            end
            3'b001: begin // LH
                data = (raw_data >> bit_offset) & 32'hFFFF; // 只取低16位
                out = {{16{data[15]}}, data[15:0]}; // 符号扩展
            end
            3'b010: begin // LW
                out = raw_data;
            end
            3'b100: begin // LBU
                data = (raw_data >> bit_offset) & 32'hFF; // 只取低8位
                out = {24'b0, data[7:0]}; // 零扩展
            end
            3'b101: begin // LHU
                data = (raw_data >> bit_offset) & 32'hFFFF; // 只取低16位
                out = {16'b0, data[15:0]}; // 零扩展
            end
            default: begin
                out = 32'hdeadbeef;
            end
        endcase
    end
endmodule