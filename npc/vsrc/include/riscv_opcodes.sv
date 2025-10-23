// riscv_opcodes.svh
// Header file for RISC-V preprocessor macro definitions.
// This file is intended to be included (`include) by modules that need these constants.

`ifndef RISCV_OPCODES_SVH
`define RISCV_OPCODES_SVH

// --- RISC-V Opcodes ---
`define OPCODE_LUI        7'b0110111
`define OPCODE_AUIPC      7'b0010111
`define OPCODE_JAL        7'b1101111
`define OPCODE_JALR       7'b1100111
`define OPCODE_BRANCH     7'b1100011
`define OPCODE_LOAD       7'b0000011
`define OPCODE_STORE      7'b0100011
`define OPCODE_I_TYPE     7'b0010011
`define OPCODE_R_TYPE     7'b0110011
`define OPCODE_I_TYPE_SYS 7'b1110011


// --- Funct3 Constants ---
`define FUNCT3_BEQ        3'b000
`define FUNCT3_BNE        3'b001
`define FUNCT3_BLT        3'b100
`define FUNCT3_BGE        3'b101
`define FUNCT3_BLTU       3'b110
`define FUNCT3_BGEU       3'b111

// --- I-type and R-type Funct3 Constants ---
`define FUNCT3_ADDI_ADD   3'b000
`define FUNCT3_SLLI_SLL   3'b001
`define FUNCT3_SLTI_SLT   3'b010
`define FUNCT3_SLTIU_SLTU 3'b011
`define FUNCT3_XORI_XOR   3'b100
`define FUNCT3_SRLI_SRAI  3'b101
`define FUNCT3_ORI_OR     3'b110
`define FUNCT3_ANDI_AND   3'b111


// ---- M extension ----
`define FUNCT3_MUL      3'h0
`define FUNCT3_MULH     3'h1
`define FUNCT3_MULHSU   3'h2
`define FUNCT3_MULHU    3'h3
`define FUNCT3_DIV      3'h4
`define FUNCT3_DIVU     3'h5
`define FUNCT3_REM      3'h6
`define FUNCT3_REMU     3'h7


// --- SYStem Instructions Funct3 Constants ---
`define FUNCT3_CSRRW     3'b001
`define FUNCT3_CSRRS     3'b010
`define FUNCT3_CSRRC     3'b011
`define FUNCT3_CSRRWI    3'b101
`define FUNCT3_CSRRSI    3'b110
`define FUNCT3_CSRRCI    3'b111




// --- Load and Store Funct3 Constants ---
`define FUNCT3_LW         3'b010
`define FUNCT3_LH         3'b001
`define FUNCT3_LB         3'b000
`define FUNCT3_LHU        3'b101
`define FUNCT3_LBU        3'b100
`define FUNCT3_SW         3'b010
`define FUNCT3_SH         3'b001
`define FUNCT3_SB         3'b000

`endif // RISCV_OPCODES_SVH
