/***************************************************************************************
* Copyright (c) 2014-2024 Zihao Yu, Nanjing University
*
* NEMU is licensed under Mulan PSL v2.
* You can use this software according to the terms and conditions of the Mulan PSL v2.
* You may obtain a copy of Mulan PSL v2 at:
*          http://license.coscl.org.cn/MulanPSL2
*
* THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
* EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
* MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
*
* See the Mulan PSL v2 for more details.
***************************************************************************************/

#include "local-include/reg.h"
#include <cpu/cpu.h>
#include <cpu/ifetch.h>
#include <cpu/decode.h>

#define R(i) gpr(i)
#define Mr vaddr_read
#define Mw vaddr_write

enum {
  TYPE_I, TYPE_U, TYPE_S, TYPE_J, TYPE_R, TYPE_B,
  TYPE_N, // none
};

//ATTENTION!  the is the value of regs not idx.
#define src1R() do { *src1 = R(rs1); } while (0)
#define src2R() do { *src2 = R(rs2); } while (0)
#define immI() do { *imm = SEXT(BITS(i, 31, 20), 12); } while(0)
#define immU() do { *imm = SEXT(BITS(i, 31, 12), 20) << 12; } while(0)
#define immS() do { *imm = (SEXT(BITS(i, 31, 25), 7) << 5) | BITS(i, 11, 7); } while(0)
#define immJ() do { \
  *imm = (SEXT(BITS(i, 31, 31), 1) << 20) | \
         (BITS(i, 30, 21) << 1) | \
         (BITS(i, 20, 20) << 11) | \
         (BITS(i, 19, 12) << 12); \
} while(0)
#define immB() do { \
  *imm = (SEXT(BITS(i, 31, 31), 1) << 12) | \
         (BITS(i, 30, 25) << 5) | \
         (BITS(i, 11, 8) << 1) | \
         (BITS(i, 7, 7) << 11); \
} while(0)

static void decode_operand(Decode *s, int *rd, word_t *src1, word_t *src2, word_t *imm, int type) {
  printf("decode_operand: pc = 0x%08x, inst = 0x%08x, type = %d\n", s->pc, s->isa.inst, type);
  uint32_t i = s->isa.inst;
  int rs1 = BITS(i, 19, 15);
  int rs2 = BITS(i, 24, 20);
  *rd     = BITS(i, 11, 7);
  printf("rs1 = %d, rs2 = %d, rd = %d\n", rs1, rs2, *rd);
  switch (type) {
    case TYPE_I: src1R();          immI(); break;
    case TYPE_U:                   immU(); break;
    case TYPE_S: src1R(); src2R(); immS(); break;
    case TYPE_J:                   immJ(); break;
    case TYPE_N: break;
    case TYPE_R: src1R(); src2R(); break;
    case TYPE_B: src1R(); src2R(); immB(); break;
    default: panic("unsupported type = %d", type);
  }
}

