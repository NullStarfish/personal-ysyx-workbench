#ifndef NPC_RUNTIME_SDB_H
#define NPC_RUNTIME_SDB_H

#include <memory>

class CPU;
class Dut;
class FTrace;
class Memory;
class IlaEngine;
class ProgramImage;
class RunControl;
class Simulation;
class WatchpointManager;

class Sdb {
public:
  Sdb(Simulation &simulation, CPU &cpu, Memory &memory, WatchpointManager &watchpoints,
      FTrace &ftrace, Dut &dut, ProgramImage &program, RunControl &runControl, IlaEngine &ila);
  ~Sdb();

  Sdb(const Sdb &) = delete;
  Sdb &operator=(const Sdb &) = delete;

  void init();
  void mainLoop();
  void setBatchMode();

private:
  class Impl;
  std::unique_ptr<Impl> impl;
};

#endif
