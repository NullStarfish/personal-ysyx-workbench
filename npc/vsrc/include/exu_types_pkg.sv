import cpu_types_pkg::*;

package exu_types_pkg;

    typedef enum logic [2:0] {
        MUL_NONE, // Not a mul op
        MUL,      // Signed x Signed, lower 32 bits
        MULH,     // Signed x Signed, upper 32 bits
        MULHSU,   // Signed (rs1) x Unsigned (rs2), upper 32 bits
        MULHU     // Unsigned x Unsigned, upper 32 bits
    } riscv_mul_op_e;


        // Add to cpu_types_pkg.sv
    typedef enum logic [2:0] {
        DIV_NONE, // Not a division op
        DIV,      // Signed Division
        DIVU,     // Unsigned Division
        REM,      // Signed Remainder
        REMU      // Unsigned Remainder
    } riscv_div_op_e;

    typedef struct packed {
        logic [31:0] dataA;
        logic [31:0] dataB;
        alusel_e     opcode;
    } exu_alu_t;

    typedef struct packed {
        logic [31:0] dataA;
        logic [31:0] dataB;
        riscv_div_op_e     opcode;
    } alu_div_t;

    typedef struct packed {
        logic [31:0] dataA;
        logic [31:0] dataB;
        riscv_mul_op_e   opcode;
    } alu_mul_t;

endpackage