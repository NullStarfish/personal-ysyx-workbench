// LOAD_Decoder.sv
// Decodes data read from memory for LW, LH, LB, LHU, LBU instructions.

module LOAD_Decoder (
    input  logic [31:0] raw_addr,
    input  logic [31:0] raw_data,
    input  logic [2:0]  funct3,
    output logic [31:0] out
);
    logic [1:0] addr_offset = raw_addr[1:0];
    logic [4:0] bit_offset  = {addr_offset, 3'b000};
    logic [31:0] data;

    always_comb begin
        // --- Defaults to prevent latches ---
        
        out  = 32'hdeadbeef;

        unique case (funct3)
            `FUNCT3_LB: begin // LB (Load Byte, sign-extended)
                data = (raw_data >> bit_offset) & 32'h000000FF;
                out  = {{24{data[7]}}, data[7:0]};
            end
            `FUNCT3_LH: begin // LH (Load Halfword, sign-extended)
                data = (raw_data >> bit_offset) & 32'h0000FFFF;
                out  = {{16{data[15]}}, data[15:0]};
            end
            `FUNCT3_LW: begin // LW (Load Word)
                data = raw_data; // Assign to data to prevent latch
                out  = raw_data;
            end
            `FUNCT3_LBU: begin // LBU (Load Byte, zero-extended)
                data = (raw_data >> bit_offset) & 32'h000000FF;
                out  = {24'b0, data[7:0]};
            end
            `FUNCT3_LHU: begin // LHU (Load Halfword, zero-extended)
                data = (raw_data >> bit_offset) & 32'h0000FFFF;
                out  = {16'b0, data[15:0]};
            end
            // MODIFIED: Added default case to prevent latches for both 'data' and 'out'.
            default: begin
                data = 32'hdeadbeef;
                out  = 32'hdeadbeef;
            end
        endcase
        
    end
endmodule

