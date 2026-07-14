#ifndef NPC_DIFFTEST_RUNTIME_H
#define NPC_DIFFTEST_RUNTIME_H

#include <string>

#include "runtime/base/cpu.h"

class Memory;

#define DIFFTEST_TO_REF 1
#define DIFFTEST_TO_DUT 0

enum class DifftestResult { Continue, Mismatch };

class Difftest {
public:
  Difftest(CPU &cpu, Memory &memory);
  ~Difftest();

  Difftest(const Difftest &) = delete;
  Difftest &operator=(const Difftest &) = delete;

  void setRefSoFile(const char *path);
  void init(long imgSize);
  void shutdown();
  DifftestResult step(const RetireEvent &event);
  void skipRef();
  bool enabled() const;

private:
  using RefMemcpy = void (*)(uint32_t addr, void *buf, size_t n, bool direction);
  using RefRegcpy = void (*)(void *dut, bool direction);
  using RefExec = void (*)(uint64_t n);
  using RefInit = void (*)(int port);

  void remember(const riscv32_CPU_state &dut);
  bool armAtSdram();
  bool isMemoryInstruction(uint32_t inst, uint32_t *addr, uint32_t *len) const;
  bool shouldSkipRefForInst(uint32_t inst) const;
  bool checkregs(const riscv32_CPU_state &dut, const riscv32_CPU_state &ref);

  CPU &cpu;
  Memory &memory;
  bool isSkipRef = false;
  bool refReady = false;
  bool enabledValue = false;
  std::string refSoFile;
  void *refHandle = nullptr;
  long imageSize = 0;
  riscv32_CPU_state lastDutState{};
  bool hasLastDutState = false;
  RefMemcpy refMemcpy = nullptr;
  RefRegcpy refRegcpy = nullptr;
  RefExec refExec = nullptr;
  RefInit refInit = nullptr;
};

#endif
