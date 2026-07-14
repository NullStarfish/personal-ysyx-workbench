#ifndef NPC_RUNTIME_SDB_H
#define NPC_RUNTIME_SDB_H

#include <memory>

class Runtime;

class Sdb {
public:
  explicit Sdb(Runtime &runtime);
  ~Sdb();

  Sdb(const Sdb &) = delete;
  Sdb &operator=(const Sdb &) = delete;

  void init();
  void mainLoop();
  void setBatchMode();
  bool checkWatchpoints();

private:
  class Impl;
  std::unique_ptr<Impl> impl;
};

#endif
