#ifndef __STATE_H__
#define __STATE_H__

#include <stdbool.h>
#include <stdint.h>

typedef struct {
  int state;
  uint64_t halt_ret;
} NpcState;

// 定义仿真器可能的状态
enum { NPC_RUNNING, NPC_STOP, NPC_END, NPC_ABORT, NPC_QUIT };

// 全局状态变量
extern NpcState npc_state;

// 检查退出状态
int is_exit_status_bad();

#endif
