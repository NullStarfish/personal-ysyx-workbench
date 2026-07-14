#include "runtime/sdb/sdb.h"

#include <algorithm>
#include <array>
#include <cstdint>
#include <cstdio>
#include <cstdlib>
#include <cstring>
#include <regex.h>
#include <string>
#include <vector>

#ifdef CONFIG_INTERACTIVE_SDB
#include <readline/history.h>
#include <readline/readline.h>
#endif

#include "cpu.h"
#include "mem.h"
#include "runtime/runtime.h"
#include "sim.h"

#ifdef CONFIG_BOARD
#include <nvboard.h>
#endif

namespace {
#ifdef CONFIG_BOARD
int nvboardReadlineEventHook() {
  nvboard_update();
  return 0;
}
#endif
}

class Sdb::Impl {
public:
  explicit Impl(Runtime &runtime) : owner(runtime) {}

  ~Impl() {
    if (!regexReady) return;
    for (regex_t &regex : regexRules) regfree(&regex);
  }

  void init() {
    compileRegex();
    watchpoints.clear();
    nextWatchpointNo = 0;
#ifdef CONFIG_WATCHPOINT
    printf("Watchpoint is ON\n");
#endif
    printf("SDB initialized. Ready for debugging.\n");
  }

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
      if (command == nullptr) {
        free(line);
        continue;
      }
#ifdef CONFIG_BOARD
      nvboard_update();
#endif
      char *args = strtok(nullptr, "");
      const CommandEntry *entry = findCommand(command);
      if (entry == nullptr) {
        printf("Unknown command '%s'\n", command);
      } else if ((this->*entry->handler)(args) < 0) {
        free(line);
        return;
      }
      free(line);
    }
#else
    cpu.exec(static_cast<uint64_t>(-1));
#endif
  }

  bool checkWatchpoints() {
    bool triggered = false;
    for (Watchpoint &watchpoint : watchpoints) {
      bool success = false;
      const uint32_t newValue = evaluate(watchpoint.expression.c_str(), &success);
      if (!success || newValue == watchpoint.oldValue) continue;

      printf("\nWatchpoint %d: %s\n", watchpoint.number, watchpoint.expression.c_str());
      printf("Old value = 0x%08x (%u)\n", watchpoint.oldValue, watchpoint.oldValue);
      printf("New value = 0x%08x (%u)\n", newValue, newValue);
      watchpoint.oldValue = newValue;
      triggered = true;
    }
    return triggered;
  }

  Runtime &owner;
  bool batchMode = false;

