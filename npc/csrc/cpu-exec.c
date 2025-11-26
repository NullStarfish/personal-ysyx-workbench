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
  // [修改前]
  // uint32_t pc = get_pc_cpp();
  // uint32_t inst = get_inst_cpp();
  // exec_one_cycle_cpp();
  
  // [修改后]
  // 1. 先让硬件跑，直到有一条指令在 WB 阶段提交
  //    此时 DPI 函数会被调用，更新 C++ 侧的 g_cpu_state
  exec_one_cycle_cpp();

  // 2. 此时读取的才是刚刚执行完的那条指令的 PC 和机器码
  uint32_t pc = get_pc_cpp();
  uint32_t inst = get_inst_cpp();
  
  // 3. 拿着最新的状态去写 Log 和做 Difftest
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
