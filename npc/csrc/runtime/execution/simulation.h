#ifndef NPC_RUNTIME_SIMULATION_H
#define NPC_RUNTIME_SIMULATION_H

#include <cstdint>

#include "runtime/base/retire_event.h"

class CPU;
class Dut;
class FTrace;
class ITrace;
class IlaEngine;
class Logger;
class RetirePipeline;
class RunControl;
class SimCounterBank;

class Simulation {
public:
  Simulation(Dut &dut, CPU &cpu, RunControl &runControl, RetirePipeline &retirePipeline,
             Logger &logger, SimCounterBank &counters, ITrace &itrace, FTrace &ftrace, IlaEngine &ila);

  void init();
  void run(uint64_t count);
  void stepInstruction();
  void acceptRetire(const RetireEvent &event);
  void handleEbreak();
  bool resumeFromEbreak();
  bool stoppedAtEbreak() const;
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
  IlaEngine &ila;
  uint64_t cycleCountValue = 0;
  bool stoppedAtEbreakValue = false;
};

#endif
