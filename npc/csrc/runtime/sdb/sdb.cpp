#include "runtime/sdb/sdb.h"

#include <algorithm>
#include <array>
#include <cstdint>
#include <cstdio>
#include <cstdlib>
#include <cstring>

#ifdef CONFIG_INTERACTIVE_SDB
#include <readline/readline.h>
#endif

#ifdef CONFIG_BOARD
#include <nvboard.h>
#endif

#include "runtime/base/cpu.h"
#include "runtime/base/run_control.h"
#include "runtime/execution/simulation.h"
#include "runtime/platform/dut.h"
#include "runtime/platform/memory.h"
#include "runtime/platform/program_image.h"
#include "runtime/services/ila_engine.h"
#include "runtime/sdb/watchpoint.h"
#include "runtime/traces/ftrace.h"

#ifdef CONFIG_BOARD
void read_event();
#endif

namespace {
#ifdef CONFIG_BOARD
// nvboard_update() also advances UART/PS2 timing and must only run with the
// simulated clock. While SDB is stopped, poll SDL events without advancing
// any board-side protocol state.
int nvboardReadlineEventHook() {
  read_event();
  return 0;
}
#endif
}

class Sdb::Impl {
public:
  Impl(Simulation &simulation, CPU &cpu, Memory &memory, WatchpointManager &watchpoints,
       FTrace &ftrace, Dut &dut, ProgramImage &program, RunControl &runControl, IlaEngine &ila)
      : simulation(simulation), cpu(cpu), memory(memory), watchpoints(watchpoints),
        ftrace(ftrace), dut(dut), program(program), runControl(runControl), ila(ila) {}

  using CommandHandler = int (Impl::*)(char *);
  struct CommandEntry { const char *name; const char *description; CommandHandler handler; };

  static const std::array<CommandEntry, 14> &commands() {
    static const std::array<CommandEntry, 14> table{{
        {"help", "Display information about all supported commands", &Impl::cmdHelp},
        {"c", "Continue the execution of the program", &Impl::cmdContinue},
        {"fc", "Force continue after an ebreak stop", &Impl::cmdForceContinue},
        {"fsi", "Force step [N] instructions after an ebreak stop (default 1)", &Impl::cmdForceStep},
        {"q", "Exit the simulator", &Impl::cmdQuit},
        {"si", "Step forward [N] instructions (default 1)", &Impl::cmdStep},
        {"info", "Print program state (r for registers, w for watchpoints)", &Impl::cmdInfo},
        {"x", "Scan memory: x N EXPR", &Impl::cmdExamine},
        {"p", "Evaluate expression: p EXPR", &Impl::cmdPrint},
        {"w", "Set a watchpoint: w EXPR", &Impl::cmdWatch},
        {"d", "Delete a watchpoint: d N", &Impl::cmdDelete},
        {"bt", "Print the current function call stack from ftrace", &Impl::cmdBacktrace},
        {"vcd", "Control interval VCD tracing: vcd watch start [FILE] | end | status", &Impl::cmdVcd},
        {"ila", "Control DPI-ILA: ila status|trigger", &Impl::cmdIla},
    }};
    return table;
  }

  const CommandEntry *findCommand(const char *name) const {
    const auto &table = commands();
    const auto found = std::find_if(table.begin(), table.end(),
                                    [name](const CommandEntry &entry) { return strcmp(name, entry.name) == 0; });
    return found == table.end() ? nullptr : &*found;
  }

  void init() { printf("SDB initialized. Ready for debugging.\n"); }

  void mainLoop() {
    if (batchMode) {
      cmdContinue(nullptr);
      return;
    }
#ifdef CONFIG_INTERACTIVE_SDB
#ifdef CONFIG_BOARD
    rl_set_keyboard_input_timeout(1000);
    rl_event_hook = nvboardReadlineEventHook;
#endif
    for (char *line; (line = readline("(npc) ")) != nullptr;) {
      char *command = strtok(line, " ");
      if (command == nullptr) { free(line); continue; }
#ifdef CONFIG_BOARD
      read_event();
#endif
      char *args = strtok(nullptr, "");
      const CommandEntry *entry = findCommand(command);
      if (entry == nullptr) printf("Unknown command '%s'\n", command);
      else if ((this->*entry->handler)(args) < 0) { free(line); return; }
      free(line);
    }
#else
    simulation.run(static_cast<uint64_t>(-1));
#endif
  }

