#include <am.h>
#include <riscv/riscv.h>
#include <klib.h>

static Context* (*user_handler)(Event, Context*) = NULL;

Context* __am_irq_handle(Context *c) {
  if (user_handler) {
    Event ev = {0};
    switch (c->mcause) {
      case 11: ev.event = EVENT_YIELD; c->mepc += 4;break;
      default: ev.event = EVENT_ERROR; break;
    }

    c = user_handler(ev, c);
    assert(c != NULL);
  }

  return c;
}

extern void __am_asm_trap(void);

bool cte_init(Context*(*handler)(Event, Context*)) {
  // initialize exception entry
  asm volatile("csrw mtvec, %0" : : "r"(__am_asm_trap));

  // register event handler
  user_handler = handler;

  return true;
}
/**
 * kstack: stack,这里的Stack完全根据__am_asm_trap的内存排布顺序进行定义,
 * 在使用的时候，我们必须保证kstack分配的大小能够装入一个Context
 * entry: 进程的函数入口
 * arg: 传给entry的参数
 * 用来构建Context对象的
 */
Context *kcontext(Area kstack, void (*entry)(void *), void *arg) {
  Context *kctx = (Context *)(kstack.end-sizeof(Context));
  kctx->mepc=(uintptr_t) entry;
  kctx->gpr[10] = (uint32_t)arg;
  kctx->mstatus = 0x1800;//to pass difftest
  return kctx;
}

void yield() {
#ifdef __riscv_e
  asm volatile("li a5, 11; ecall");
#else
  asm volatile("li a7, 11; ecall");
#endif
}

bool ienabled() {
  return false;
}

void iset(bool enable) {
}
