#ifndef NPC_RUNTIME_HOST_CLOCK_H
#define NPC_RUNTIME_HOST_CLOCK_H

#include <cstdint>

class HostClock {
public:
  void reset();
  uint64_t elapsedMicros() const;

private:
  static uint64_t nowMicros();

  uint64_t bootTime = 0;
};

#endif
