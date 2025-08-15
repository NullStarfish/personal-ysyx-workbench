#ifndef __ITRACE_H__
#define __ITRACE_H__

#include <stdint.h>

// Formats, logs, and traces a single instruction.
void log_and_trace(uint32_t pc, uint32_t inst);

// Prints the content of the ring buffer.
void print_iring_buffer();

#endif
