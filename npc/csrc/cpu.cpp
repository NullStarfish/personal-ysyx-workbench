#include "cpu.h"

#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <dlfcn.h>
#include <limits.h>
#include <map>
#include <string>

#include "difftest_runtime.h"
#include "mem.h"
#include "runtime.h"
#include "sim.h"

#include "sdb/sdb.h"
#include "trace/ftrace.h"
#include "trace/itrace.h"

#ifndef PATH_MAX
#define PATH_MAX 4096
#endif

#ifndef IMAGE_BASE_ADDR
#define IMAGE_BASE_ADDR 0xa0000000u
#endif

namespace {
constexpr uint32_t kResetPc = 0xa0000000u;
}

void CPU::init() {
  archState = {};
  archState.pc = kResetPc;
  archState.csrs.mstatus = 0x1800;
  retirePcValue = kResetPc;
  retireInstValue = 0;
  hasCommitted = false;
}

void CPU::exec(uint64_t n) {
  if (runtime.hasEnded()) {
    printf("Program execution has ended. To restart, exit and run again.\n");
    return;
  }
  runtime.setRunning();
  for (; n > 0; n--) {
    execOnce();
    if (!runtime.isRunning()) break;
  }
  if (runtime.isRunning()) {
    runtime.setStop();
  }
}

void CPU::execOnce() {
  hasCommitted = false;
  while (!hasCommitted && runtime.isRunning()) {
    runtime.stepOneClk();
    cycleCountValue++;
  }
  if (!runtime.isRunning()) return;

  instrCountValue++;
  traceAndDifftest();
}

void CPU::traceAndDifftest() {
#ifdef CONFIG_ITRACE
  log_and_trace(retirePcValue, retireInstValue);
#endif

#ifdef CONFIG_FTRACE
  trace_func_call(retirePcValue, retireInstValue);
#endif

  difftest.step();

#ifdef CONFIG_WATCHPOINT
  volatile bool needStop = check_watchpoints();
  if (needStop) {
    runtime.setStop();
  }
#endif
}

void CPU::commitRetire(const RetireSnapshot &snapshot) {
  retirePcValue = snapshot.pc;
  retireInstValue = snapshot.inst;
  archState.pc = snapshot.dnpc;
  archState.csrs = snapshot.csrs;
  memcpy(archState.gpr, snapshot.gpr, sizeof(archState.gpr));
  if (snapshot.regWen && snapshot.regAddr != 0) {
    archState.gpr[snapshot.regAddr & 0x1f] = snapshot.regData;
  }
  hasCommitted = true;
}

uint32_t CPU::pc() const { return archState.pc; }
uint32_t CPU::retirePc() const { return retirePcValue; }
uint32_t CPU::inst() const { return retireInstValue; }

uint32_t CPU::regRead(int regNum) const {
  if (regNum >= 0 && regNum < 32) return archState.gpr[regNum];
  return 0;
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
      "zero", "ra",   "sp",  "gp",  "tp",  "t0",  "t1",  "t2",
      "s0",   "s1",   "a0",  "a1",  "a2",  "a3",  "a4",  "a5",
      "a6",   "a7",   "s2",  "s3",  "s4",  "s5",  "s6",  "s7",
      "s8",   "s9",   "s10", "s11", "t3",  "t4",  "t5",  "t6",
  };

  for (int i = 0; i < 32; i++) {
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
      {"pc", -1},   {"zero", 0}, {"ra", 1},   {"sp", 2},   {"gp", 3},   {"tp", 4},
      {"t0", 5},    {"t1", 6},   {"t2", 7},   {"s0", 8},   {"s1", 9},   {"a0", 10},
      {"a1", 11},   {"a2", 12},  {"a3", 13},  {"a4", 14},  {"a5", 15},  {"a6", 16},
      {"a7", 17},   {"s2", 18},  {"s3", 19},  {"s4", 20},  {"s5", 21},  {"s6", 22},
      {"s7", 23},   {"s8", 24},  {"s9", 25},  {"s10", 26}, {"s11", 27}, {"t3", 28},
      {"t4", 29},   {"t5", 30},  {"t6", 31},
  };

  *success = true;
  std::string str(s);
  if (regMap.count(str)) {
    int regNum = regMap.at(str);
    return regNum == -1 ? pc() : regRead(regNum);
  }
  if (str.length() > 1 && str[0] == 'x') {
    try {
      int regNum = std::stoi(str.substr(1));
      if (regNum >= 0 && regNum < 32) {
        return regRead(regNum);
      }
    } catch (...) {
    }
  }
  *success = false;
  return 0;
}



riscv32_CPU_state CPU::dutState() const { return archState; }

void CPU::copyDutState(riscv32_CPU_state *dut) const {
  if (dut == nullptr) return;
  *dut = dutState();
}
long long CPU::cycleCount() const { return cycleCountValue; }

void CPU::printStats() const {
  printf("\nExecution Statistics:\n");
  printf("  Total Cycles:       %lld\n", cycleCountValue);
  printf("  Total Instructions: %lld\n", instrCountValue);
  if (cycleCountValue > 0) {
    printf("  Average IPC:        %f\n", static_cast<double>(instrCountValue) / cycleCountValue);
  } else {
    printf("  Average IPC:        N/A (cycles = 0)\n");
  }
}

void CPU::handleSigint() {
  printf("\n\nCaught Ctrl+C (SIGINT). Terminating simulation...\n");
  printStats();
  exit(0);
}

extern "C" void dpi_update_state(int pc, int dnpc, int reg_wen, int reg_addr, int reg_data, const svBitVecVal *gprs,
                                 int mtvec, int mepc, int mstatus, int mcause, int inst) {
  CPU::RetireSnapshot snapshot;
  snapshot.pc = static_cast<uint32_t>(pc);
  snapshot.dnpc = static_cast<uint32_t>(dnpc);
  snapshot.inst = static_cast<uint32_t>(inst);
  snapshot.regWen = reg_wen != 0;
  snapshot.regAddr = static_cast<uint32_t>(reg_addr);
  snapshot.regData = static_cast<uint32_t>(reg_data);
  for (int i = 0; i < 32; ++i) {
    snapshot.gpr[i] = gprs[i];
  }
  snapshot.csrs.mtvec = static_cast<uint32_t>(mtvec);
  snapshot.csrs.mepc = static_cast<uint32_t>(mepc);
  snapshot.csrs.mstatus = static_cast<uint32_t>(mstatus);
  snapshot.csrs.mcause = static_cast<uint32_t>(mcause);
  cpu.commitRetire(snapshot);
}

extern "C" void ebreak() {
  uint32_t a0Val = cpu.regRead(10);
  if (a0Val == 0) runtime.setEnd(a0Val);
  else runtime.setAbort(a0Val);
  printf("ebreak: state: %d, a0: %d\n", runtime.state().state, a0Val);
}
