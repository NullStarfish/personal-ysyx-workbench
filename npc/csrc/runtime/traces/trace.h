#ifndef NPC_RUNTIME_TRACE_H
#define NPC_RUNTIME_TRACE_H

#include <cstdint>

#include "runtime/base/retire_event.h"

class Trace {
public:
  virtual ~Trace() = default;

  virtual void init() = 0;
  virtual void reset() = 0;
  virtual void record(const RetireEvent &event) = 0;

  bool enabled() const { return enabledValue; }

protected:
  void setEnabled(bool enabled) { enabledValue = enabled; }

private:
  bool enabledValue = false;
};

#endif
