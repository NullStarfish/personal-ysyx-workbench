#ifndef NPC_RUNTIME_RUN_CONTROL_H
#define NPC_RUNTIME_RUN_CONTROL_H

#include <cstdint>

enum class RunStatus { Running = 0, Stopped = 1, Ended = 2, Aborted = 3, Quit = 4 };

class RunControl {
public:
  void reset();
  void start();
  void stop();
  void end(uint64_t haltCode);
  void abort(uint64_t haltCode = 1);
  void quit();

  bool isRunning() const;
  bool hasEnded() const;
  int exitStatus() const;
  RunStatus status() const;
  uint64_t haltCode() const;

private:
  RunStatus statusValue = RunStatus::Stopped;
  uint64_t haltCodeValue = 0;
};

#endif
