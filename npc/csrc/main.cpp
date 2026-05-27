#include <csignal>

#include "cpu.h"
#include "difftest_runtime.h"
#include "mem.h"
#include "runtime.h"
#include "verilated.h"

#include "monitor.h"
#include "readline/readline.h"
#include "sdb/sdb.h"
#include "sim.h"

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
  rl_catch_signals = 0;
  signal(SIGINT, handle_sigint);
  sdb_mainloop();
  cpu.printStats();
  const int exitStatus = runtime.isExitStatusBad();
  runtime.shutdown();
  return exitStatus;
}
