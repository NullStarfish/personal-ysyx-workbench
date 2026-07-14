#include "runtime/sdb/watchpoint.h"

#include <algorithm>
#include <array>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <regex.h>
#include <string>
#include <vector>

#include "runtime/base/cpu.h"
#include "runtime/platform/memory.h"

class WatchpointManager::Impl {
public:
  Impl(CPU &cpu, Memory &memory) : cpu(cpu), memory(memory) {}
  ~Impl() {
    if (!regexReady) return;
    for (regex_t &regex : regexRules) regfree(&regex);
  }

  enum TokenType {
    TK_NOTYPE = 256, TK_EQ, TK_NEQ, TK_AND, TK_OR, TK_NUMBER, TK_HEX, TK_REG, TK_DEREF, TK_NEG,
  };
  struct Rule { const char *pattern; int tokenType; };
  struct Token { int type; std::string text; };
  struct Watchpoint { int number; std::string expression; uint32_t oldValue; };

  static constexpr size_t kMaxTokens = 64;
  static constexpr size_t kMaxWatchpoints = 32;
  static constexpr size_t kMaxWatchExpression = 63;
  static constexpr std::array<Rule, 15> rules{{
      {" +", TK_NOTYPE}, {"==", TK_EQ}, {"!=", TK_NEQ}, {"&&", TK_AND}, {"\\|\\|", TK_OR},
      {"\\+", '+'}, {"-", '-'}, {"\\*", '*'}, {"/", '/'}, {"\\(", '('}, {"\\)", ')'},
      {"0[xX][0-9a-fA-F]+", TK_HEX}, {"[0-9]+", TK_NUMBER},
      {"\\$((x([0-9]|[1-2][0-9]|3[0-1]))|zero|ra|sp|gp|tp|t[0-6]|s[0-9]|s1[0-1]|a[0-7]|pc)", TK_REG},
      {"\\t+", TK_NOTYPE},
  }};

  void init() {
    compileRegex();
    watchpoints.clear();
    nextWatchpointNo = 0;
#ifdef CONFIG_WATCHPOINT
    printf("Watchpoint is ON\n");
#endif
  }

  void compileRegex() {
    if (regexReady) return;
    for (size_t i = 0; i < rules.size(); ++i) {
      const int result = regcomp(&regexRules[i], rules[i].pattern, REG_EXTENDED);
      if (result == 0) continue;
      char error[128];
      regerror(result, &regexRules[i], error, sizeof(error));
      printf("Regex compilation failed: %s\n%s\n", error, rules[i].pattern);
      exit(1);
    }
    regexReady = true;
  }

  bool tokenize(const char *expression) {
    tokens.clear();
    size_t position = 0;
    while (expression[position] != '\0') {
      if (tokens.size() >= kMaxTokens) {
        printf("Expression too long\n");
        return false;
      }
      regmatch_t match;
      size_t ruleIndex = 0;
      for (; ruleIndex < rules.size(); ++ruleIndex) {
        if (regexec(&regexRules[ruleIndex], expression + position, 1, &match, 0) != 0 || match.rm_so != 0) continue;
        const size_t length = static_cast<size_t>(match.rm_eo);
        if (rules[ruleIndex].tokenType != TK_NOTYPE) {
          tokens.push_back({rules[ruleIndex].tokenType, std::string(expression + position, length)});
        }
        position += length;
        break;
      }
      if (ruleIndex == rules.size()) {
        printf("No match at position %zu\n%s\n%*s^\n", position, expression, static_cast<int>(position), "");
        return false;
      }
    }
    for (size_t i = 0; i < tokens.size(); ++i) {
      if (tokens[i].type != '*' && tokens[i].type != '-') continue;
      const bool unary = i == 0 || (tokens[i - 1].type != TK_NUMBER && tokens[i - 1].type != TK_HEX &&
                                    tokens[i - 1].type != TK_REG && tokens[i - 1].type != ')');
      if (unary) tokens[i].type = tokens[i].type == '*' ? TK_DEREF : TK_NEG;
    }
    return true;
  }

  static int priority(int type) {
    if (type == TK_OR) return 5;
    if (type == TK_AND) return 4;
    if (type == TK_EQ || type == TK_NEQ) return 3;
    if (type == '+' || type == '-') return 2;
    if (type == '*' || type == '/') return 1;
    return -1;
  }

  int findMainOperator(int first, int last) const {
    int result = -1;
    int bestPriority = -1;
    int parentheses = 0;
    for (int i = first; i <= last; ++i) {
      if (tokens[i].type == '(') { ++parentheses; continue; }
      if (tokens[i].type == ')') { --parentheses; continue; }
      if (parentheses != 0) continue;
      const int current = priority(tokens[i].type);
      if (current >= bestPriority && current >= 0) {
        bestPriority = current;
        result = i;
      }
    }
    if (result >= 0) return result;
    parentheses = 0;
    for (int i = first; i <= last; ++i) {
      if (tokens[i].type == '(') ++parentheses;
      else if (tokens[i].type == ')') --parentheses;
      else if (parentheses == 0 && (tokens[i].type == TK_NEG || tokens[i].type == TK_DEREF)) return i;
    }
    return -1;
  }

  bool enclosesExpression(int first, int last, bool &badExpression) const {
    if (tokens[first].type != '(' || tokens[last].type != ')') return false;
    int balance = 0;
    for (int i = first; i <= last; ++i) {
      if (tokens[i].type == '(') ++balance;
      else if (tokens[i].type == ')') --balance;
      if (balance < 0) { badExpression = true; return false; }
      if (balance == 0 && i < last) return false;
    }
    if (balance != 0) badExpression = true;
    return balance == 0;
  }

