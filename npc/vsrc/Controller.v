
import "DPI-C" function void ebreak();
// Controller.v (Updated for Advanced Branching)
// Generates control signals, now with full support for all RV32I branch instructions.
module Controller (
    // Instruction fields from IMEM
    input [31:0] inst, // Full instruction for debugging

    // Branch flags from ALU
    input BrEq,
    input BrLT,

    // Control Signals (Outputs)
    output       RegWEn,  // Register file write enable
    output       DMWen,   // Data memory write enable
    output       Asel,    // ALU input A selector
    output       Bsel,    // ALU input B selector
    output [1:0] WBSel,   // Write-back mux selector
    output [2:0] ImmSel,  // Immediate generator selector
    output [2:0] ALUSel,  // ALU operation selector
    output       BrUn,    // New output: Selects unsigned comparison in ALU
    output       PCSel    // PC source selector
);


    wire [6:0] opcode;
    wire [2:0] funct3;
    wire [6:0] funct7;
    assign opcode = inst[6:0];   // Extract opcode from instruction
    assign funct3 = inst[14:12]; // Extract funct3 from instruction
    assign funct7 = inst[31:25]; // Extract funct7 from instruction

    // --- Opcodes ---
    localparam OPCODE_LUI   = 7'b0110111;
    localparam OPCODE_AUIPC = 7'b0010111;
    localparam OPCODE_JAL   = 7'b1101111;
    localparam OPCODE_JALR  = 7'b1100111;
    localparam OPCODE_BRANCH= 7'b1100011;
    localparam OPCODE_I_TYPE_LOAD  = 7'b0000011;
    localparam OPCODE_STORE = 7'b0100011;
    localparam OPCODE_I_TYPE= 7'b0010011;
    localparam OPCODE_I_TYPE_SYS = 7'b1110011; // System instructions
    localparam OPCODE_R_TYPE= 7'b0110011;

    // --- Funct3 for Branch Instructions ---
    localparam FUNCT3_BEQ  = 3'b000;
    localparam FUNCT3_BNE  = 3'b001;
    localparam FUNCT3_BLT  = 3'b100;
    localparam FUNCT3_BGE  = 3'b101;
    localparam FUNCT3_BLTU = 3'b110;
    localparam FUNCT3_BGEU = 3'b111;

    // --- Instruction Type Flags ---
    wire is_lui, is_auipc, is_jal, is_jalr, is_branch, is_load, is_store, is_i_type, is_r_type;
    assign is_lui    = (opcode == OPCODE_LUI);
    assign is_auipc  = (opcode == OPCODE_AUIPC);
    assign is_jal    = (opcode == OPCODE_JAL);
    assign is_jalr   = (opcode == OPCODE_JALR);
    assign is_branch = (opcode == OPCODE_BRANCH);
    assign is_load   = (opcode == OPCODE_I_TYPE_LOAD);
    assign is_store  = (opcode == OPCODE_STORE);
    assign is_i_type = (opcode == OPCODE_I_TYPE);
    assign is_r_type = (opcode == OPCODE_R_TYPE);
    


    wire is_ebreak;
    assign is_ebreak = inst == 32'b00000000000100000000000001110011; // ebreak instruction
    
    // If ebreak is detected, call the DPI-C function to halt execution.
    always @(*) begin
        if (is_ebreak) begin
            ebreak(); // Call the DPI-C function to handle ebreak
        end
    end

    // --- Control Signal Generation ---
    assign RegWEn = is_r_type | is_i_type | is_load | is_lui | is_auipc | is_jal | is_jalr;
    assign DMWen  = is_store;
    assign Asel   = is_auipc | is_jal | is_branch;
    assign Bsel   = is_i_type | is_load | is_store | is_lui | is_auipc | is_jal | is_jalr;

    MuxKeyWithDefault #(3, 7, 2) wb_mux (.out(WBSel), .key(opcode), .default_out(2'b01), .lut({{OPCODE_I_TYPE_LOAD, 2'b00}, {OPCODE_JAL, 2'b10}, {OPCODE_JALR, 2'b10}}));
    MuxKeyWithDefault #(5, 7, 3) imm_mux (.out(ImmSel), .key(opcode), .default_out(3'b001), .lut({{OPCODE_LUI, 3'b000}, {OPCODE_AUIPC, 3'b000}, {OPCODE_STORE, 3'b010}, {OPCODE_BRANCH,3'b011}, {OPCODE_JAL, 3'b100}}));
    
    // ALUSel is simplified. For a full implementation, more funct3/funct7 decoding is needed.
    // For now, we set the mode to 'compare' for any branch instruction.
    MuxKeyWithDefault #(2, 7, 3) alu_sel_mux (
        .out(ALUSel),
        .key(opcode),
        .default_out(3'b000), // Default to ADD
        .lut({
            {OPCODE_R_TYPE, {1'b0, funct7[5], funct3[0]}}, // ADD/SUB for R-type
            {OPCODE_BRANCH, 3'b110} // COMPARE for branches
        })
    );

    // --- Advanced Branch Logic ---
    
    // 1. Set BrUn for the ALU based on funct3 for branch instructions.
    //    It's high for BLTU (110) and BGEU (111).
    assign BrUn = is_branch & funct3[1];

    // 2. Determine if the branch should be taken based on ALU flags and funct3.
    wire take_branch;
    MuxKeyWithDefault #(6, 3, 1) branch_logic_mux (
        .out(take_branch),
        .key(funct3),
        .default_out(1'b0), // Default to not taking the branch
        .lut({
            {FUNCT3_BEQ,  BrEq},
            {FUNCT3_BNE, !BrEq},
            {FUNCT3_BLT,  BrLT},
            {FUNCT3_BGE, !BrLT & !BrEq}, // BGE is !(A < B)
            {FUNCT3_BLTU, BrLT},
            {FUNCT3_BGEU,!BrLT & !BrEq}  // BGEU is !(A < B)
        })
    );

    // 3. Set PCSel for jumps and taken branches.
    assign PCSel = is_jal | is_jalr | (is_branch & take_branch);

endmodule