static int decode_exec(Decode *s) {
  s->dnpc = s->snpc;

#define INSTPAT_INST(s) ((s)->isa.inst)
#define INSTPAT_MATCH(s, name, type, ... /* execute body */ ) { \
  int rd = 0; \
  word_t src1 = 0, src2 = 0, imm = 0; \
  decode_operand(s, &rd, &src1, &src2, &imm, concat(TYPE_, type)); \
  __VA_ARGS__ ; \
}

  INSTPAT_START(); // Start instruction pattern matching

  // RV32I Base Instruction Set
  // LUI (Load Upper Immediate)
  // LUI places the 20-bit U-immediate value into the destination register rd,
  // filling the lowest 12 bits with zeros.
  INSTPAT("??????? ????? ????? ??? ????? 01101 11", lui    , U, R(rd) = imm);
  // AUIPC (Add Upper Immediate to PC)
  // AUIPC forms a 32-bit offset from the U-immediate, adds this offset to the
  // PC of the AUIPC instruction, and places the result in register rd.
  INSTPAT("??????? ????? ????? ??? ????? 00101 11", auipc  , U, R(rd) = s->pc + imm);

  // Load Instructions
  // LBU (Load Byte Unsigned)
  // Loads an 8-bit value from memory at address (rs1 + imm) and zero-extends it to XLEN bits.
  INSTPAT("??????? ????? ????? 100 ????? 00000 11", lbu    , I, R(rd) = Mr(src1 + imm, 1));
  // LW (Load Word)
  // Loads a 32-bit value from memory at address (rs1 + imm) and sign-extends it to XLEN bits.
  INSTPAT("??????? ????? ????? 010 ????? 00000 11", lw     , I, R(rd) = Mr(src1 + imm, 4));
  // LH (Load Halfword)
  // Loads a 16-bit value from memory at address (rs1 + imm) and sign-extends it to XLEN bits.
  INSTPAT("??????? ????? ????? 001 ????? 00000 11", lh     , I, R(rd) = SEXT(Mr(src1 + imm, 2), 16));
  // LHU (Load Halfword Unsigned)
  // Loads a 16-bit value from memory at address (rs1 + imm) and zero-extends it to XLEN bits.
  INSTPAT("??????? ????? ????? 101 ????? 00000 11", lhu    , I, R(rd) = Mr(src1 + imm, 2));
  // LB (Load Byte)
  // Loads an 8-bit value from memory at address (rs1 + imm) and sign-extends it to XLEN bits.
  INSTPAT("??????? ????? ????? 000 ????? 00000 11", lb     , I, R(rd) = SEXT(Mr(src1 + imm, 1), 8));

  // Store Instructions
  // SB (Store Byte)
  // Stores the lowest 8 bits of rs2 to memory at address (rs1 + imm).
  INSTPAT("??????? ????? ????? 000 ????? 01000 11", sb     , S, Mw(src1 + imm, 1, src2));
  // SW (Store Word)
  // Stores the lowest 32 bits of rs2 to memory at address (rs1 + imm).
  INSTPAT("??????? ????? ????? 010 ????? 01000 11", sw     , S, Mw(src1 + imm, 4, src2));
  // SH (Store Halfword)
  // Stores the lowest 16 bits of rs2 to memory at address (rs1 + imm).
  INSTPAT("??????? ????? ????? 001 ????? 01000 11", sh     , S, Mw(src1 + imm, 2, src2));

  // Arithmetic Instructions (R-type)
  // ADD
  // Adds the values in rs1 and rs2 and stores the result in rd.
  INSTPAT("0000000 ????? ????? 000 ????? 01100 11", add    , R, R(rd) = src1 + src2);
  // SUB
  // Subtracts the value in rs2 from rs1 and stores the result in rd.
  INSTPAT("0100000 ????? ????? 000 ????? 01100 11", sub    , R, R(rd) = src1 - src2);
  // SLL (Shift Left Logical)
  // Shifts src1 left by the amount specified in the lowest 5 bits of src2, zero-extending.
  INSTPAT("0000000 ????? ????? 001 ????? 01100 11", sll    , R, R(rd) = src1 << (src2 & 0x1f));
  // SLT (Set Less Than)
  // Sets rd to 1 if src1 (signed) < src2 (signed), otherwise 0.
  INSTPAT("0000000 ????? ????? 010 ????? 01100 11", slt    , R, R(rd) = (sword_t)src1 < (sword_t)src2);
  // SLTU (Set Less Than Unsigned)
  // Sets rd to 1 if src1 (unsigned) < src2 (unsigned), otherwise 0.
  INSTPAT("0000000 ????? ????? 011 ????? 01100 11", sltu   , R, R(rd) = (uint32_t)src1 < (uint32_t)src2);
  // XOR
  // Performs a bitwise XOR operation on src1 and src2 and stores the result in rd.
  INSTPAT("0000000 ????? ????? 100 ????? 01100 11", xor    , R, R(rd) = src1 ^ src2);
  // SRL (Shift Right Logical)
  // Shifts src1 right by the amount specified in the lowest 5 bits of src2, zero-extending.
  INSTPAT("0000000 ????? ????? 101 ????? 01100 11", srl    , R, R(rd) = src1 >> (src2 & 0x1f));
  // SRA (Shift Right Arithmetic)
  // Shifts src1 right by the amount specified in the lowest 5 bits of src2, sign-extending.
  INSTPAT("0100000 ????? ????? 101 ????? 01100 11", sra    , R, R(rd) = (sword_t)src1 >> (src2 & 0x1f));
  // OR
  // Performs a bitwise OR operation on src1 and src2 and stores the result in rd.
  INSTPAT("0000000 ????? ????? 110 ????? 01100 11", or     , R, R(rd) = src1 | src2);
  // AND
  // Performs a bitwise AND operation on src1 and src2 and stores the result in rd.
  INSTPAT("0000000 ????? ????? 111 ????? 01100 11", and    , R, R(rd) = src1 & src2);

  // Arithmetic Instructions (I-type)
  // ADDI
  // Adds the sign-extended 12-bit immediate to rs1 and stores the result in rd.
  INSTPAT("??????? ????? ????? 000 ????? 00100 11", addi   , I, R(rd) = src1 + imm);
  // SLTI (Set Less Than Immediate)
  // Sets rd to 1 if src1 (signed) < imm (signed), otherwise 0.
  INSTPAT("??????? ????? ????? 010 ????? 00100 11", slti   , I, R(rd) = (sword_t)src1 < (sword_t)imm);
  // SLTIU (Set Less Than Immediate Unsigned)
  // Sets rd to 1 if src1 (unsigned) < imm (unsigned), otherwise 0.
  INSTPAT("??????? ????? ????? 011 ????? 00100 11", sltiu  , I, R(rd) = (uint32_t)src1 < imm);
  // XORI
  // Performs a bitwise XOR operation on src1 and the sign-extended 12-bit immediate.
  INSTPAT("??????? ????? ????? 100 ????? 00100 11", xori   , I, R(rd) = src1 ^ imm);
  // ORI
  // Performs a bitwise OR operation on src1 and the sign-extended 12-bit immediate.
  INSTPAT("??????? ????? ????? 110 ????? 00100 11", ori    , I, R(rd) = src1 | imm);
  // ANDI
  // Performs a bitwise AND operation on src1 and the sign-extended 12-bit immediate.
  INSTPAT("??????? ????? ????? 111 ????? 00100 11", andi   , I, R(rd) = src1 & imm);
  // SLLI (Shift Left Logical Immediate)
  // Shifts src1 left by the amount specified in the lowest 5 bits of the immediate, zero-extending.
  INSTPAT("0000000 ????? ????? 001 ????? 00100 11", slli   , I, R(rd) = src1 << (imm & 0x1f));
  // SRLI (Shift Right Logical Immediate)
  // Shifts src1 right by the amount specified in the lowest 5 bits of the immediate, zero-extending.
  INSTPAT("0000000 ????? ????? 101 ????? 00100 11", srli   , I, R(rd) = src1 >> (imm & 0x1f));
  // SRAI (Shift Right Arithmetic Immediate)
  // Shifts src1 right by the amount specified in the lowest 5 bits of the immediate, sign-extending.
  INSTPAT("0100000 ????? ????? 101 ????? 00100 11", srai   , I, R(rd) = (sword_t)src1 >> (imm & 0x1f));

  // Control Transfer Instructions
  // JAL (Jump and Link)
  // JAL forms a signed offset by multiplying the J-immediate by 2. The offset is added to the
  // current PC to form the jump target address. The address of the instruction following JAL (PC+4)
  // is stored in register rd.
  INSTPAT("??????? ????? ????? ??? ????? 11011 11", jal    , J, s->dnpc = s->pc + imm; R(rd) = s->snpc);
  // JALR (Jump and Link Register)
  // JALR forms the target address by adding the sign-extended 12-bit immediate to rs1,
  // then setting the least significant bit of the result to zero. The address of the instruction
  // following JALR (PC+4) is written to register rd.
  INSTPAT("??????? ????? ????? 000 ????? 11001 11", jalr   , I, s->dnpc = (src1 + imm) & ~1; R(rd) = s->snpc);
  // BEQ (Branch Equal)
  // Takes the branch if rs1 == rs2.
  INSTPAT("??????? ????? ????? 000 ????? 11000 11", beq    , B, if (src1 == src2) s->dnpc = s->pc + imm);
  // BNE (Branch Not Equal)
  // Takes the branch if rs1 != rs2.
  INSTPAT("??????? ????? ????? 001 ????? 11000 11", bne    , B, if (src1 != src2) s->dnpc = s->pc + imm);
  // BLT (Branch Less Than)
  // Takes the branch if rs1 (signed) < rs2 (signed).
  INSTPAT("??????? ????? ????? 100 ????? 11000 11", blt    , B, if ((sword_t)src1 < (sword_t)src2) s->dnpc = s->pc + imm);
  // BGE (Branch Greater Than or Equal)
  // Takes the branch if rs1 (signed) >= rs2 (signed).
  INSTPAT("??????? ????? ????? 101 ????? 11000 11", bge    , B, if ((sword_t)src1 >= (sword_t)src2) s->dnpc = s->pc + imm);
  // BLTU (Branch Less Than Unsigned)
  // Takes the branch if rs1 (unsigned) < rs2 (unsigned).
  INSTPAT("??????? ????? ????? 110 ????? 11000 11", bltu   , B, if ((uint32_t)src1 < (uint32_t)src2) s->dnpc = s->pc + imm);
  // BGEU (Branch Greater Than or Equal Unsigned)
  // Takes the branch if rs1 (unsigned) >= rs2 (unsigned).
  INSTPAT("??????? ????? ????? 111 ????? 11000 11", bgeu   , B, if ((uint32_t)src1 >= (uint32_t)src2) s->dnpc = s->pc + imm);

  // System Instructions
  // ECALL (Environment Call)
  // Used to make a service request to the execution environment.
  INSTPAT("0000000 00000 00000 000 00000 11100 11", ecall  , N, NEMUTRAP(s->pc, R(10))); // R(10) is $a0 for argument/return value
  // EBREAK (Environment Breakpoint)
  // Used to return control to a debugging environment.
  INSTPAT("0000000 00001 00000 000 00000 11100 11", ebreak , N, NEMUTRAP(s->pc, R(10))); // R(10) is $a0

  // FENCE (Memory Fence)
  // Used to order device I/O and memory accesses as seen by other RISC-V harts and external devices.
  INSTPAT("??????? ????? ????? 000 ????? 00011 11", fence  , I, ); // No operation in NEMU for fence, it's a memory ordering hint

  // FENCE.I (Instruction Fetch Fence)
  // Used to synchronize instruction and data streams. Ensures subsequent instruction fetches
  // on a RISC-V hart will see any prior data stores that are visible to the same RISC-V hart.
  INSTPAT("??????? ????? ????? 001 ????? 00011 11", fence_i, I, ); // No operation in NEMU for fence.i

  // "M" Standard Extension for Integer Multiplication and Division (RV32M)
  // MUL
  // Multiplies src1 and src2, stores the lower XLEN bits of the product in rd.
  INSTPAT("0000001 ????? ????? 000 ????? 01100 11", mul    , R, R(rd) = src1 * src2);
  // MULH (Multiply High Signed-Signed)
  // Multiplies src1 and src2 (both signed), stores the upper XLEN bits of the 2*XLEN-bit product in rd.
  INSTPAT("0000001 ????? ????? 001 ????? 01100 11", mulh   , R, R(rd) = ((long long)src1 * (long long)src2) >> 32);
  // MULHSU (Multiply High Signed-Unsigned)
  // Multiplies src1 (signed) and src2 (unsigned), stores the upper XLEN bits of the 2*XLEN-bit product in rd.
  INSTPAT("0000001 ????? ????? 010 ????? 01100 11", mulhsu , R, R(rd) = ((long long)src1 * (unsigned long long)src2) >> 32);
  // MULHU (Multiply High Unsigned-Unsigned)
  // Multiplies src1 and src2 (both unsigned), stores the upper XLEN bits of the 2*XLEN-bit product in rd.
  INSTPAT("0000001 ????? ????? 011 ????? 01100 11", mulhu  , R, R(rd) = ((unsigned long long)src1 * (unsigned long long)src2) >> 32);
  // DIV (Divide Signed)
  // Divides src1 by src2 (both signed), rounds towards zero, and stores the quotient in rd.
  // Handles division by zero and signed overflow cases as per spec.
  INSTPAT("0000001 ????? ????? 100 ????? 01100 11", div    , R, R(rd) = (src2 == 0) ? -1 : ((sword_t)src1 == (sword_t)0x80000000 && (sword_t)src2 == -1) ? (sword_t)src1 : (sword_t)src1 / (sword_t)src2);
  // DIVU (Divide Unsigned)
  // Divides src1 by src2 (both unsigned), and stores the quotient in rd.
  // Handles division by zero (result 0xFFFFFFFF for unsigned max).
  INSTPAT("0000001 ????? ????? 101 ????? 01100 11", divu   , R, R(rd) = (src2 == 0) ? 0xFFFFFFFF : src1 / src2);
  // REM (Remainder Signed)
  // Divides src1 by src2 (both signed), stores the remainder (with same sign as dividend) in rd.
  // Handles division by zero and signed overflow cases.
  INSTPAT("0000001 ????? ????? 110 ????? 01100 11", rem    , R, R(rd) = (src2 == 0) ? src1 : ((sword_t)src1 == (sword_t)0x80000000 && (sword_t)src2 == -1) ? 0 : (sword_t)src1 % (sword_t)src2);
  // REMU (Remainder Unsigned)
  // Divides src1 by src2 (both unsigned), stores the remainder in rd.
  // Handles division by zero.
  INSTPAT("0000001 ????? ????? 111 ????? 01100 11", remu   , R, R(rd) = (src2 == 0) ? src1 : src1 % src2);

  // Invalid Instruction Pattern: If no match, it's an invalid instruction.
  // INV (Invalid Instruction)
  // If the instruction does not match any known pattern, panic with an invalid instruction error.
  INSTPAT("??????? ????? ????? ??? ????? ????? ??", inv    , N, INV(s->pc));
  INSTPAT_END(); // End instruction pattern matching
  R(0) = 0; // reset $zero to 0

  return 0;
}

int isa_exec_once(Decode *s) {
  s->isa.inst = inst_fetch(&s->snpc, 4);
  return decode_exec(s);
}
