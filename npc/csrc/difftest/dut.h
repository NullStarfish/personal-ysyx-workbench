#ifndef __DIFFTEST_H__
#define __DIFFTEST_H__

// 这是一个标准的宏，只有 C++ 编译器会定义它
#ifdef __cplusplus
extern "C" {
#endif

#include <stdint.h>
#include <stdbool.h>

// A flag to check if difftest is enabled.
extern bool difftest_is_enabled;

// Direction for data copy
#define DIFFTEST_TO_REF 1
#define DIFFTEST_TO_DUT 0

typedef struct {
  uint32_t mtvec;
  uint32_t mepc;
  uint32_t mstatus;
  uint32_t mcause;
} CSRS;

// Structure to hold the DUT's register state for comparison
typedef struct {
  uint32_t gpr[32];
  uint32_t pc;
  CSRS csrs;
} riscv32_CPU_state;

void init_difftest(char *ref_so_file, long img_size);
void difftest_step();
void difftest_skip_ref();

#ifdef __cplusplus
}
#endif

#endif