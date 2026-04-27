#ifndef __DISASM_H__
#define __DISASM_H__

#include <stdint.h>

/**
 * @brief Initializes the Capstone disassembler engine.
 * This must be called once before any disassembly.
 */
void init_disasm();

/**
 * @brief Disassembles a single instruction.
 *
 * @param str The output buffer to write the assembly string to.
 * @param size The size of the output buffer.
 * @param pc The program counter address of the instruction.
 * @param code A pointer to the raw instruction bytes.
 * @param nbyte The number of bytes in the instruction (e.g., 4 for RISC-V).
 */
void disassemble(char *str, int size, uint64_t pc, uint8_t *code, int nbyte);

#endif
