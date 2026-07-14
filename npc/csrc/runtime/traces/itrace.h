#ifndef NPC_RUNTIME_ITRACE_H
#define NPC_RUNTIME_ITRACE_H

#include <array>
#include <string>

#include "runtime/traces/trace.h"

class ITrace final : public Trace {
public:
  ITrace();

  void init() override;
  void reset() override;
  void record(uint32_t pc, uint32_t inst) override;
  void printRecent() const;

private:
  static constexpr size_t kRingCapacity = 16;

  std::array<std::string, kRingCapacity> ring;
  size_t writeIndex = 0;
  size_t size = 0;
};

#endif
