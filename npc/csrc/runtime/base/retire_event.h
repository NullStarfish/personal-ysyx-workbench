#ifndef NPC_RETIRE_EVENT_H
#define NPC_RETIRE_EVENT_H

#include <cstdint>

struct CSRS {
  uint32_t mtvec = 0;
  uint32_t mepc = 0;
  uint32_t mstatus = 0;
  uint32_t mcause = 0;
};

struct riscv32_CPU_state {
  uint32_t gpr[32] = {};
  uint32_t pc = 0;
  CSRS csrs = {};
};

struct RetireEvent {
  uint32_t pc = 0;
  uint32_t dnpc = 0;
  uint32_t inst = 0;
  uint32_t instType = 0;
  uint32_t gpr[32] = {};
  bool regWen = false;
  uint32_t regAddr = 0;
  uint32_t regData = 0;
  CSRS csrs = {};
};

#endif
