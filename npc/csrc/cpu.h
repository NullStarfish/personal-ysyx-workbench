#ifndef NPC_CPU_H
#define NPC_CPU_H

#include <cstddef>
#include <cstdint>

#include "svdpi.h"

#ifdef __cplusplus
extern "C" {
#endif

typedef struct {
  uint32_t mtvec;
  uint32_t mepc;
  uint32_t mstatus;
  uint32_t mcause;
} CSRS;

typedef struct {
  uint32_t gpr[32];
  uint32_t pc;
  CSRS csrs;
} riscv32_CPU_state;

extern bool difftestis_enabled;

#define DIFFTEST_TO_REF 1
#define DIFFTEST_TO_DUT 0

void diffteststep();
void difftestskip_ref();

#ifdef __cplusplus
}
#endif

#ifdef __cplusplus
class CPU;
class Runtime;
class Difftest;
class Mem;

class CPU {
public:
  enum instType {arith, mem, redirect, sys};
  struct RetireSnapshot {
    uint32_t pc = 0;
    uint32_t dnpc = 0;
    uint32_t inst = 0;
    uint32_t instType = arith;
    uint32_t gpr[32] = {};
    bool regWen = false;
    uint32_t regAddr = 0;
    uint32_t regData = 0;
    CSRS csrs = {};
  };

  CPU() = default;

  void init();
  void exec(uint64_t n);
  void execOnce();
  void commitRetire(const RetireSnapshot &snapshot);

  uint32_t pc() const;
  uint32_t retirePc() const;
  uint32_t inst() const;
  uint32_t regRead(int regNum) const;
  uint32_t csrRead(int csrNum) const;
  uint32_t regStr2Val(const char *s, bool *success) const;
  void isa_reg_display() const;
  uint32_t isa_reg_str2val(const char *s, bool *success) const;
  riscv32_CPU_state dutState() const;
  void copyDutState(riscv32_CPU_state *dut) const;

  long long cycleCount() const;
  void printStats() const;
  void handleSigint();

private:
  void traceAndDifftest();

  riscv32_CPU_state archState{};
  uint32_t retirePcValue = 0;
  uint32_t retireInstValue = 0;
  bool hasCommitted = false;
  long long cycleCountValue = 0;
};
#endif

#endif
