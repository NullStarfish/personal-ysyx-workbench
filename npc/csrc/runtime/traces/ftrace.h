#ifndef NPC_RUNTIME_FTRACE_H
#define NPC_RUNTIME_FTRACE_H

#include <memory>
#include <string>

#include "runtime/traces/trace.h"

class FTrace final : public Trace {
public:
  FTrace();
  ~FTrace() override;

  FTrace(const FTrace &) = delete;
  FTrace &operator=(const FTrace &) = delete;

  void setElfFile(const char *path);
  void init() override;
  void reset() override;
  void record(const RetireEvent &event) override;
  void printStack() const;

private:
  void loadSymbols();

  class Impl;
  std::unique_ptr<Impl> impl;
  std::string elfFile;
};

#endif
