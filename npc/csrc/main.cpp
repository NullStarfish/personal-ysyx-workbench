#include <csignal>
#include <utility>

#include "runtime/execution/interrupt.h"
#include "runtime/runtime.h"

#ifdef CONFIG_INTERACTIVE_SDB
#include <readline/readline.h>
#endif

namespace {
void handleSigint(int) { requestRuntimeInterrupt(); }
}

int main(int argc, char **argv) {
  Runtime runtime(RuntimeOptions::parse(argc, argv));
#ifdef CONFIG_INTERACTIVE_SDB
  rl_catch_signals = 0;
#endif
  signal(SIGINT, handleSigint);
  runtime.init();
  const int exitStatus = runtime.run();
  runtime.shutdown();
  return exitStatus;
}
