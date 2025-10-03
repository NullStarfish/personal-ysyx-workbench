// STORE_Decoder.sv

module STORE_Decoder (
    input  logic [31:0] raw_addr,
    input logic [31:0] raw_data,
    // MODIFIED: Reverted from enum to logic vector
    input  logic [2:0]  funct3,
    output logic [7:0]  wmask,
    output logic [31:0] wdata
);

    logic [1:0] addr_offset;
    logic [4:0] bit_offset;
    assign addr_offset = raw_addr[1:0];
    assign bit_offset = {addr_offset, 3'b000}; // 乘8得到位偏移

    always_comb begin
        wmask = 8'b0;
        wdata = 32'b0;
        // MODIFIED: Using defines instead of enum labels
        unique case (funct3)
            `FUNCT3_SB: begin 
                wmask = 8'b00000001 << addr_offset;
                wdata = raw_data << bit_offset;
            end
            `FUNCT3_SH: begin 
                wmask = 8'b00000011 << addr_offset;
                wdata = raw_data << bit_offset;
            end
            `FUNCT3_SW: begin 
                wmask = 8'b00001111;
                wdata = raw_data;
            end
            default:   begin wmask = 8'b0; wdata = 32'b0; end
        endcase
    end
endmodule