  int cmdHelp(char *args) {
    char *name = args == nullptr ? nullptr : strtok(args, " ");
    if (name == nullptr) {
      for (const CommandEntry &entry : commands()) printf("%-5s - %s\n", entry.name, entry.description);
      return 0;
    }
    const CommandEntry *entry = findCommand(name);
    if (entry == nullptr) printf("Unknown command '%s'\n", name);
    else printf("%s - %s\n", entry->name, entry->description);
    return 0;
  }

  int cmdContinue(char *) {
    ila.prepareRun();
    simulation.run(static_cast<uint64_t>(-1));
    return 0;
  }

  int cmdForceContinue(char *) {
    if (!simulation.resumeFromEbreak()) {
      printf("The simulator is not stopped at an ebreak.\n");
      return 0;
    }

    ila.prepareRun();
    simulation.run(static_cast<uint64_t>(-1));
    return 0;
  }

  int cmdForceStep(char *args) {
    if (!simulation.resumeFromEbreak()) {
      printf("The simulator is not stopped at an ebreak.\n");
      return 0;
    }

    char *end = nullptr;
    long count = args == nullptr ? 1 : strtol(args, &end, 10);
    if (args != nullptr && end != nullptr && *end != '\0') count = 1;

    ila.prepareRun();
    simulation.run(count);
    return 0;
  }

  int cmdQuit(char *) {
    runControl.quit();
    printf("file %s quit\n", program.path() == nullptr ? "(none)" : program.path());
    return -1;
  }
  int cmdStep(char *args) {
    char *end = nullptr;
    long count = args == nullptr ? 1 : strtol(args, &end, 10);
    if (args != nullptr && end != nullptr && *end != '\0') count = 1;
    ila.prepareRun();
    simulation.run(count);
    return 0;
  }
  int cmdInfo(char *args) {
    if (args == nullptr) printf("Usage: info r|w\n");
    else if (strcmp(args, "r") == 0) cpu.isa_reg_display();
    else if (strcmp(args, "w") == 0) watchpoints.display();
    else printf("Unknown argument for 'info': %s\n", args);
    return 0;
  }
  int cmdExamine(char *args) {
    if (args == nullptr) { printf("Usage: x N EXPR\n"); return 0; }
    char *countText = strtok(args, " ");
    char *expression = countText == nullptr ? nullptr : strtok(nullptr, "");
    if (expression == nullptr) { printf("Usage: x N EXPR\n"); return 0; }
    bool success = false;
    const int count = strtol(countText, nullptr, 10);
    const uint32_t address = watchpoints.evaluate(expression, &success);
    if (!success) { printf("Invalid expression: %s\n", expression); return 0; }
    printf("Scanning %d words from address 0x%x:\n", count, address);
    for (int i = 0; i < count; ++i) {
      printf("0x%08x: 0x%08x\n", address + i * 4, memory.pmemRead(address + i * 4));
    }
    return 0;
  }
  int cmdPrint(char *args) {
    if (args == nullptr) { printf("Usage: p EXPR\n"); return 0; }
    bool success = false;
    const uint32_t result = watchpoints.evaluate(args, &success);
    if (success) printf("%s = %u (0x%x)\n", args, result, result);
    else printf("Invalid expression\n");
    return 0;
  }
  int cmdWatch(char *args) {
    if (args == nullptr) printf("Usage: w EXPR\n");
    else watchpoints.add(args);
    return 0;
  }
  int cmdDelete(char *args) {
    if (args == nullptr) printf("Usage: d N\n");
    else watchpoints.remove(strtol(args, nullptr, 10));
    return 0;
  }
  int cmdBacktrace(char *) {
#ifdef CONFIG_FTRACE
    if (!ftrace.enabled()) printf("Backtrace unavailable: start NPC with --ftrace=FILE using an ELF with symbols.\n");
    else ftrace.printStack();
#else
    printf("Backtrace unavailable: rebuild with CONFIG_FTRACE=y and start NPC with --ftrace=FILE.\n");
#endif
    return 0;
  }
  int cmdVcd(char *args) {
    char *scope = args == nullptr ? nullptr : strtok(args, " ");
    char *action = scope == nullptr ? nullptr : strtok(nullptr, " ");
    char *filename = action == nullptr ? nullptr : strtok(nullptr, " ");
    if (scope == nullptr || strcmp(scope, "watch") != 0 || action == nullptr) {
      printf("Usage: vcd watch start [FILE] | end | status\n");
      return 0;
    }
    if (strcmp(action, "start") == 0) {
      if (dut.isVcdWatching()) printf("VCD watch is already active (%s)\n", dut.vcdPath());
      else if (dut.startVcdWatch(filename)) printf("VCD watch started: %s\n", dut.vcdPath());
    } else if (strcmp(action, "end") == 0) {
      if (dut.endVcdWatch()) printf("VCD watch stopped: %s\n", dut.vcdPath());
      else printf("VCD watch is not active\n");
    } else if (strcmp(action, "status") == 0) {
      const char *path = dut.vcdPath();
      printf("VCD watch: %s", dut.isVcdWatching() ? "active" : "inactive");
      if (path != nullptr) printf(" (%s)", path);
      printf("\n");
    } else {
      printf("Usage: vcd watch start [FILE] | end | status\n");
    }
    return 0;
  }


