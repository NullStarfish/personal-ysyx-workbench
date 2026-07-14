#include "runtime/base/cpu.h"

#include <cstdio>
#include <cstring>
#include <map>
#include <string>

#ifndef CONFIG_RESET_PC
#define CONFIG_RESET_PC 0x30000000u
#endif

void CPU::init() {
  archState = {};
  archState.pc = CONFIG_RESET_PC;
  archState.csrs.mstatus = 0x1800;
  lastRetireValue = {};
  lastRetireValue.pc = CONFIG_RESET_PC;
  retired = false;
}

void CPU::beginRetireWait() { retired = false; }

void CPU::acceptRetire(const RetireEvent &event) {
  lastRetireValue = event;
  archState.pc = event.dnpc;
  archState.csrs = event.csrs;
  memcpy(archState.gpr, event.gpr, sizeof(archState.gpr));
  if (event.regWen && event.regAddr != 0) {
    archState.gpr[event.regAddr & 0x1f] = event.regData;
  }
  archState.gpr[0] = 0;
  memcpy(lastRetireValue.gpr, archState.gpr, sizeof(lastRetireValue.gpr));
  lastRetireValue.csrs = archState.csrs;
  retired = true;
}

bool CPU::hasRetired() const { return retired; }
const RetireEvent &CPU::lastRetire() const { return lastRetireValue; }
uint32_t CPU::pc() const { return archState.pc; }
uint32_t CPU::retirePc() const { return lastRetireValue.pc; }
uint32_t CPU::inst() const { return lastRetireValue.inst; }

uint32_t CPU::regRead(int regNum) const {
  return regNum >= 0 && regNum < 32 ? archState.gpr[regNum] : 0;
}

uint32_t CPU::csrRead(int csrNum) const {
  switch (csrNum) {
    case 0x300: return archState.csrs.mstatus;
    case 0x305: return archState.csrs.mtvec;
    case 0x341: return archState.csrs.mepc;
    case 0x342: return archState.csrs.mcause;
    default: return 0;
  }
}

void CPU::isa_reg_display() const {
  static const char *abiNames[32] = {
      "zero", "ra", "sp", "gp", "tp", "t0", "t1", "t2",
      "s0", "s1", "a0", "a1", "a2", "a3", "a4", "a5",
      "a6", "a7", "s2", "s3", "s4", "s5", "s6", "s7",
      "s8", "s9", "s10", "s11", "t3", "t4", "t5", "t6",
  };
  for (int i = 0; i < 32; ++i) {
    printf("  $%-4s (x%-2d) = 0x%08x\n", abiNames[i], i, regRead(i));
  }
  printf("  $pc       = 0x%08x\n", pc());
  printf("$mstatus = 0x%08x\n", csrRead(0x300));
  printf("$mtvec  = 0x%08x\n", csrRead(0x305));
  printf("$mepc   = 0x%08x\n", csrRead(0x341));
  printf("$mcause = 0x%08x\n", csrRead(0x342));
}

uint32_t CPU::isa_reg_str2val(const char *s, bool *success) const {
  static const std::map<std::string, int> regMap = {
      {"pc", -1}, {"zero", 0}, {"ra", 1}, {"sp", 2}, {"gp", 3}, {"tp", 4},
      {"t0", 5}, {"t1", 6}, {"t2", 7}, {"s0", 8}, {"s1", 9}, {"a0", 10},
      {"a1", 11}, {"a2", 12}, {"a3", 13}, {"a4", 14}, {"a5", 15}, {"a6", 16},
      {"a7", 17}, {"s2", 18}, {"s3", 19}, {"s4", 20}, {"s5", 21}, {"s6", 22},
      {"s7", 23}, {"s8", 24}, {"s9", 25}, {"s10", 26}, {"s11", 27}, {"t3", 28},
      {"t4", 29}, {"t5", 30}, {"t6", 31},
  };

  *success = true;
  const std::string name(s);
  const auto found = regMap.find(name);
  if (found != regMap.end()) return found->second == -1 ? pc() : regRead(found->second);
  if (name.size() > 1 && name[0] == 'x') {
    try {
      const int regNum = std::stoi(name.substr(1));
      if (regNum >= 0 && regNum < 32) return regRead(regNum);
    } catch (...) {
    }
  }
  *success = false;
  return 0;
}

const riscv32_CPU_state &CPU::state() const { return archState; }
