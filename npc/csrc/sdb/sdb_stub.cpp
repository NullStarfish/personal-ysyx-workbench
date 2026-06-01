#include "sdb/sdb.h"

#include <cstdint>

#include "sim.h"

namespace {
bool batchMode = false;
}

void sdb_set_batch_mode() {
  batchMode = true;
}

void sdb_mainloop() {
  (void)batchMode;
  cpu.exec(static_cast<uint64_t>(-1));
}

void init_sdb() {}

void init_regex() {}

uint32_t expr(char *, bool *success) {
  if (success != nullptr) {
    *success = false;
  }
  return 0;
}

void init_wp_pool() {}
void wp_add(char *) {}
void wp_remove(int) {}
void display_wp() {}

bool check_watchpoints() {
  return false;
}
