// WBMux.v
// Write-Back Mux
// Changes:
// 1. Corrected comments to match the hardware logic.

module WBMux (
    input [31:0] ALUResult, // Result from the ALU
    input [31:0] DMEMData,  // Data read from memory
    input [31:0] PCPlus4,   // PC + 4 value
    input [1:0] WBSel,      // Select signal for the multiplexer
    output [31:0] WBData    // Output data for write-back stage
);
    MuxKeyWithDefault # (3, 2, 32) i0 (
        .out(WBData),
        .key(WBSel),
        .default_out(32'h00000000), // Default value for write-back data
        .lut({
            // Note: The comments now correctly describe the hardware behavior.
            {2'b00, DMEMData},  // WBSel = 00, WBData = DMEMData (for LW)
            {2'b01, ALUResult}, // WBSel = 01, WBData = ALUResult (for R-type, I-type)
            {2'b10, PCPlus4}    // WBSel = 10, WBData = PC + 4 (for JAL, JALR)
        })
    );
endmodule