  uint32_t evaluateRange(int first, int last, bool &badExpression) const {
    if (badExpression || first > last) { badExpression = true; return 0; }
    if (first == last) {
      if (tokens[first].type == TK_NUMBER) return strtoul(tokens[first].text.c_str(), nullptr, 10);
      if (tokens[first].type == TK_HEX) return strtoul(tokens[first].text.c_str(), nullptr, 16);
      if (tokens[first].type == TK_REG) {
        bool success = false;
        const uint32_t value = cpu.isa_reg_str2val(tokens[first].text.c_str() + 1, &success);
        badExpression = !success;
        return value;
      }
      badExpression = true;
      return 0;
    }
    if (enclosesExpression(first, last, badExpression)) return evaluateRange(first + 1, last - 1, badExpression);
    const int op = findMainOperator(first, last);
    if (op < 0) { badExpression = true; return 0; }
    if (tokens[op].type == TK_NEG || tokens[op].type == TK_DEREF) {
      const uint32_t value = evaluateRange(op + 1, last, badExpression);
      return tokens[op].type == TK_NEG ? -value : static_cast<uint32_t>(memory.pmemRead(value));
    }
    const uint32_t lhs = evaluateRange(first, op - 1, badExpression);
    const uint32_t rhs = evaluateRange(op + 1, last, badExpression);
    if (badExpression) return 0;
    switch (tokens[op].type) {
      case '+': return lhs + rhs;
      case '-': return lhs - rhs;
      case '*': return lhs * rhs;
      case '/':
        if (rhs != 0) return lhs / rhs;
        printf("Error: Division by zero\n");
        badExpression = true;
        return 0;
      case TK_EQ: return lhs == rhs;
      case TK_NEQ: return lhs != rhs;
      case TK_AND: return lhs && rhs;
      case TK_OR: return lhs || rhs;
      default: badExpression = true; return 0;
    }
  }

  uint32_t evaluate(const char *expression, bool *success) {
    if (expression == nullptr || !tokenize(expression) || tokens.empty()) {
      *success = false;
      return 0;
    }
    bool badExpression = false;
    const uint32_t result = evaluateRange(0, static_cast<int>(tokens.size()) - 1, badExpression);
    *success = !badExpression;
    return result;
  }

  CPU &cpu;
  Memory &memory;
  bool regexReady = false;
  std::array<regex_t, rules.size()> regexRules{};
  std::vector<Token> tokens;
  std::vector<Watchpoint> watchpoints;
  int nextWatchpointNo = 0;
};

WatchpointManager::WatchpointManager(CPU &cpu, Memory &memory)
    : impl(std::make_unique<Impl>(cpu, memory)) {}
WatchpointManager::~WatchpointManager() = default;
void WatchpointManager::init() { impl->init(); }
uint32_t WatchpointManager::evaluate(const char *expression, bool *success) {
  return impl->evaluate(expression, success);
}

bool WatchpointManager::add(const char *expression) {
  if (expression == nullptr) return false;
  if (impl->watchpoints.size() >= Impl::kMaxWatchpoints) {
    printf("No free watchpoint available.\n");
    return false;
  }
  if (strlen(expression) > Impl::kMaxWatchExpression) {
    printf("Error: Watchpoint expression is too long (max %zu characters).\n", Impl::kMaxWatchExpression);
    return false;
  }
  bool success = false;
  const uint32_t value = impl->evaluate(expression, &success);
  if (!success) {
    printf("Invalid expression for watchpoint: %s\n", expression);
    return false;
  }
  impl->watchpoints.push_back({impl->nextWatchpointNo++, expression, value});
  printf("Watchpoint %d: %s\n", impl->watchpoints.back().number, expression);
  return true;
}

bool WatchpointManager::remove(int number) {
  auto found = std::find_if(impl->watchpoints.begin(), impl->watchpoints.end(),
                            [number](const Impl::Watchpoint &watchpoint) { return watchpoint.number == number; });
  if (found == impl->watchpoints.end()) {
    printf("Watchpoint %d not found.\n", number);
    return false;
  }
  printf("Deleted watchpoint %d: %s\n", found->number, found->expression.c_str());
  impl->watchpoints.erase(found);
  return true;
}

void WatchpointManager::display() const {
  if (impl->watchpoints.empty()) {
    printf("No watchpoints.\n");
    return;
  }
  printf("Num\tWhat\t\tValue\n---\t----\t\t-----\n");
  for (const Impl::Watchpoint &watchpoint : impl->watchpoints) {
    printf("%-d\t%-16s\t0x%08x (%u)\n", watchpoint.number, watchpoint.expression.c_str(),
           watchpoint.oldValue, watchpoint.oldValue);
  }
}

bool WatchpointManager::check() {
  bool triggered = false;
  for (Impl::Watchpoint &watchpoint : impl->watchpoints) {
    bool success = false;
    const uint32_t value = impl->evaluate(watchpoint.expression.c_str(), &success);
    if (!success || value == watchpoint.oldValue) continue;
    printf("\nWatchpoint %d: %s\n", watchpoint.number, watchpoint.expression.c_str());
    printf("Old value = 0x%08x (%u)\n", watchpoint.oldValue, watchpoint.oldValue);
    printf("New value = 0x%08x (%u)\n", value, value);
    watchpoint.oldValue = value;
    triggered = true;
  }
  return triggered;
}
