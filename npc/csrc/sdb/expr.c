#include <stdint.h>
#include <stdbool.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <regex.h>
#include "sdb.h"
#include "../reg.h"

// --- The rest of the file remains the same, except for one line in eval() ---
enum { TK_NOTYPE = 256, TK_EQ, TK_NEQ, TK_AND, TK_OR, TK_NUMBER, TK_HEX, TK_REG, TK_DEREF, TK_NEG };
static struct rule { const char *regex; int token_type; } rules[] = {
  {" +", TK_NOTYPE}, {"==", TK_EQ}, {"!=", TK_NEQ}, {"&&", TK_AND}, {"\\|\\|", TK_OR},
  {"\\+", '+'}, {"-", '-'}, {"\\*", '*'}, {"/", '/'}, {"\\(", '('}, {"\\)", ')'},
  {"0[xX][0-9a-fA-F]+", TK_HEX}, {"[0-9]+", TK_NUMBER},
  {"\\$((x([0-9]|[1-2][0-9]|3[0-1]))|zero|ra|sp|gp|tp|t[0-6]|s[0-9]|s1[0-1]|a[0-7]|pc)", TK_REG},
};
#define NR_REGEX (sizeof(rules) / sizeof(rules[0]))
static regex_t re[NR_REGEX] = {};
void init_regex() {
  int i; char error_msg[128]; int ret;
  for (i = 0; i < NR_REGEX; i++) {
    ret = regcomp(&re[i], rules[i].regex, REG_EXTENDED);
    if (ret != 0) { regerror(ret, &re[i], error_msg, 128); printf("Regex compilation failed: %s\n%s\n", error_msg, rules[i].regex); exit(1); }
  }
}
typedef struct token { int type; char str[32]; } Token;
static Token tokens[64] = {};
static int nr_token = 0;
static bool make_token(char *e) {
  int position = 0; int i; regmatch_t pmatch; nr_token = 0;
  while (e[position] != '\0') {
    if (nr_token >= 64) { printf("Expression too long\n"); return false; }
    for (i = 0; i < NR_REGEX; i++) {
      if (regexec(&re[i], e + position, 1, &pmatch, 0) == 0 && pmatch.rm_so == 0) {
        char *substr_start = e + position; int substr_len = pmatch.rm_eo;
        position += substr_len;
        if (rules[i].token_type != TK_NOTYPE) {
          if (substr_len >= 32) { printf("Token too long: %.*s\n", substr_len, substr_start); return false; }
          strncpy(tokens[nr_token].str, substr_start, substr_len);
          tokens[nr_token].str[substr_len] = '\0';
          tokens[nr_token].type = rules[i].token_type;
          nr_token++;
        }
        break;
      }
    }
    if (i == NR_REGEX) { printf("No match at position %d\n%s\n%*s^\n", position, e, position, ""); return false; }
  }
  return true;
}
static void pre_token_process() {
    for (int i = 0; i < nr_token; i++) {
        if (tokens[i].type == '*' || tokens[i].type == '-') {
            if (i == 0 || (tokens[i - 1].type != TK_NUMBER && tokens[i - 1].type != TK_HEX && tokens[i-1].type != ')')) {
                if (tokens[i].type == '*') tokens[i].type = TK_DEREF;
                if (tokens[i].type == '-') tokens[i].type = TK_NEG;
            }
        }
    }
}
static int get_priority(int token_type) {
    if (token_type == TK_OR) return 12; if (token_type == TK_AND) return 11;
    if (token_type == TK_EQ || token_type == TK_NEQ) return 7;
    if (token_type == '+' || token_type == '-') return 4;
    if (token_type == '*' || token_type == '/') return 3;
    if (token_type == TK_NEG || token_type == TK_DEREF) return 2;
    return 0;
}
static int find_main_op(int p, int q) {
    int op = -1; int max_priority = -1; int parentheses = 0;
    for (int i = p; i <= q; i++) {
        if (tokens[i].type == '(') { parentheses++; continue; }
        if (tokens[i].type == ')') { parentheses--; continue; }
        if (parentheses != 0) continue;
        int priority = get_priority(tokens[i].type);
        if (priority >= max_priority) { max_priority = priority; op = i; }
    }
    return op;
}
static bool check_parentheses(int p, int q, bool* badexpr) {
    if (tokens[p].type != '(' || tokens[q].type != ')') return false;
    int balance = 0;
    for (int i = p; i <= q; i++) {
        if (tokens[i].type == '(') balance++; else if (tokens[i].type == ')') balance--;
        if (balance == 0 && i < q) return false;
    }
    if (balance != 0) { *badexpr = true; return false; }
    return true;
}
static uint32_t eval(int p, int q, bool* badexpr) {
    if (*badexpr) return 0; if (p > q) { *badexpr = true; return 0; }
    else if (p == q) {
        bool success; uint32_t val;
        if (tokens[p].type == TK_NUMBER) return strtoul(tokens[p].str, NULL, 10);
        // BUG FIX: 让 strtoul 自动处理 "0x" 前缀，更安全
        if (tokens[p].type == TK_HEX) return strtoul(tokens[p].str, NULL, 16);
        if (tokens[p].type == TK_REG) { val = isa_reg_str2val(tokens[p].str + 1, &success); if (!success) *badexpr = true; return val; }
        *badexpr = true; return 0;
    } else if (check_parentheses(p, q, badexpr)) { return eval(p + 1, q - 1, badexpr);
    } else {
        int op = find_main_op(p, q); if (op == -1) { *badexpr = true; return 0; }
        if (tokens[op].type == TK_NEG || tokens[op].type == TK_DEREF) {
            uint32_t val = eval(op + 1, q, badexpr);
            if (tokens[op].type == TK_NEG) return -val; if (tokens[op].type == TK_DEREF) return pmem_read(val);
        }
        uint32_t val1 = eval(p, op - 1, badexpr); uint32_t val2 = eval(op + 1, q, badexpr);
        if (*badexpr) return 0;
        switch (tokens[op].type) {
            case '+': return val1 + val2; case '-': return val1 - val2; case '*': return val1 * val2;
            case '/': if (val2 == 0) { printf("Error: Division by zero\n"); *badexpr = true; return 0; } return val1 / val2;
            case TK_EQ: return val1 == val2; case TK_NEQ: return val1 != val2;
            case TK_AND: return val1 && val2; case TK_OR: return val1 || val2;
            default: *badexpr = true; return 0;
        }
    }
}
uint32_t expr(char *e, bool *success) {
    if (e == NULL) { *success = false; return 0; }
    if (!make_token(e)) { *success = false; return 0; }
    pre_token_process(); bool badexpr = false;
    uint32_t result = eval(0, nr_token - 1, &badexpr);
    *success = !badexpr; return result;
}
