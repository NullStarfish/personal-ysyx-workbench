/***************************************************************************************
* Copyright (c) 2014-2024 Zihao Yu, Nanjing University
*
* NEMU is licensed under Mulan PSL v2.
* You can use this software according to the terms and conditions of the Mulan PSL v2.
* You may obtain a copy of Mulan PSL v2 at:
*          http://license.coscl.org.cn/MulanPSL2
*
* THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
* EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
* MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
*
* See the Mulan PSL v2 for more details.
***************************************************************************************/

#include <isa.h>
#include <cpu/cpu.h>
#include <readline/readline.h>
#include <readline/history.h>
#include "sdb.h"
#include "memory/paddr.h"
#include "memory/vaddr.h"



static int is_batch_mode = false;

void init_regex();
void init_wp_pool();

typedef struct watchpoint {
  int NO;
  struct watchpoint *next;
  struct watchpoint *last;
  
  /* TODO: Add more members if necessary */
  char *expr;
  uint32_t old_val;

} WP;
WP* new_wp(char *e);
void free_wp(WP *wp);
void wp_remove(int NO);
void display_wp();
void wp_add(char *str);

/* We use the `readline' library to provide more flexibility to read from stdin. */
static char* rl_gets() {
  static char *line_read = NULL;

  if (line_read) {
    free(line_read);
    line_read = NULL;
  }

  line_read = readline("(nemu) ");

  if (line_read && *line_read) {
    add_history(line_read);
  }

  return line_read;
}

static int cmd_c(char *args) {
  cpu_exec(-1);
  return 0;
}


static int cmd_q(char *args) {
  nemu_state.state = NEMU_QUIT;
  return -1;
}

static int cmd_help(char *args);

static int cmd_si(char *args);

static int cmd_info(char *args);

static int cmd_x(char *args);

static int cmd_p(char *args);

static int cmd_w(char *args);

static int cmd_d(char *args);

static struct {
  const char *name;
  const char *description;
  int (*handler) (char *);
} cmd_table [] = {
  /* TODO: Add more commands */
  { "help", "Display information about all supported commands", cmd_help },
  { "c", "Continue the execution of the program", cmd_c },
  { "q", "Exit NEMU", cmd_q },
  {"si", "step forward [n] times", cmd_si},
  {"info", "print information", cmd_info},
  {"x", "scan memory", cmd_x},
  {"p", "process expressions", cmd_p},
  {"w", "set watchpoints", cmd_w},
  {"d", "delete watchpoint", cmd_d}
};

#define NR_CMD ARRLEN(cmd_table)

static int cmd_help(char *args) {
  /* extract the first argument */
  char *arg = strtok(NULL, " ");
  int i;

  if (arg == NULL) {
    /* no argument given */
    for (i = 0; i < NR_CMD; i ++) {
      printf("%s - %s\n", cmd_table[i].name, cmd_table[i].description);
    }
  }
  else {
    for (i = 0; i < NR_CMD; i ++) {
      if (strcmp(arg, cmd_table[i].name) == 0) {
        printf("%s - %s\n", cmd_table[i].name, cmd_table[i].description);
        return 0;
      }
    }
    printf("Unknown command '%s'\n", arg);
  }
  return 0;
}

static int cmd_si(char *args) {
  int n = 1;
  if (args != NULL) {
    n = atoi(args);
  }
  cpu_exec(n);
  return 0;
}

static int cmd_info(char *args) {
  if (args == NULL) {
    printf("Usage: info r|w\n");
    return 0;
  }

  if (strcmp(args, "r") == 0) {
    isa_reg_display();
    printf("$pc:%x\n", cpu.pc);
  }
  else if (strcmp(args, "w") == 0) {
    display_wp();
  }
  else {
    printf("Unknown command '%s'\n", args);
  }
  return 0;
}

static int cmd_x(char *args) {
  if (args == NULL) {
    printf("Usage: x [number of chunks] [start address of the memory]\n");
    return 0;
  }
  int N;
  char expr_buf[65536 + 10];
  int ret = sscanf(args, "%d %[^\n]", &N, expr_buf);


  if (ret != 2) {
    printf("Invalid format\n");
    return 0;
  }


  bool success = true;
  word_t startAddress = expr(expr_buf, &success);
  if (!success) {
    printf("BAD EXPRESSION!\n");
  }
  
  for (int i = 0; i < N; i ++) {
    printf("%10x:%10x\n", startAddress + i * 4, vaddr_read(startAddress + i * 4, 4));
  }

  return 0;
}

static int cmd_p(char *args) {
  bool success;
  int ret =  expr(args, &success);
  if (!success) printf("bad expression!\n");
  else printf("%u\n", ret);
  return success;
}

static int cmd_w(char *args) {
  if (args == NULL) {
    printf("Usage: w + expression\n");
    return 0;
  }
  wp_add(args);
  return 0;
}

static int cmd_d(char *args) {
  bool success = false;
  int NO = expr(args, &success);
  if (!success) {
    printf("BAD expr!");
    return 0;
  }
  //printf("start remove\n");
  wp_remove(NO);
  return 0;
}

void sdb_set_batch_mode() {
  is_batch_mode = true;
}

void sdb_mainloop() {
  if (is_batch_mode) {
    cmd_c(NULL);
    return;
  }

  for (char *str; (str = rl_gets()) != NULL; ) {
    char *str_end = str + strlen(str);

    /* extract the first token as the command */
    char *cmd = strtok(str, " ");
    if (cmd == NULL) { continue; }

    /* treat the remaining string as the arguments,
     * which may need further parsing
     */
    char *args = cmd + strlen(cmd) + 1;
    if (args >= str_end) {
      args = NULL;
    }

#ifdef CONFIG_DEVICE
    extern void sdl_clear_event_queue();
    sdl_clear_event_queue();
#endif

    int i;
    for (i = 0; i < NR_CMD; i ++) {
      if (strcmp(cmd, cmd_table[i].name) == 0) {
        if (cmd_table[i].handler(args) < 0) { return; }
        break;
      }
    }

    if (i == NR_CMD) { printf("Unknown command '%s'\n", cmd); }
  }
}

void init_sdb() {
  /* Compile the regular expressions. */
  init_regex();

  /* Initialize the watchpoint pool. */
  init_wp_pool();
}
