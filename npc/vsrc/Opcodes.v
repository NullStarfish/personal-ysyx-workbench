// Opcodes.v
// RISC-V 指令集和控制信号的常量定义

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

// --- Funct3 for Branch, I-type, R-type ---
`define FUNCT3_BEQ        3'b000
`define FUNCT3_BNE        3'b001
`define FUNCT3_BLT        3'b100
`define FUNCT3_BGE        3'b101
`define FUNCT3_BLTU       3'b110
`define FUNCT3_BGEU       3'b111
`define FUNCT3_ADDI_ADD   3'b000
`define FUNCT3_SLLI_SLL   3'b001
`define FUNCT3_SLTI_SLT   3'b010
`define FUNCT3_SLTIU_SLTU 3'b011
`define FUNCT3_XORI_XOR   3'b100
`define FUNCT3_SRLI_SRAI  3'b101
`define FUNCT3_ORI_OR     3'b110
`define FUNCT3_ANDI_AND   3'b111
`define FUNCT3_LW         3'b010
`define FUNCT3_LH         3'b001
`define FUNCT3_LB         3'b000
`define FUNCT3_LHU        3'b101
`define FUNCT3_LBU        3'b100
`define FUNCT3_SW         3'b010
`define FUNCT3_SH         3'b001
`define FUNCT3_SB         3'b000

// --- Immediate Type Selections (for ImmSel) ---
`define IMM_U             3'b000
`define IMM_I             3'b001
`define IMM_S             3'b010
`define IMM_B             3'b011
`define IMM_J             3'b100

// --- ALU Operation Selections (for ALUSel) ---
`define ALU_ADD           4'h0
`define ALU_SUB           4'h1
`define ALU_AND           4'h2
`define ALU_OR            4'h3
`define ALU_XOR           4'h4
`define ALU_SLT           4'h5
`define ALU_SLTU          4'h6
`define ALU_SLL           4'b1000
`define ALU_SRL           4'b1001
`define ALU_SRA           4'b1010

// --- Write-Back Selections (for WBSel) ---
`define WB_MEM            2'b00
`define WB_ALU            2'b01
`define WB_PC4            2'b10
