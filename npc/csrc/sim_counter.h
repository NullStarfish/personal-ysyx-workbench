#ifndef NPC_SIM_COUNTER_H
#define NPC_SIM_COUNTER_H

#include <cstdint>
#include <string>
#include <unordered_map>
#include <vector>

class SimCounterBank {
public:
  int allocate(const char *name);
  void add(int id, uint64_t delta);
  uint64_t read(int id) const;
  uint64_t read(const char *name) const;
  void reset();
  void dump() const;

private:
  struct Counter {
    std::string name;
    uint64_t value = 0;
  };

  std::vector<Counter> counters_;
  std::unordered_map<std::string, int> ids_;
};

extern SimCounterBank simCounters;

#endif
