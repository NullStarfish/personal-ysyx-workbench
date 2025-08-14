#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <readline/readline.h>
#include <readline/history.h>
#include "sdb.h"
#include "../reg.h" // 依赖 reg.h 获取寄存器函数

// --- Command handlers and other functions remain the same ---
static bool is_batch_mode = false;
static int cmd_c(char *args) { cpu_exec(-1); return 0; }
static int cmd_q(char *args) { return -1; }
static int cmd_help(char *args);
static int cmd_si(char *args) {
    char *endptr;
    long n = (args == NULL) ? 1 : strtol(args, &endptr, 10);
    if (args != NULL && *endptr != '\0') { n = 1; }
    cpu_exec(n);
    return 0;
}
static int cmd_info(char *args) {
    if (args == NULL) { printf("Usage: info r|w\n"); return 0; }
    if (strcmp(args, "r") == 0) { isa_reg_display(); }
    else if (strcmp(args, "w") == 0) { display_wp(); }
    else { printf("Unknown argument for 'info': %s\n", args); }
    return 0;
}
static int cmd_x(char *args) {
    if (args == NULL) { printf("Usage: x N EXPR\n"); return 0; }
    char *n_str = strtok(args, " ");
    char *expr_str = n_str ? strtok(NULL, "") : NULL;
    if (expr_str == NULL) { printf("Usage: x N EXPR\n"); return 0; }
    bool success;
    int n = strtol(n_str, NULL, 10);
    uint32_t addr = expr(expr_str, &success);
    if (!success) { printf("Invalid expression: %s\n", expr_str); return 0; }
    printf("Scanning %d words from address 0x%x:\n", n, addr);
    for (int i = 0; i < n; i++) {
        printf("0x%08x: 0x%08x\n", addr + i * 4, paddr_read(addr + i * 4));
    }
    return 0;
}
static int cmd_p(char *args) {
    if (args == NULL) { printf("Usage: p EXPR\n"); return 0; }
    bool success;
    uint32_t result = expr(args, &success);
    if (success) { printf("%s = %u (0x%x)\n", args, result, result); }
    else { printf("Invalid expression\n"); }
    return 0;
}
static int cmd_w(char *args) { wp_add(args); return 0; }
static int cmd_d(char *args) {
    if (args == NULL) { printf("Usage: d N\n"); return 0; }
    int no = strtol(args, NULL, 10);
    wp_remove(no);
    return 0;
}
static struct { const char *name; const char *description; int (*handler) (char *); } cmd_table[] = {
    { "help", "Display information about all supported commands", cmd_help },
    { "c", "Continue the execution of the program", cmd_c },
    { "q", "Exit the simulator", cmd_q },
    { "si", "Step forward [N] instructions (default 1)", cmd_si },
    { "info", "Print program state (r for registers, w for watchpoints)", cmd_info },
    { "x", "Scan memory: x N EXPR", cmd_x },
    { "p", "Evaluate expression: p EXPR", cmd_p },
    { "w", "Set a watchpoint: w EXPR", cmd_w },
    { "d", "Delete a watchpoint: d N", cmd_d },
};
#define NR_CMD (sizeof(cmd_table) / sizeof(cmd_table[0]))
static int cmd_help(char *args) {
    char *arg = strtok(args, " ");
    if (arg == NULL) { for (size_t i = 0; i < NR_CMD; i++) { printf("%-5s - %s\n", cmd_table[i].name, cmd_table[i].description); } }
    else { for (size_t i = 0; i < NR_CMD; i++) { if (strcmp(arg, cmd_table[i].name) == 0) { printf("%s - %s\n", cmd_table[i].name, cmd_table[i].description); return 0; } } printf("Unknown command '%s'\n", arg); }
    return 0;
}
void sdb_set_batch_mode() { is_batch_mode = true; }
void sdb_mainloop() {
    if (is_batch_mode) { cmd_c(NULL); return; }
    for (char *str; (str = readline("(npc) ")) != NULL; ) {
        char *str_end = str + strlen(str);
        char *cmd = strtok(str, " ");
        if (cmd == NULL) { continue; }
        char *args = cmd + strlen(cmd) + 1;
        if (args >= str_end) { args = NULL; }
        size_t i;
        for (i = 0; i < NR_CMD; i++) { if (strcmp(cmd, cmd_table[i].name) == 0) { if (cmd_table[i].handler(args) < 0) { free(str); return; } break; } }
        if (i == NR_CMD) { printf("Unknown command '%s'\n", cmd); }
        free(str);
    }
}
void init_sdb() { init_regex(); init_wp_pool(); printf("SDB initialized. Ready for debugging.\n"); }
