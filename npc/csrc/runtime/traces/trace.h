#ifndef NPC_RUNTIME_TRACE_H
#define NPC_RUNTIME_TRACE_H

#include <cstdint>

class Trace {
public:
  virtual ~Trace() = default;

  virtual void init() = 0;
  virtual void reset() = 0;
  virtual void record(uint32_t pc, uint32_t inst) = 0;

  bool enabled() const { return enabledValue; }

protected:
  void setEnabled(bool enabled) { enabledValue = enabled; }

private:
  bool enabledValue = false;
};

#endif
