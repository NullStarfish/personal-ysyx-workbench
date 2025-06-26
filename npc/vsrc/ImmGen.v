module ImmGen (
    input [24:0] imm_in, // 25 bits immediate value
    input [2:0] ImmSel.
    output [31:0] imm_out // 32 bits immediate value output
)
