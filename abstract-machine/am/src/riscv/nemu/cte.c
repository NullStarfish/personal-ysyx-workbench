#include <am.h>
#include <riscv/riscv.h>
#include <klib.h>

static Context* (*user_handler)(Event, Context*) = NULL;

Context* __am_irq_handle(Context *c) {
  if (user_handler) {
    Event ev = {0};
    switch (c->mcause) {
      case 0: ev.event = EVENT_SYSCALL; break;
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
  asm volatile("csrw mtvec, %0" : : "r"(__am_asm_trap));//;把__am_asm_trap装进%0寄存器

  // register event handler
  user_handler = handler;
 
  return true;
}

Context *kcontext(Area kstack, void (*entry)(void *), void *arg) {
  Context *kctx = (Context *)(kstack.end-sizeof(Context));
  kctx->mepc=(uintptr_t) entry;
  kctx->gpr[10] = (uint32_t)arg;//a0寄存器
  kctx->mstatus = 0x1800;//to pass difftest
  return kctx;
}


void yield() {
#ifdef __riscv_e
  asm volatile("li a5, 11; ecall");
#else
  asm volatile("li a7, 11; ecall");//a7用来传递服务号
#endif
}

bool ienabled() {
  return false;
}

void iset(bool enable) {
}
