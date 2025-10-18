module BranchComp (
    input [31:0] rs1, // First source register value
    input [31:0] rs2, // Second source register value
    input BrUn, // switch for unsigned comparison
    output BrEq,
    output BrLT
);
    assign BrEq = (rs1 == rs2); // Equality check
    assign BrLT = (BrUn) ? (rs1 < rs2) : ($signed(rs1) < $signed(rs2)); // Less than check with unsigned option
endmodule
    