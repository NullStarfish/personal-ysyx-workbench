#include "sdb/sdb.h"

#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <regex.h>

#include "cpu.h"
#include "mem.h"
#include "sim.h"

namespace {
enum {
  TK_NOTYPE = 256,
  TK_EQ,
  TK_NEQ,
  TK_AND,
  TK_OR,
  TK_NUMBER,
  TK_HEX,
  TK_REG,
  TK_DEREF,
  TK_NEG
};

struct Rule {
  const char *regex;
  int tokenType;
};

Rule rules[] = {
    {" +", TK_NOTYPE},
    {"==", TK_EQ},
    {"!=", TK_NEQ},
    {"&&", TK_AND},
    {"\\|\\|", TK_OR},
    {"\\+", '+'},
    {"-", '-'},
    {"\\*", '*'},
    {"/", '/'},
    {"\\(", '('},
    {"\\)", ')'},
    {"0[xX][0-9a-fA-F]+", TK_HEX},
    {"[0-9]+", TK_NUMBER},
    {"\\$((x([0-9]|[1-2][0-9]|3[0-1]))|zero|ra|sp|gp|tp|t[0-6]|s[0-9]|s1[0-1]|a[0-7]|pc)", TK_REG},
};

constexpr size_t kRegexCount = sizeof(rules) / sizeof(rules[0]);

struct Token {
  int type;
  char str[32];
};

regex_t regexRules[kRegexCount];
Token tokens[64];
int tokenCount = 0;

bool make_token(char *exprString) {
  int position = 0;
  tokenCount = 0;
  while (exprString[position] != '\0') {
    if (tokenCount >= 64) {
      printf("Expression too long\n");
      return false;
    }
    regmatch_t match;
    size_t i = 0;
    for (; i < kRegexCount; i++) {
      if (regexec(&regexRules[i], exprString + position, 1, &match, 0) == 0 && match.rm_so == 0) {
        char *substrStart = exprString + position;
        int substrLen = match.rm_eo;
        position += substrLen;
        if (rules[i].tokenType != TK_NOTYPE) {
          if (substrLen >= (int)sizeof(tokens[tokenCount].str)) {
            printf("Token too long: %.*s\n", substrLen, substrStart);
            return false;
          }
          strncpy(tokens[tokenCount].str, substrStart, substrLen);
          tokens[tokenCount].str[substrLen] = '\0';
          tokens[tokenCount].type = rules[i].tokenType;
          tokenCount++;
        }
        break;
      }
    }
    if (i == kRegexCount) {
      printf("No match at position %d\n%s\n%*s^\n", position, exprString, position, "");
      return false;
    }
  }
  return true;
}

void pre_token_process() {
  for (int i = 0; i < tokenCount; i++) {
    if (tokens[i].type == '*' || tokens[i].type == '-') {
      if (i == 0 || (tokens[i - 1].type != TK_NUMBER && tokens[i - 1].type != TK_HEX &&
                     tokens[i - 1].type != TK_REG && tokens[i - 1].type != ')')) {
        if (tokens[i].type == '*') {
          tokens[i].type = TK_DEREF;
        }
        if (tokens[i].type == '-') {
          tokens[i].type = TK_NEG;
        }
      }
    }
  }
}

int get_priority(int tokenType) {
  if (tokenType == TK_OR) return 12;
  if (tokenType == TK_AND) return 11;
  if (tokenType == TK_EQ || tokenType == TK_NEQ) return 7;
  if (tokenType == '+' || tokenType == '-') return 4;
  if (tokenType == '*' || tokenType == '/') return 3;
  if (tokenType == TK_NEG || tokenType == TK_DEREF) return 2;
  return 0;
}

int find_main_op(int p, int q) {
  int op = -1;
  int maxPriority = -1;
  int parentheses = 0;
  for (int i = p; i <= q; i++) {
    if (tokens[i].type == '(') {
      parentheses++;
      continue;
    }
    if (tokens[i].type == ')') {
      parentheses--;
      continue;
    }
    if (parentheses != 0) {
      continue;
    }
    int priority = get_priority(tokens[i].type);
    if (priority >= maxPriority) {
      maxPriority = priority;
      op = i;
    }
  }
  return op;
}

bool check_parentheses(int p, int q, bool *badExpr) {
  if (tokens[p].type != '(' || tokens[q].type != ')') {
    return false;
  }
  int balance = 0;
  for (int i = p; i <= q; i++) {
    if (tokens[i].type == '(') balance++;
    else if (tokens[i].type == ')') balance--;
    if (balance == 0 && i < q) {
      return false;
    }
  }
  if (balance != 0) {
    *badExpr = true;
    return false;
  }
  return true;
}

uint32_t eval(int p, int q, bool *badExpr) {
  if (*badExpr) return 0;
  if (p > q) {
    *badExpr = true;
    return 0;
  }
  if (p == q) {
    bool success = false;
    if (tokens[p].type == TK_NUMBER) return strtoul(tokens[p].str, nullptr, 10);
    if (tokens[p].type == TK_HEX) return strtoul(tokens[p].str, nullptr, 16);
    if (tokens[p].type == TK_REG) return cpu.isa_reg_str2val(tokens[p].str + 1, &success);
    *badExpr = !success;
    return success ? 0 : 0;
  }
  if (check_parentheses(p, q, badExpr)) {
    return eval(p + 1, q - 1, badExpr);
  }

  int op = find_main_op(p, q);
  if (op == -1) {
    *badExpr = true;
    return 0;
  }

  if (tokens[op].type == TK_NEG || tokens[op].type == TK_DEREF) {
    uint32_t value = eval(op + 1, q, badExpr);
    if (tokens[op].type == TK_NEG) return -value;
    if (tokens[op].type == TK_DEREF) return mem.pmemRead(value);
  }

  uint32_t lhs = eval(p, op - 1, badExpr);
  uint32_t rhs = eval(op + 1, q, badExpr);
  if (*badExpr) {
    return 0;
  }

  switch (tokens[op].type) {
    case '+': return lhs + rhs;
    case '-': return lhs - rhs;
    case '*': return lhs * rhs;
    case '/':
      if (rhs == 0) {
        printf("Error: Division by zero\n");
        *badExpr = true;
        return 0;
      }
      return lhs / rhs;
    case TK_EQ: return lhs == rhs;
    case TK_NEQ: return lhs != rhs;
    case TK_AND: return lhs && rhs;
    case TK_OR: return lhs || rhs;
    default:
      *badExpr = true;
      return 0;
  }
}
}

void init_regex() {
  for (size_t i = 0; i < kRegexCount; i++) {
    char errorMsg[128];
    int ret = regcomp(&regexRules[i], rules[i].regex, REG_EXTENDED);
    if (ret != 0) {
      regerror(ret, &regexRules[i], errorMsg, sizeof(errorMsg));
      printf("Regex compilation failed: %s\n%s\n", errorMsg, rules[i].regex);
      exit(1);
    }
  }
}

uint32_t expr(char *e, bool *success) {
  if (e == nullptr) {
    *success = false;
    return 0;
  }
  if (!make_token(e)) {
    *success = false;
    return 0;
  }
  pre_token_process();
  bool badExpr = false;
  uint32_t result = eval(0, tokenCount - 1, &badExpr);
  *success = !badExpr;
  return result;
}
