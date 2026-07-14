#ifndef NPC_RUNTIME_ITRACE_H
#define NPC_RUNTIME_ITRACE_H

#include <array>
#include <string>

#include "runtime/traces/trace.h"

class Logger;

class ITrace final : public Trace {
public:
  explicit ITrace(Logger &logger);

  void init() override;
  void reset() override;
  void record(const RetireEvent &event) override;
  void printRecent() const;

private:
  static constexpr size_t kRingCapacity = 16;

  Logger &logger;
  std::array<std::string, kRingCapacity> ring;
  size_t writeIndex = 0;
  size_t size = 0;
};

#endif
