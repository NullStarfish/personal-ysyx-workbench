#include <regex>
#include <string>
#include <vector>
#include <iostream>
#include <cstdint>
#include <cstring>
#include "sdb.h"

// --- Tokenization ---

enum {
  TK_NOTYPE = 256, TK_EQ,
  TK_NUMBER, TK_HEX, TK_REG,
  TK_NEG, TK_POS, TK_DEREF, TK_NEQ, TK_LE, TK_GE, TK_LT, TK_GT,
  TK_AND, TK_OR, TK_NOT
};

static struct rule {
  std::string regex;
  int token_type;
} rules[] = {
  {" +", TK_NOTYPE},
  {"==", TK_EQ},
  {"!=", TK_NEQ},
  {"<=", TK_LE},
  {">=", TK_GE},
  {"<", TK_LT},
  {">", TK_GT},
  {"&&", TK_AND},
  {"\\|\\|", TK_OR},
  {"!", TK_NOT},
  {"\\+", '+'},
  {"-", '-'},
  {"\\*", '*'},
  {"/", '/'},
  {"\\(", '('},
  {"\\)", ')'},
  {"0[xX][0-9a-fA-F]+", TK_HEX},
  {"[0-9]+", TK_NUMBER},
  {"\\$(([0-9]|[1-2][0-9]|3[0-1])|pc|ra|sp|gp|tp|t[0-6]|a[0-7]|s([0-9]|10|11)|zero)", TK_REG},
};

#define NR_REGEX (sizeof(rules) / sizeof(rules[0]))

static std::vector<std::regex> re_vec;

void init_regex() {
  for (unsigned i = 0; i < NR_REGEX; i++) {
    re_vec.emplace_back(rules[i].regex, std::regex::extended);
  }
}

typedef struct token {
  int type;
  char str[32];
} Token;

static std::vector<Token> tokens;

static bool make_token(char *e) {
  tokens.clear();
  std::string s(e);
  std::smatch match;

  while (!s.empty()) {
    bool found = false;
    for (unsigned i = 0; i < NR_REGEX; i++) {
      if (std::regex_search(s, match, re_vec[i]) && match.prefix().length() == 0) {
        if (rules[i].token_type != TK_NOTYPE) {
          Token new_token;
          new_token.type = rules[i].token_type;
          strncpy(new_token.str, match.str().c_str(), 31);
          new_token.str[31] = '\0';
          tokens.push_back(new_token);
        }
        s = match.suffix().str();
        found = true;
        break;
      }
    }
    if (!found) {
      printf("No match at the beginning of '%s'\n", s.c_str());
      return false;
    }
  }
  return true;
}

// --- Expression Evaluation ---

uint32_t eval(int p, int q, bool* badexpr); // Forward declaration

void pre_token_process() {
  for (size_t i = 0; i < tokens.size(); i++) {
    if (tokens[i].type == '+' || tokens[i].type == '-' || tokens[i].type == '*') {
      if (i == 0 || (tokens[i - 1].type != TK_NUMBER && tokens[i - 1].type != TK_HEX && tokens[i - 1].type != ')' && tokens[i - 1].type != TK_REG)) {
        switch (tokens[i].type) {
          case '+': tokens[i].type = TK_POS; break;
          case '-': tokens[i].type = TK_NEG; break;
          case '*': tokens[i].type = TK_DEREF; break;
          default: break;
        }
      }
    }
  }
}

int get_priority(int token_type) {
  if (token_type == TK_OR) return 12;
  if (token_type == TK_AND) return 11;
  if (token_type == TK_EQ || token_type == TK_NEQ) return 7;
  if (token_type == TK_LE || token_type == TK_GE || token_type == TK_LT || token_type == TK_GT) return 6;
  if (token_type == '+' || token_type == '-') return 4;
  if (token_type == '*' || token_type == '/') return 3;
  if (token_type == TK_POS || token_type == TK_NEG || token_type == TK_DEREF || token_type == TK_NOT) return 2;
  return -1;
}

int find_main_op(int p, int q) {
  int op = -1;
  int max_priority = -1;
  int parentheses = 0;
  for (int i = p; i <= q; i++) {
    if (tokens[i].type == '(') { parentheses++; continue; }
    if (tokens[i].type == ')') { parentheses--; continue; }
    if (parentheses != 0) continue;

    int priority = get_priority(tokens[i].type);
    if (priority >= max_priority) {
      max_priority = priority;
      op = i;
    }
  }
  return op;
}

bool check_parentheses(int p, int q, bool* badexpr) {
  if (tokens[p].type != '(' || tokens[q].type != ')') return false;
  int balance = 0;
  for (int i = p; i <= q; i++) {
    if (tokens[i].type == '(') balance++;
    else if (tokens[i].type == ')') balance--;
    if (balance == 0 && i < q) return false;
  }
  if (balance != 0) { *badexpr = true; return false; }
  return true;
}

uint32_t eval(int p, int q, bool* badexpr) {
  if (*badexpr) return 0;
  if (p > q) { *badexpr = true; return 0; }
  else if (p == q) {
    if (tokens[p].type == TK_NUMBER) return std::stoul(tokens[p].str);
    if (tokens[p].type == TK_HEX) return std::stoul(tokens[p].str, nullptr, 16);
    if (tokens[p].type == TK_REG) {
        bool success;
        uint32_t val = isa_reg_str2val(tokens[p].str + 1, &success);
        if (!success) *badexpr = true;
        return val;
    }
    *badexpr = true;
    return 0;
  } else if (check_parentheses(p, q, badexpr)) {
    return eval(p + 1, q - 1, badexpr);
  } else {
    int op = find_main_op(p, q);
    if (op == -1) { *badexpr = true; return 0; }

    uint32_t val2 = eval(op + 1, q, badexpr);
    if (tokens[op].type == TK_NEG) return -val2;
    if (tokens[op].type == TK_POS) return val2;
    if (tokens[op].type == TK_NOT) return !val2;
    if (tokens[op].type == TK_DEREF) return paddr_read(val2);

    uint32_t val1 = eval(p, op - 1, badexpr);
    if (*badexpr) return 0;

    switch (tokens[op].type) {
      case '+': return val1 + val2;
      case '-': return val1 - val2;
      case '*': return val1 * val2;
      case '/': if (val2 == 0) { printf("Error: Division by zero\n"); *badexpr = true; return 0; } return val1 / val2;
      case TK_EQ: return val1 == val2;
      case TK_NEQ: return val1 != val2;
      case TK_LE: return val1 <= val2;
      case TK_GE: return val1 >= val2;
      case TK_LT: return val1 < val2;
      case TK_GT: return val1 > val2;
      case TK_AND: return val1 && val2;
      case TK_OR: return val1 || val2;
      default: *badexpr = true; return 0;
    }
  }
}

uint32_t expr(char *e, bool *success) {
  if (!make_token(e)) {
    *success = false;
    return 0;
  }
  pre_token_process();
  bool badexpr = false;
  uint32_t result = eval(0, tokens.size() - 1, &badexpr);
  *success = !badexpr;
  return result;
}
