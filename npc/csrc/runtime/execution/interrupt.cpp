#include "runtime/execution/interrupt.h"

#include <csignal>

namespace {
volatile sig_atomic_t interruptPending = 0;
}

void requestRuntimeInterrupt() { interruptPending = 1; }
bool consumeRuntimeInterrupt() {
  if (interruptPending == 0) return false;
  interruptPending = 0;
  return true;
}
