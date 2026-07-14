#include <csignal>

#include "cpu.h"
#include "difftest_runtime.h"
#include "mem.h"
#include "runtime/runtime.h"

#include "sim.h"

#ifdef CONFIG_INTERACTIVE_SDB
#include "readline/readline.h"
#endif

Runtime runtime;
Mem mem;
CPU cpu;

namespace {
void handle_sigint(int) {
  cpu.handleSigint();
}
}

int main(int argc, char **argv) {
  runtime.init(argc, argv);
#ifdef CONFIG_INTERACTIVE_SDB
  rl_catch_signals = 0;
#endif
  signal(SIGINT, handle_sigint);
  runtime.sdb().mainLoop();
  cpu.printStats();
  const int exitStatus = runtime.isExitStatusBad();
  runtime.shutdown();
  return exitStatus;
}
