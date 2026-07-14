#include "runtime/base/run_control.h"

void RunControl::reset() {
  statusValue = RunStatus::Stopped;
  haltCodeValue = 0;
}

void RunControl::start() { statusValue = RunStatus::Running; }
void RunControl::stop() { statusValue = RunStatus::Stopped; }
void RunControl::end(uint64_t haltCode) {
  statusValue = RunStatus::Ended;
  haltCodeValue = haltCode;
}
void RunControl::abort(uint64_t haltCode) {
  statusValue = RunStatus::Aborted;
  haltCodeValue = haltCode;
}
void RunControl::quit() { statusValue = RunStatus::Quit; }

bool RunControl::isRunning() const { return statusValue == RunStatus::Running; }
bool RunControl::hasEnded() const {
  return statusValue == RunStatus::Ended || statusValue == RunStatus::Aborted;
}
int RunControl::exitStatus() const {
  return !((statusValue == RunStatus::Ended && haltCodeValue == 0) || statusValue == RunStatus::Quit);
}
RunStatus RunControl::status() const { return statusValue; }
uint64_t RunControl::haltCode() const { return haltCodeValue; }
