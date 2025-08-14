#include <stdio.h>
#include "state.h"
#include "sdb/sdb.h"

// --- 由 C++ 桥接文件 (main.cpp) 提供的底层硬件接口 ---
void single_cycle_cpp();
long long get_cycle_count();

void cpu_exec(uint64_t n) {
  if (npc_state.state == NPC_END || npc_state.state == NPC_ABORT) {
    printf("Program execution has ended. To restart, exit and run again.\n");
    return;
  }
  npc_state.state = NPC_RUNNING;

  uint64_t i = 0;
  while ((n == (uint64_t)-1 || i < n) && npc_state.state == NPC_RUNNING) {
    single_cycle_cpp();
    i++;
    if (check_watchpoints()) {
      npc_state.state = NPC_STOP;
    }
  }

  switch (npc_state.state) {
    case NPC_RUNNING: npc_state.state = NPC_STOP; break;
    case NPC_END: case NPC_ABORT:
      printf("Program execution has ended at cycle %lld.\n", get_cycle_count());
      break;
    case NPC_QUIT:
      break;
    // case NPC_STOP is handled by the main loop
  }
}
