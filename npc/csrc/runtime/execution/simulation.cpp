#include "runtime/execution/simulation.h"

#include <cstdio>

#include "runtime/base/cpu.h"
#include "runtime/base/run_control.h"
#include "runtime/execution/interrupt.h"
#include "runtime/execution/retire_pipeline.h"
#include "runtime/platform/dut.h"
#include "runtime/services/logger.h"
#include "runtime/services/ila_engine.h"
#include "runtime/services/sim_counter.h"
#include "runtime/traces/ftrace.h"
#include "runtime/traces/itrace.h"

Simulation::Simulation(Dut &dut, CPU &cpu, RunControl &runControl, RetirePipeline &retirePipeline,
                       Logger &logger, SimCounterBank &counters, ITrace &itrace, FTrace &ftrace, IlaEngine &ila)
    : dut(dut), cpu(cpu), runControl(runControl), retirePipeline(retirePipeline),
      logger(logger), counters(counters), itrace(itrace), ftrace(ftrace), ila(ila) {}

void Simulation::init() {
  cpu.init();
  runControl.reset();
  counters.reset();
  cycleCountValue = 0;
  stoppedAtEbreakValue = false;
}

void Simulation::run(uint64_t count) {
  if (stoppedAtEbreakValue) {
    printf("Stopped at ebreak. Use 'fc' to force continue.\n");
    return;
  }
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
    if (ila.finishCycle()) { runControl.stop(); break; }
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
    if (ila.finishCycle()) { runControl.stop(); return; }
    if (checkInterrupt()) return;
  }
  if (!runControl.isRunning()) {
    if (stoppedAtEbreakValue && cpu.hasRetired()) {
      retirePipeline.process(cpu.lastRetire());
      if (runControl.hasEnded()) {
        stoppedAtEbreakValue = false;
        return;
      }

      const uint32_t a0 = cpu.regRead(10);
      printf("\nebreak: stopped, a0: %u (0x%x)\n", a0, a0);
    }
    return;
  }
  retirePipeline.process(cpu.lastRetire());
#else
  dut.stepCycle();
  ++cycleCountValue;
  if (ila.finishCycle()) runControl.stop();
#endif
}

void Simulation::acceptRetire(const RetireEvent &event) {
#ifdef CONFIG_PCTRACE
  logger.writePcTrace("pc: %x\n", event.pc);
#endif
  cpu.acceptRetire(event);
}

void Simulation::handleEbreak() {
  stoppedAtEbreakValue = true;
  runControl.stop();
#ifndef CONFIG_RETIRE_TRACE
  printf("\nebreak: stopped\n");
#endif
}

bool Simulation::resumeFromEbreak() {
  if (!stoppedAtEbreakValue) {
    return false;
  }
  stoppedAtEbreakValue = false;
  return true;
}

bool Simulation::stoppedAtEbreak() const {
  return stoppedAtEbreakValue;
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
