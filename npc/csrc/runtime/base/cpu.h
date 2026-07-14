#ifndef NPC_CPU_H
#define NPC_CPU_H

#include <cstdint>

#include "runtime/base/retire_event.h"

class CPU {
public:
  enum InstType { arith, mem, redirect, sys };

  void init();
  void beginRetireWait();
  void acceptRetire(const RetireEvent &event);
  bool hasRetired() const;
  const RetireEvent &lastRetire() const;

  uint32_t pc() const;
  uint32_t retirePc() const;
  uint32_t inst() const;
  uint32_t regRead(int regNum) const;
  uint32_t csrRead(int csrNum) const;
  void isa_reg_display() const;
  uint32_t isa_reg_str2val(const char *s, bool *success) const;
  const riscv32_CPU_state &state() const;

private:
  riscv32_CPU_state archState{};
  RetireEvent lastRetireValue{};
  bool retired = false;
};

#endif
