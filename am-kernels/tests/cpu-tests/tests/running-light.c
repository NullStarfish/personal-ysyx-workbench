#define GPIO_BASE 0x10002000
#define GPIO_OUT  (*(volatile unsigned int *)(GPIO_BASE + 0x0))

#include <klib.h>

static void delay(void) {
  for (volatile int i = 0; i < 20000; i++) {
  }
}

int main() {
  unsigned int led = 1;
  int dir = 1;

  printf("running light start\n");

  while (1) {
    GPIO_OUT = led;
    delay();

    if (dir) {
      led <<= 1;
      if (led == 0x8000) {
        dir = 0;
      }
    } else {
      led >>= 1;
      if (led == 0x0001) {
        dir = 1;
      }
    }
  }
}
