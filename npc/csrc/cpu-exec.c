#include <stdio.h>
#include "cpu-exec.h"
#include "state.h"
#include "sdb/sdb.h"
#include "trace/itrace.h"
#include "trace/ftrace.h"
#include "difftest/dut.h"

// --- From C++ bridge ---
uint32_t get_pc_cpp();
uint32_t get_retire_pc_cpp();
uint32_t get_inst_cpp();
void exec_one_cycle_cpp();
void nvboard_flush_cpp();

// This function is now defined in NEMU's difftest framework
void (*ref_difftest_raise_intr)(uint64_t NO) = NULL;

static void trace_and_difftest(uint32_t pc, uint32_t inst) {
#ifdef CONFIG_ITRACE
  // The log_and_trace function now handles disassembly correctly
  // in both modes, so we can call it unconditionally.
  log_and_trace(pc, inst);
#endif

#ifdef CONFIG_FTRACE
  trace_func_call(pc, inst);
#endif
  
#ifdef CONFIG_DIFFTEST
  if (difftest_is_enabled) {
    difftest_step();
  }
#endif
#ifdef CONFIG_WATCHPOINT
  volatile bool need_stop = check_watchpoints();
  if (need_stop) {
    npc_state.state = NPC_STOP;
  }
#endif
}

static void exec_once() {
  // 1. 让硬件跑到一次退休事件。
  //    当前 tracer/DPI 的语义是：在退休当拍把 retiring instruction
  //    的 pc/inst/dnpc 和对应架构态快照送到 C++ 侧。
  exec_one_cycle_cpp();
  if (npc_state.state != NPC_RUNNING) return;

  // 2. itrace/ftrace 仍然观察“刚刚退休的那条指令”。
  uint32_t pc = get_retire_pc_cpp();
  uint32_t inst = get_inst_cpp();
  
  // 3. 再基于同一次退休事件的快照做日志和 difftest。
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