private:
  enum TokenType {
    TK_NOTYPE = 256,
    TK_EQ,
    TK_NEQ,
    TK_AND,
    TK_OR,
    TK_NUMBER,
    TK_HEX,
    TK_REG,
    TK_DEREF,
    TK_NEG,
  };

  struct Rule {
    const char *pattern;
    int tokenType;
  };

  struct Token {
    int type;
    std::string text;
  };

  struct Watchpoint {
    int number;
    std::string expression;
    uint32_t oldValue;
  };

  using CommandHandler = int (Impl::*)(char *);
  struct CommandEntry {
    const char *name;
    const char *description;
    CommandHandler handler;
  };

  static constexpr size_t kMaxTokens = 64;
  static constexpr size_t kMaxWatchpoints = 32;
  static constexpr size_t kMaxWatchExpression = 63;
  static constexpr std::array<Rule, 15> rules{{
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
      {"\\t+", TK_NOTYPE},
  }};

  static const std::array<CommandEntry, 11> &commands() {
    static const std::array<CommandEntry, 11> table{{
        {"help", "Display information about all supported commands", &Impl::cmdHelp},
        {"c", "Continue the execution of the program", &Impl::cmdContinue},
        {"q", "Exit the simulator", &Impl::cmdQuit},
        {"si", "Step forward [N] instructions (default 1)", &Impl::cmdStep},
        {"info", "Print program state (r for registers, w for watchpoints)", &Impl::cmdInfo},
        {"x", "Scan memory: x N EXPR", &Impl::cmdExamine},
        {"p", "Evaluate expression: p EXPR", &Impl::cmdPrint},
        {"w", "Set a watchpoint: w EXPR", &Impl::cmdWatch},
        {"d", "Delete a watchpoint: d N", &Impl::cmdDelete},
        {"bt", "Print the current function call stack from ftrace", &Impl::cmdBacktrace},
        {"vcd", "Control interval VCD tracing: vcd watch start [FILE] | end | status", &Impl::cmdVcd},
    }};
    return table;
  }

  const CommandEntry *findCommand(const char *name) const {
    const auto &table = commands();
    auto it = std::find_if(table.begin(), table.end(),
                           [name](const CommandEntry &entry) { return strcmp(name, entry.name) == 0; });
    return it == table.end() ? nullptr : &*it;
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
      if (tokens[i].type == '(') {
        ++parentheses;
        continue;
      }
      if (tokens[i].type == ')') {
        --parentheses;
        continue;
      }
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
      if (balance < 0) {
        badExpression = true;
        return false;
      }
      if (balance == 0 && i < last) return false;
    }
    if (balance != 0) badExpression = true;
    return balance == 0;
  }

  uint32_t evaluateRange(int first, int last, bool &badExpression) const {
    if (badExpression || first > last) {
      badExpression = true;
      return 0;
    }
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
    if (op < 0) {
      badExpression = true;
      return 0;
    }
    if (tokens[op].type == TK_NEG || tokens[op].type == TK_DEREF) {
      const uint32_t value = evaluateRange(op + 1, last, badExpression);
      return tokens[op].type == TK_NEG ? -value : static_cast<uint32_t>(mem.pmemRead(value));
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
      default:
        badExpression = true;
        return 0;
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
    cpu.exec(static_cast<uint64_t>(-1));
    return 0;
  }

  int cmdQuit(char *) {
    owner.setQuit();
    const char *imagePath = owner.imagePath();
    printf("file %s quit\n", imagePath == nullptr ? "(none)" : imagePath);
    return -1;
  }

  int cmdStep(char *args) {
    char *end = nullptr;
    long count = args == nullptr ? 1 : strtol(args, &end, 10);
    if (args != nullptr && end != nullptr && *end != '\0') count = 1;
    cpu.exec(count);
    return 0;
  }

  int cmdInfo(char *args) {
    if (args == nullptr) {
      printf("Usage: info r|w\n");
    } else if (strcmp(args, "r") == 0) {
      cpu.isa_reg_display();
    } else if (strcmp(args, "w") == 0) {
      displayWatchpoints();
    } else {
      printf("Unknown argument for 'info': %s\n", args);
    }
    return 0;
  }

  int cmdExamine(char *args) {
    if (args == nullptr) {
      printf("Usage: x N EXPR\n");
      return 0;
    }
    char *countText = strtok(args, " ");
    char *expression = countText == nullptr ? nullptr : strtok(nullptr, "");
    if (expression == nullptr) {
      printf("Usage: x N EXPR\n");
      return 0;
    }

    bool success = false;
    const int count = strtol(countText, nullptr, 10);
    const uint32_t address = evaluate(expression, &success);
    if (!success) {
      printf("Invalid expression: %s\n", expression);
      return 0;
    }
    printf("Scanning %d words from address 0x%x:\n", count, address);
    for (int i = 0; i < count; ++i) {
      printf("0x%08x: 0x%08x\n", address + i * 4, mem.pmemRead(address + i * 4));
    }
    return 0;
  }

  int cmdPrint(char *args) {
    if (args == nullptr) {
      printf("Usage: p EXPR\n");
      return 0;
    }
    bool success = false;
    const uint32_t result = evaluate(args, &success);
    if (success) printf("%s = %u (0x%x)\n", args, result, result);
    else printf("Invalid expression\n");
    return 0;
  }

  int cmdWatch(char *args) {
    if (args == nullptr) {
      printf("Usage: w EXPR\n");
      return 0;
    }
    if (watchpoints.size() >= kMaxWatchpoints) {
      printf("No free watchpoint available.\n");
      return 0;
    }
    if (strlen(args) > kMaxWatchExpression) {
      printf("Error: Watchpoint expression is too long (max %zu characters).\n", kMaxWatchExpression);
      return 0;
    }

    bool success = false;
    const uint32_t initialValue = evaluate(args, &success);
    if (!success) {
      printf("Invalid expression for watchpoint: %s\n", args);
      return 0;
    }
    watchpoints.push_back({nextWatchpointNo++, args, initialValue});
    printf("Watchpoint %d: %s\n", watchpoints.back().number, watchpoints.back().expression.c_str());
    return 0;
  }

  int cmdDelete(char *args) {
    if (args == nullptr) {
      printf("Usage: d N\n");
      return 0;
    }
    const int number = strtol(args, nullptr, 10);
    auto it = std::find_if(watchpoints.begin(), watchpoints.end(),
                           [number](const Watchpoint &watchpoint) { return watchpoint.number == number; });
    if (it == watchpoints.end()) {
      printf("Watchpoint %d not found.\n", number);
    } else {
      printf("Deleted watchpoint %d: %s\n", it->number, it->expression.c_str());
      watchpoints.erase(it);
    }
    return 0;
  }

  int cmdBacktrace(char *) {
#ifdef CONFIG_FTRACE
    if (!owner.ftrace().enabled()) {
      printf("Backtrace unavailable: start NPC with --ftrace=FILE using an ELF with symbols.\n");
    } else {
      owner.ftrace().printStack();
    }
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
      if (owner.isVcdWatching()) printf("VCD watch is already active (%s)\n", owner.vcdPath());
      else if (owner.startVcdWatch(filename)) printf("VCD watch started: %s\n", owner.vcdPath());
    } else if (strcmp(action, "end") == 0) {
      if (owner.endVcdWatch()) printf("VCD watch stopped: %s\n", owner.vcdPath());
      else printf("VCD watch is not active\n");
    } else if (strcmp(action, "status") == 0) {
      const char *path = owner.vcdPath();
      printf("VCD watch: %s", owner.isVcdWatching() ? "active" : "inactive");
      if (path != nullptr) printf(" (%s)", path);
      printf("\n");
    } else {
      printf("Usage: vcd watch start [FILE] | end | status\n");
    }
    return 0;
  }

  void displayWatchpoints() const {
    if (watchpoints.empty()) {
      printf("No watchpoints.\n");
      return;
    }
    printf("Num\tWhat\t\tValue\n---\t----\t\t-----\n");
    for (const Watchpoint &watchpoint : watchpoints) {
      printf("%-d\t%-16s\t0x%08x (%u)\n", watchpoint.number, watchpoint.expression.c_str(),
             watchpoint.oldValue, watchpoint.oldValue);
    }
  }

  bool regexReady = false;
  std::array<regex_t, rules.size()> regexRules{};
  std::vector<Token> tokens;
  std::vector<Watchpoint> watchpoints;
  int nextWatchpointNo = 0;
};

Sdb::Sdb(Runtime &runtime) : impl(std::make_unique<Impl>(runtime)) {}
Sdb::~Sdb() = default;

void Sdb::init() { impl->init(); }
void Sdb::mainLoop() { impl->mainLoop(); }
void Sdb::setBatchMode() { impl->batchMode = true; }
bool Sdb::checkWatchpoints() { return impl->checkWatchpoints(); }
