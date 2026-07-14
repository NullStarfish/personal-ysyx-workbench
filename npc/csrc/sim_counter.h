#ifndef NPC_SIM_COUNTER_H
#define NPC_SIM_COUNTER_H

#include <cstdint>
#include <string>
#include <unordered_map>
#include <vector>

class SimCounterBank {
public:
  int allocate(const char *tag, const char *name);
  void registerRatio(const char *tag, const char *name,
                     const char *numeratorTag, const char *numeratorName,
                     const char *denominatorTag, const char *denominatorName,
                     bool percentage);
  void add(int id, uint64_t delta);
  uint64_t read(int id) const;
  uint64_t read(const char *tag, const char *name) const;
  void reset();
  void dump() const;

private:
  struct Counter {
    std::string tag;
    std::string name;
    uint64_t value = 0;
  };

  struct Ratio {
    std::string tag;
    std::string name;
    int numerator = -1;
    int denominator = -1;
    bool percentage = false;
  };

  void rememberTag(const std::string &tag);

  std::vector<Counter> counters_;
  std::vector<Ratio> ratios_;
  std::vector<std::string> tags_;
  std::unordered_map<std::string, int> ids_;
  std::unordered_map<std::string, int> ratioIds_;
};

extern SimCounterBank simCounters;

#endif
