#include <csignal>

#include "cpu.h"
#include "difftest_runtime.h"
#include "mem.h"
#include "runtime.h"
#include "verilated.h"

#include "monitor.h"
#include "sdb/sdb.h"
#include "sim.h"

#ifdef CONFIG_INTERACTIVE_SDB
#include "readline/readline.h"
#endif

Runtime runtime;
Mem mem;
Difftest difftest;
CPU cpu;

namespace {
void handle_sigint(int) {
  cpu.handleSigint();
}
}

int main(int argc, char **argv) {
  Verilated::commandArgs(argc, argv);
  init_monitor(argc, argv);
#ifdef CONFIG_INTERACTIVE_SDB
  rl_catch_signals = 0;
#endif
  signal(SIGINT, handle_sigint);
  sdb_mainloop();
  cpu.printStats();
  const int exitStatus = runtime.isExitStatusBad();
  runtime.shutdown();
  return exitStatus;
}
