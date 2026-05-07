#include "sdb/sdb.h"

#include <cstdio>
#include <cstdlib>
#include <cstring>

#include <readline/history.h>
#include <readline/readline.h>

#include "cpu.h"
#include "mem.h"
#include "runtime.h"
#include "sim.h"

extern "C" void nvboard_flush();

namespace {
bool batchMode = false;

#ifdef CONFIG_BOARD
int nvboard_readline_event_hook() {
  nvboard_flush();
  return 0;
}
#endif

using CommandHandler = int (*)(char *);

int cmd_help(char *args);

int cmd_c(char *args) {
  (void)args;
  cpu.exec(static_cast<uint64_t>(-1));
  return 0;
}

int cmd_q(char *args) {
  (void)args;
  runtime.setQuit();
  printf("file %s quit\n", img_file);
  return -1;
}

int cmd_si(char *args) {
  char *endptr = nullptr;
  long n = (args == nullptr) ? 1 : strtol(args, &endptr, 10);
  if (args != nullptr && endptr != nullptr && *endptr != '\0') {
    n = 1;
  }
  cpu.exec(n);
  return 0;
}

int cmd_info(char *args) {
  if (args == nullptr) {
    printf("Usage: info r|w\n");
    return 0;
  }
  if (strcmp(args, "r") == 0) {
    cpu.isa_reg_display();
  } else if (strcmp(args, "w") == 0) {
    display_wp();
  } else {
    printf("Unknown argument for 'info': %s\n", args);
  }
  return 0;
}

int cmd_x(char *args) {
  if (args == nullptr) {
    printf("Usage: x N EXPR\n");
    return 0;
  }
  char *countString = strtok(args, " ");
  char *exprString = countString ? strtok(nullptr, "") : nullptr;
  if (exprString == nullptr) {
    printf("Usage: x N EXPR\n");
    return 0;
  }
  bool success = false;
  int n = strtol(countString, nullptr, 10);
  uint32_t addr = expr(exprString, &success);
  if (!success) {
    printf("Invalid expression: %s\n", exprString);
    return 0;
  }
  printf("Scanning %d words from address 0x%x:\n", n, addr);
  for (int i = 0; i < n; i++) {
    printf("0x%08x: 0x%08x\n", addr + i * 4, mem.pmemRead(addr + i * 4));
  }
  return 0;
}

int cmd_p(char *args) {
  if (args == nullptr) {
    printf("Usage: p EXPR\n");
    return 0;
  }
  bool success = false;
  uint32_t result = expr(args, &success);
  if (success) {
    printf("%s = %u (0x%x)\n", args, result, result);
  } else {
    printf("Invalid expression\n");
  }
  return 0;
}

int cmd_w(char *args) {
  wp_add(args);
  return 0;
}

int cmd_d(char *args) {
  if (args == nullptr) {
    printf("Usage: d N\n");
    return 0;
  }
  wp_remove(strtol(args, nullptr, 10));
  return 0;
}

struct CommandEntry {
  const char *name;
  const char *description;
  CommandHandler handler;
};

CommandEntry commandTable[] = {
    {"help", "Display information about all supported commands", cmd_help},
    {"c", "Continue the execution of the program", cmd_c},
    {"q", "Exit the simulator", cmd_q},
    {"si", "Step forward [N] instructions (default 1)", cmd_si},
    {"info", "Print program state (r for registers, w for watchpoints)", cmd_info},
    {"x", "Scan memory: x N EXPR", cmd_x},
    {"p", "Evaluate expression: p EXPR", cmd_p},
    {"w", "Set a watchpoint: w EXPR", cmd_w},
    {"d", "Delete a watchpoint: d N", cmd_d},
};

constexpr size_t kCommandCount = sizeof(commandTable) / sizeof(commandTable[0]);

int cmd_help(char *args) {
  char *arg = (args == nullptr) ? nullptr : strtok(args, " ");
  if (arg == nullptr) {
    for (size_t i = 0; i < kCommandCount; i++) {
      printf("%-5s - %s\n", commandTable[i].name, commandTable[i].description);
    }
    return 0;
  }
  for (size_t i = 0; i < kCommandCount; i++) {
    if (strcmp(arg, commandTable[i].name) == 0) {
      printf("%s - %s\n", commandTable[i].name, commandTable[i].description);
      return 0;
    }
  }
  printf("Unknown command '%s'\n", arg);
  return 0;
}
}

void sdb_set_batch_mode() {
  batchMode = true;
}

void sdb_mainloop() {
  if (batchMode) {
    cmd_c(nullptr);
    return;
  }
#ifdef CONFIG_BOARD
  rl_set_keyboard_input_timeout(1000);
  rl_event_hook = nvboard_readline_event_hook;
#endif
  for (char *line; (line = readline("(npc) ")) != nullptr;) {
    char *cmd = strtok(line, " ");
    if (cmd == nullptr) {
      free(line);
      continue;
    }
#ifdef CONFIG_BOARD
    nvboard_flush();
#endif
    char *args = strtok(nullptr, "");
    size_t i = 0;
    for (; i < kCommandCount; i++) {
      if (strcmp(cmd, commandTable[i].name) == 0) {
        if (commandTable[i].handler(args) < 0) {
          free(line);
          return;
        }
        break;
      }
    }
    if (i == kCommandCount) {
      printf("Unknown command '%s'\n", cmd);
    }
    free(line);
  }
}

void init_sdb() {
  init_regex();
#ifdef CONFIG_WATCHPOINT
  printf("Watchpoint is ON\n");
  init_wp_pool();
#endif
  printf("SDB initialized. Ready for debugging.\n");
}
