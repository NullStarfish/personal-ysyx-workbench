#include <stdio.h>
#include "cpu-exec.h"
#include "state.h"
#include "sdb/sdb.h"
#include "trace/itrace.h"
#include "trace/ftrace.h"
#include "difftest/dut.h"

// --- From C++ bridge ---
uint32_t get_pc_cpp();
uint32_t get_inst_cpp();
void exec_one_cycle_cpp();

static void trace_and_difftest(uint32_t pc, uint32_t inst) {
  log_and_trace(pc, inst);
  trace_func_call(pc, inst);
  
  // FIX: Only perform difftest step if it is enabled.
  if (difftest_is_enabled) {
    difftest_step();
  }

  if (check_watchpoints()) {
    npc_state.state = NPC_STOP;
  }
}

static void exec_once() {
  uint32_t pc = get_pc_cpp();
  uint32_t inst = get_inst_cpp();
  exec_one_cycle_cpp();
  trace_and_difftest(pc, inst);
}

static void execute(uint64_t n) {
  for (; n > 0; n--) {
    exec_once();
    if (npc_state.state != NPC_RUNNING) break;
  }
}

void cpu_exec(uint64_t n) {
  if (npc_state.state == NPC_END || npc_state.state == NPC_ABORT) {
    printf("Program execution has ended. To restart, exit and run again.\n");
    return;
  }
  npc_state.state = NPC_RUNNING;
  execute(n);
  if (npc_state.state == NPC_RUNNING) {
    npc_state.state = NPC_STOP;
  }
}
