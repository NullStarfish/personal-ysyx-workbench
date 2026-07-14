#include "runtime/base/host_clock.h"

#include <sys/time.h>

uint64_t HostClock::nowMicros() {
  timeval now{};
  gettimeofday(&now, nullptr);
  return static_cast<uint64_t>(now.tv_sec) * 1000000ull + static_cast<uint64_t>(now.tv_usec);
}

void HostClock::reset() { bootTime = nowMicros(); }

uint64_t HostClock::elapsedMicros() const {
  const uint64_t now = nowMicros();
  return bootTime == 0 ? 0 : now - bootTime;
}
