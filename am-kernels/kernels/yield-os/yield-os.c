#include <am.h>
#include <klib-macros.h>

#define STACK_SIZE (4096 * 8)
typedef union {
  uint8_t stack[STACK_SIZE];
  struct { Context *cp; };
} PCB;
static PCB pcb[2], pcb_boot, *current = &pcb_boot;

static void f(void *arg) {
  while (1) {
    putch("?AB"[(uintptr_t)arg > 2 ? 0 : (uintptr_t)arg]);
    for (int volatile i = 0; i < 100000; i++) ;
    yield();
  }
}

static Context *schedule(Event ev, Context *prev) {
  current->cp = prev;// save current context,进程初启动不需要上下文，kcontext初始化为空。第二次切换时装在这个上下文
  current = (current == &pcb[0] ? &pcb[1] : &pcb[0]);
  current->cp->mstatus = 0x1800;//to pass difftest
  return current->cp;
}

int main() {
  cte_init(schedule);
  pcb[0].cp = kcontext((Area) { pcb[0].stack, &pcb[0] + 1 }, f, (void *)1L);
  pcb[1].cp = kcontext((Area) { pcb[1].stack, &pcb[1] + 1 }, f, (void *)2L);
  yield();
  panic("Should not reach here!");
}
