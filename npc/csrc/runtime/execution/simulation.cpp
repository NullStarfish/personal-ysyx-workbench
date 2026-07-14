#include "runtime/execution/simulation.h"

#include <cstdio>

#include "runtime/base/cpu.h"
#include "runtime/base/run_control.h"
#include "runtime/execution/interrupt.h"
#include "runtime/execution/retire_pipeline.h"
#include "runtime/platform/dut.h"
#include "runtime/services/logger.h"
#include "runtime/services/sim_counter.h"
#include "runtime/traces/ftrace.h"
#include "runtime/traces/itrace.h"

Simulation::Simulation(Dut &dut, CPU &cpu, RunControl &runControl, RetirePipeline &retirePipeline,
                       Logger &logger, SimCounterBank &counters, ITrace &itrace, FTrace &ftrace)
    : dut(dut), cpu(cpu), runControl(runControl), retirePipeline(retirePipeline),
      logger(logger), counters(counters), itrace(itrace), ftrace(ftrace) {}

void Simulation::init() {
  cpu.init();
  runControl.reset();
  counters.reset();
  cycleCountValue = 0;
}

void Simulation::run(uint64_t count) {
  if (runControl.hasEnded()) {
    printf("Program execution has ended. To restart, exit and run again.\n");
    return;
  }
  runControl.start();
#ifdef CONFIG_RETIRE_TRACE
  for (; count > 0; --count) {
    stepInstruction();
    if (!runControl.isRunning()) break;
  }
#else
  for (; count > 0 && runControl.isRunning(); --count) {
    dut.stepCycle();
    ++cycleCountValue;
    if (checkInterrupt()) break;
  }
#endif
  if (runControl.isRunning()) runControl.stop();
}

void Simulation::stepInstruction() {
#ifdef CONFIG_RETIRE_TRACE
  cpu.beginRetireWait();
  while (!cpu.hasRetired() && runControl.isRunning()) {
    dut.stepCycle();
    ++cycleCountValue;
    if (checkInterrupt()) return;
  }
  if (!runControl.isRunning()) return;
  retirePipeline.process(cpu.lastRetire());
#else
  dut.stepCycle();
  ++cycleCountValue;
#endif
}

void Simulation::acceptRetire(const RetireEvent &event) {
#ifdef CONFIG_PCTRACE
  logger.writePcTrace("pc: %x\n", event.pc);
#endif
  cpu.acceptRetire(event);
}

void Simulation::handleEbreak() {
#ifdef CONFIG_RETIRE_TRACE
  const uint32_t a0 = cpu.regRead(10);
  if (a0 == 0) runControl.end(a0);
  else runControl.abort(a0);
  printf("ebreak: state: %d, a0: %d\n", static_cast<int>(runControl.status()), a0);
#else
  runControl.end(0);
  printf("ebreak: state: %d\n", static_cast<int>(runControl.status()));
#endif
}

void Simulation::handleInterrupt() {
  printf("\n\nCaught Ctrl+C (SIGINT). Terminating simulation...\n");
#ifdef CONFIG_FTRACE
  ftrace.printStack();
#endif
  runControl.quit();
}

void Simulation::printStats() const {
  const uint64_t retired = counters.read("inst", "total");
  printf("\nExecution Statistics:\n");
  printf("  Total Cycles:       %llu\n", static_cast<unsigned long long>(cycleCountValue));
  printf("  Total Instructions: %llu\n", static_cast<unsigned long long>(retired));
  counters.dump();
}

void Simulation::printFailureContext() const {
  cpu.isa_reg_display();
#ifdef CONFIG_ITRACE
  itrace.printRecent();
#endif
#ifdef CONFIG_FTRACE
  ftrace.printStack();
#endif
}

uint64_t Simulation::cycleCount() const { return cycleCountValue; }

bool Simulation::checkInterrupt() {
  if (!consumeRuntimeInterrupt()) return false;
  handleInterrupt();
  return true;
}
