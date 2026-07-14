#include "runtime/services/sim_counter.h"

#include <algorithm>
#include <cstdio>

namespace {

std::string text(const char *value) { return value == nullptr ? "" : value; }

std::string key(const std::string &tag, const std::string &name) {
  return tag + '\x1f' + name;
}

} // namespace

void SimCounterBank::rememberTag(const std::string &tag) {
  if (std::find(tags_.begin(), tags_.end(), tag) == tags_.end()) {
    tags_.push_back(tag);
  }
}

int SimCounterBank::allocate(const char *tag, const char *name) {
  const std::string counterTag = text(tag);
  const std::string counterName = text(name);
  const std::string counterKey = key(counterTag, counterName);
  const auto found = ids_.find(counterKey);
  if (found != ids_.end()) return found->second;

  const int id = static_cast<int>(counters_.size());
  counters_.push_back({counterTag, counterName, 0});
  ids_.emplace(counterKey, id);
  rememberTag(counterTag);
  return id;
}

void SimCounterBank::registerRatio(
    const char *tag, const char *name,
    const char *numeratorTag, const char *numeratorName,
    const char *denominatorTag, const char *denominatorName,
    bool percentage) {
  const std::string ratioTag = text(tag);
  const std::string ratioName = text(name);
  const std::string ratioKey = key(ratioTag, ratioName);
  if (ratioIds_.find(ratioKey) != ratioIds_.end()) return;

  const int numerator = allocate(numeratorTag, numeratorName);
  const int denominator = allocate(denominatorTag, denominatorName);
  const int id = static_cast<int>(ratios_.size());
  ratios_.push_back({ratioTag, ratioName, numerator, denominator, percentage});
  ratioIds_.emplace(ratioKey, id);
  rememberTag(ratioTag);
}

void SimCounterBank::add(int id, uint64_t delta) {
  if (id < 0 || static_cast<size_t>(id) >= counters_.size()) return;
  counters_[id].value += delta;
}

uint64_t SimCounterBank::read(int id) const {
  if (id < 0 || static_cast<size_t>(id) >= counters_.size()) return 0;
  return counters_[id].value;
}

uint64_t SimCounterBank::read(const char *tag, const char *name) const {
  const auto found = ids_.find(key(text(tag), text(name)));
  return found == ids_.end() ? 0 : read(found->second);
}

void SimCounterBank::reset() {
  for (Counter &counter : counters_) counter.value = 0;
}

void SimCounterBank::dump() const {
  if (counters_.empty()) return;

  printf("Simulation counters:\n");
  std::vector<std::string> sortedTags = tags_;
  std::sort(sortedTags.begin(), sortedTags.end());
  for (const std::string &tag : sortedTags) {
    printf("  [%s]\n", tag.c_str());

    for (const Counter &counter : counters_) {
      if (counter.tag != tag) continue;
      printf("    %-32s %llu\n", counter.name.c_str(),
             static_cast<unsigned long long>(counter.value));
    }

    for (const Ratio &ratio : ratios_) {
      if (ratio.tag != tag) continue;
      const uint64_t denominator = read(ratio.denominator);
      if (denominator == 0) {
        printf("    %-32s n/a\n", ratio.name.c_str());
        continue;
      }

      const double value = static_cast<double>(read(ratio.numerator)) /
                           static_cast<double>(denominator);
      if (ratio.percentage) {
        printf("    %-32s %.6f%%\n", ratio.name.c_str(), value * 100.0);
      } else {
        printf("    %-32s %.6f\n", ratio.name.c_str(), value);
      }
    }
  }
}
