#include "sim_counter.h"

#include <cstdio>

SimCounterBank simCounters;

int SimCounterBank::allocate(const char *name) {
  const std::string key = name == nullptr ? "" : name;
  const auto found = ids_.find(key);
  if (found != ids_.end()) return found->second;

  const int id = static_cast<int>(counters_.size());
  counters_.push_back({key, 0});
  ids_.emplace(key, id);
  return id;
}

void SimCounterBank::add(int id, uint64_t delta) {
  if (id < 0 || static_cast<size_t>(id) >= counters_.size()) return;
  counters_[id].value += delta;
}

uint64_t SimCounterBank::read(int id) const {
  if (id < 0 || static_cast<size_t>(id) >= counters_.size()) return 0;
  return counters_[id].value;
}

uint64_t SimCounterBank::read(const char *name) const {
  const auto found = ids_.find(name == nullptr ? "" : name);
  return found == ids_.end() ? 0 : read(found->second);
}

void SimCounterBank::reset() {
  for (Counter &counter : counters_) counter.value = 0;
}

void SimCounterBank::dump() const {
  if (counters_.empty()) return;

  printf("Simulation counters:\n");
  for (const Counter &counter : counters_) {
    printf("  %-36s %llu\n", counter.name.c_str(),
           static_cast<unsigned long long>(counter.value));
  }
}

extern "C" int sim_counter_alloc(const char *name) {
  return simCounters.allocate(name);
}

extern "C" void sim_counter_add(int id, uint64_t delta) {
  simCounters.add(id, delta);
}

extern "C" uint64_t sim_counter_read(int id) {
  return simCounters.read(id);
}
