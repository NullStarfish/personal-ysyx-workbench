
#include <am.h>
#include "../riscv.h"

#define GPIO_BASE 0x10002000
#define SEG_ADDR  (GPIO_BASE + 0x8)

void print_seg(uint32_t a) {
  assert(a < 100000000);

  uint32_t v = 0;
  for (int i = 0; i < 4; i++) {
    uint32_t lo = a % 10; a /= 10;
    uint32_t hi = a % 10; a /= 10;
    v |= ((hi << 4) | lo) << (i * 8);
  }

  outl(SEG_ADDR, v);
}
