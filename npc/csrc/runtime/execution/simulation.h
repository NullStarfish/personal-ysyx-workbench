#ifndef NPC_RUNTIME_SIMULATION_H
#define NPC_RUNTIME_SIMULATION_H

#include <cstdint>

#include "runtime/base/retire_event.h"

class CPU;
class Dut;
class FTrace;
class ITrace;
class Logger;
class RetirePipeline;
class RunControl;
class SimCounterBank;

class Simulation {
public:
  Simulation(Dut &dut, CPU &cpu, RunControl &runControl, RetirePipeline &retirePipeline,
             Logger &logger, SimCounterBank &counters, ITrace &itrace, FTrace &ftrace);

  void init();
  void run(uint64_t count);
  void stepInstruction();
  void acceptRetire(const RetireEvent &event);
  void handleEbreak();
  void handleInterrupt();
  void printStats() const;
  void printFailureContext() const;
  uint64_t cycleCount() const;

private:
  bool checkInterrupt();

  Dut &dut;
  CPU &cpu;
  RunControl &runControl;
  RetirePipeline &retirePipeline;
  Logger &logger;
  SimCounterBank &counters;
  ITrace &itrace;
  FTrace &ftrace;
  uint64_t cycleCountValue = 0;
};

#endif