  int cmdIla(char *args) {
    char *action = args == nullptr ? nullptr : strtok(args, " ");
    char *rest = action == nullptr ? nullptr : strtok(nullptr, "");
    if (action == nullptr) {
      printf("Usage: ila status [CAPTURE|all] | trigger on|off [CAPTURE|all]\n");
      return 0;
    }
    if (strcmp(action, "status") == 0) {
      ila.printStatus(rest == nullptr ? "all" : rest);
      return 0;
    }
    if (strcmp(action, "trigger") == 0) {
      char *mode = rest == nullptr ? nullptr : strtok(rest, " ");
      char *target = mode == nullptr ? nullptr : strtok(nullptr, " ");
      if (mode == nullptr || (strcmp(mode, "on") != 0 && strcmp(mode, "off") != 0)) {
        printf("Usage: ila trigger on|off [CAPTURE|all]\n");
        return 0;
      }
      std::string error;
      const bool enabled = strcmp(mode, "on") == 0;
      if (ila.setTrigger(target == nullptr ? "all" : target, enabled, error))
        printf("ILA trigger %s: %s\n", enabled ? "enabled" : "disabled", target == nullptr ? "all" : target);
      else printf("ILA trigger error: %s\n", error.c_str());
      return 0;
    }
    printf("Usage: ila status [CAPTURE|all] | trigger on|off [CAPTURE|all]\n");
    return 0;
  }

  Simulation &simulation;
  CPU &cpu;
  Memory &memory;
  WatchpointManager &watchpoints;
  FTrace &ftrace;
  Dut &dut;
  ProgramImage &program;
  RunControl &runControl;
  IlaEngine &ila;
  bool batchMode = false;
};

Sdb::Sdb(Simulation &simulation, CPU &cpu, Memory &memory, WatchpointManager &watchpoints,
         FTrace &ftrace, Dut &dut, ProgramImage &program, RunControl &runControl, IlaEngine &ila)
    : impl(std::make_unique<Impl>(simulation, cpu, memory, watchpoints, ftrace, dut, program, runControl, ila)) {}
Sdb::~Sdb() = default;
void Sdb::init() { impl->init(); }
void Sdb::mainLoop() { impl->mainLoop(); }
void Sdb::setBatchMode() { impl->batchMode = true; }
