#ifndef __FTRACE_H__
#define __FTRACE_H__

#include <stdint.h>
#include <stdbool.h>

// Initializes the function tracer by reading an ELF file.
void init_ftrace(const char *elf_file);

// This is the main trace function, called after each instruction execution.
void trace_func_call(uint32_t pc, uint32_t inst);

// Prints the function call stack, useful for debugging failures.
void print_ftrace_stack();

#endif
