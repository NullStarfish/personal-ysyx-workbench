#include <am.h>
#include <klib.h>

static volatile bool trapped = false;

static Context *trap_handler(Event ev, Context *ctx) {
  assert(ev.event == EVENT_ERROR);
  assert(ctx->mcause == 2);

  trapped = true;
  ctx->mepc += 4;
  return ctx;
}

int main() {
  assert(cte_init(trap_handler));

  asm volatile(".word 0xffffffff");

  assert(trapped);
  printf("illegal instruction trap passed\n");
  return 0;
}
