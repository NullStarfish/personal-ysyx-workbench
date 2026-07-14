#include "runtime/runtime.h"

#include <cstdio>
#include <cstdlib>
#include <getopt.h>
#include <utility>

#include <verilated.h>

#include "runtime/base/cpu.h"
#include "runtime/base/host_clock.h"
#include "runtime/base/run_control.h"
#include "runtime/dpi/dpi_bridge.h"
#include "runtime/execution/interrupt.h"
#include "runtime/execution/retire_pipeline.h"
#include "runtime/execution/simulation.h"
#include "runtime/platform/dut.h"
#include "runtime/platform/memory.h"
#include "runtime/platform/program_image.h"
#include "runtime/sdb/sdb.h"
#include "runtime/sdb/watchpoint.h"
#include "runtime/services/difftest.h"
#include "runtime/services/logger.h"
#include "runtime/services/sim_counter.h"
#include "runtime/traces/ftrace.h"
#include "runtime/traces/itrace.h"

RuntimeOptions RuntimeOptions::parse(int argc, char *argv[]) {
  Verilated::commandArgs(argc, argv);
  RuntimeOptions options;
  const option table[] = {
      {"batch", no_argument, nullptr, 'b'},
      {"log", required_argument, nullptr, 'l'},
      {"diff", required_argument, nullptr, 'd'},
      {"ftrace", required_argument, nullptr, 'f'},
      {"help", no_argument, nullptr, 'h'},
      {"pc-trace", required_argument, nullptr, 'p'},
      {0, 0, nullptr, 0},
  };

  int opt = 0;
  while ((opt = getopt_long(argc, argv, "-bl:d:f:h:p:", table, nullptr)) != -1) {
    switch (opt) {
      case 'b': options.batchMode = true; break;
      case 'l': options.logFile = optarg; break;
      case 'd': options.diffSoFile = optarg; break;
      case 'f': options.elfFile = optarg; break;
      case 'p': options.pcTraceFile = optarg; break;
      case 'h':
        printf("pass img file or opts to the executable\n");
        exit(0);
      case 1:
        if (options.imageFile.empty()) options.imageFile = optarg;
        break;
      default: exit(0);
    }
  }
  if (options.imageFile.empty() && optind < argc) options.imageFile = argv[optind];
  return options;
}

class Runtime::Impl {
public:
  explicit Impl(RuntimeOptions options)
      : options(std::move(options)), memory(clock), itrace(logger),
        difftest(cpu, memory), watchpoints(cpu, memory),
        retirePipeline(itrace, ftrace, difftest, watchpoints, runControl),
        program(this->options.imageFile),
        simulation(dut, cpu, runControl, retirePipeline, logger, counters, itrace, ftrace),
        dpiBridge(simulation, memory, difftest, counters),
        sdb(simulation, cpu, memory, watchpoints, ftrace, dut, program, runControl) {
    logger.setLogFile(this->options.logFile.c_str());
    logger.setPcTraceFile(this->options.pcTraceFile.c_str());
    ftrace.setElfFile(this->options.elfFile.c_str());
    difftest.setRefSoFile(this->options.diffSoFile.c_str());
    if (this->options.batchMode) sdb.setBatchMode();
  }

  void init() {
    if (initialized) return;
    logger.init();
    clock.reset();
    itrace.init();
    ftrace.init();
    simulation.init();
    watchpoints.init();
    dpiBridge.bind();
    dut.init();
    program.init(memory);
    dut.reset(100);
    printf("CPU reset complete.\n");
    difftest.init(static_cast<long>(program.size()));
    sdb.init();
    printf("Welcome to the RISC-V NPC simulator!\nFor help, type \"help\"\nThe current img is %s\n",
           program.path() == nullptr ? "(none)" : program.path());
    initialized = true;
  }

  int run() {
    sdb.mainLoop();
    if (consumeRuntimeInterrupt()) simulation.handleInterrupt();
    simulation.printStats();
    return runControl.exitStatus();
  }

  void shutdown() {
    if (!initialized) return;
    difftest.shutdown();
    dut.shutdown();
    dpiBridge.unbind();
    logger.shutdown();
    initialized = false;
  }

  RuntimeOptions options;
  Logger logger;
  HostClock clock;
  Memory memory;
  CPU cpu;
  RunControl runControl;
  SimCounterBank counters;
  ITrace itrace;
  FTrace ftrace;
  Difftest difftest;
  WatchpointManager watchpoints;
  RetirePipeline retirePipeline;
  Dut dut;
  ProgramImage program;
  Simulation simulation;
  DpiBridge dpiBridge;
  Sdb sdb;
  bool initialized = false;
};

Runtime::Runtime(RuntimeOptions options) : impl(std::make_unique<Impl>(std::move(options))) {}
Runtime::~Runtime() { shutdown(); }
void Runtime::init() { impl->init(); }
int Runtime::run() { return impl->run(); }
void Runtime::shutdown() { impl->shutdown(); }
