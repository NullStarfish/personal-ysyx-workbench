#ifndef NPC_RUNTIME_WATCHPOINT_H
#define NPC_RUNTIME_WATCHPOINT_H

#include <memory>

class CPU;
class Memory;

class WatchpointManager {
public:
  WatchpointManager(CPU &cpu, Memory &memory);
  ~WatchpointManager();

  WatchpointManager(const WatchpointManager &) = delete;
  WatchpointManager &operator=(const WatchpointManager &) = delete;

  void init();
  uint32_t evaluate(const char *expression, bool *success);
  bool add(const char *expression);
  bool remove(int number);
  void display() const;
  bool check();

private:
  class Impl;
  std::unique_ptr<Impl> impl;
};

#endif
