#ifndef __DIFFTEST_H__
#define __DIFFTEST_H__

#include <stdint.h>
#include <stdbool.h>

// A flag to check if difftest is enabled.
extern bool difftest_is_enabled;

// Direction for data copy
#define DIFFTEST_TO_REF 1
#define DIFFTEST_TO_DUT 0

// Structure to hold the DUT's register state for comparison
typedef struct {
  uint32_t gpr[32];
  uint32_t pc;
} riscv32_CPU_state;

void init_difftest(char *ref_so_file, long img_size);
void difftest_step();

#endif
