// ImmGen.sv
import cpu_types_pkg::*;
// MODIFIED: Removed the redundant `include directive.

module ImmGen (
    input  logic [31:0] inst_in,
    input  immsel_e     ImmSel,
    output logic [31:0] imm_out
);

    always_comb begin
        imm_out = 32'hdeadbeef; // Default for debugging

        unique case (ImmSel)
            IMM_U: imm_out = {inst_in[31:12], 12'b0};
            IMM_I: imm_out = {{20{inst_in[31]}}, inst_in[31:20]};
            IMM_S: imm_out = {{20{inst_in[31]}}, inst_in[31:25], inst_in[11:7]};
            IMM_B: imm_out = {{20{inst_in[31]}}, inst_in[7], inst_in[30:25], inst_in[11:8], 1'b0};
            IMM_J: imm_out = {{12{inst_in[31]}}, inst_in[19:12], inst_in[20], inst_in[30:21], 1'b0};
            default: imm_out = 32'hdeadbeef;
        endcase
    end

endmodule
