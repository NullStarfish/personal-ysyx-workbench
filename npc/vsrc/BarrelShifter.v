// BarrelShifter.v
// A 32-bit barrel shifter for SLL, SRL, and SRA operations.

module BarrelShifter (
    input [31:0] data_in,      // The data to be shifted
    input [4:0]  shamt,        // Shift amount (from rs2 or immediate)
    input [1:0]  shift_type,   // 00: SLL, 01: SRL, 10: SRA
    output [31:0] data_out     // The shifted data
);

    // Shift Type Constants
    localparam SLL = 2'b00;
    localparam SRL = 2'b01;
    localparam SRA = 2'b10;

    // Intermediate wires for each stage of the shifter
    wire [31:0] stage1, stage2, stage3, stage4;

    // --- Stage 0: Shift by 16 ---
    // If the 4th bit of shamt is 1, shift left/right by 16 bits.
    wire [31:0] shift16_val;
    assign shift16_val = (shift_type == SLL) ? {data_in[15:0], 16'b0} : // SLL
                         (shift_type == SRA) ? {{16{data_in[31]}}, data_in[31:16]} : // SRA
                                               {16'b0, data_in[31:16]}; // SRL
    assign stage1 = shamt[4] ? shift16_val : data_in;

    // --- Stage 1: Shift by 8 ---
    // If the 3rd bit of shamt is 1, shift left/right by 8 bits.
    wire [31:0] shift8_val;
    assign shift8_val = (shift_type == SLL) ? {stage1[23:0], 8'b0} :
                        (shift_type == SRA) ? {{8{stage1[31]}}, stage1[31:8]} :
                                              {8'b0, stage1[31:8]};
    assign stage2 = shamt[3] ? shift8_val : stage1;

    // --- Stage 2: Shift by 4 ---
    // If the 2nd bit of shamt is 1, shift left/right by 4 bits.
    wire [31:0] shift4_val;
    assign shift4_val = (shift_type == SLL) ? {stage2[27:0], 4'b0} :
                        (shift_type == SRA) ? {{4{stage2[31]}}, stage2[31:4]} :
                                              {4'b0, stage2[31:4]};
    assign stage3 = shamt[2] ? shift4_val : stage2;

    // --- Stage 3: Shift by 2 ---
    // If the 1st bit of shamt is 1, shift left/right by 2 bits.
    wire [31:0] shift2_val;
    assign shift2_val = (shift_type == SLL) ? {stage3[29:0], 2'b0} :
                        (shift_type == SRA) ? {{2{stage3[31]}}, stage3[31:2]} :
                                              {2'b0, stage3[31:2]};
    assign stage4 = shamt[1] ? shift2_val : stage3;

    // --- Stage 4: Shift by 1 ---
    // If the 0th bit of shamt is 1, shift left/right by 1 bit.
    wire [31:0] shift1_val;
    assign shift1_val = (shift_type == SLL) ? {stage4[30:0], 1'b0} :
                        (shift_type == SRA) ? {{1{stage4[31]}}, stage4[31:1]} :
                                              {1'b0, stage4[31:1]};
    assign data_out = shamt[0] ? shift1_val : stage4;

endmodule
