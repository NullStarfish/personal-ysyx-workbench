module ImmGen (
    input  [31:7] inst_in,    // 25 bits immediate value
    input  [2:0]  ImmSel,     // Immediate select
    output [31:0] imm_out     // 32 bits immediate value output
);

    MuxKeyWithDefault #(6, 3, 32) i0 (
        .out(imm_out),        .key(ImmSel),
        .default_out(32'h00000000), // Default value for immediate
        .lut({
            // U-type: imm[31:12] << 12
            {3'b000, {inst_in[31:12], 12'b0}},
            // I-type: sign-extend inst_in[31:20]
            {3'b001, {{20{inst_in[31]}}, inst_in[31:20]}},
            // S-type: sign-extend {inst_in[31:25], inst_in[11:7]}
            {3'b010, {{20{inst_in[31]}}, inst_in[31:25], inst_in[11:7]}},
            // B-type: sign-extend {inst_in[31], inst_in[7], inst_in[30:25], inst_in[11:8], 1'b0}
            {3'b011, {{20{inst_in[31]}}, inst_in[7], inst_in[30:25], inst_in[11:8], 1'b0}},
            // J-type: sign-extend {inst_in[31], inst_in[19:12], inst_in[20], inst_in[30:21], 1'b0}
            {3'b100, {{12{inst_in[31]}}, inst_in[19:12], inst_in[20], inst_in[30:21], 1'b0}},
            // Zimm-type: zero-extend inst_in[19:15]
            {3'b101, {27'b0, inst_in[19:15]}}
        })
    );

endmodule
