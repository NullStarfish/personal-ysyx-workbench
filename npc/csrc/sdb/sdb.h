#ifndef __SDB_H__
#define __SDB_H__

#include <cstdint>

// --- SDB Public Interface ---
// Initializes all SDB components.
void init_sdb();
// The main loop for the Simple Debugger.
void sdb_mainloop();

// --- CPU Execution Control ---
// Executes the simulation for n cycles.
void cpu_exec(uint64_t n);

// --- ISA Access (to be implemented in main.cpp) ---
// Reads a general-purpose register by its number.
uint32_t isa_reg_read(int reg_num);
// Displays the state of all registers.
void isa_reg_display();
// Reads a 32-bit word from the simulated physical memory.
uint32_t paddr_read(uint32_t addr);
// Converts a register name string (e.g., "pc", "a0") to its value.
uint32_t isa_reg_str2val(const char *s, bool *success);

// --- Expression Evaluation (from expr.cpp) ---
// Initializes the regex compiler.
void init_regex();
// Evaluates a string expression.
uint32_t expr(char *e, bool *success);

// --- Watchpoint Management (from watchpoint.cpp) ---
// Initializes the watchpoint pool.
void init_wp_pool();
// Checks if any watchpoint has been triggered.
bool check_watchpoints();
// Displays all active watchpoints.
void display_wp();
// Removes a watchpoint by its number.
void wp_remove(int no);
// Adds a new watchpoint from an expression string.
void wp_add(char* args);

#endif
