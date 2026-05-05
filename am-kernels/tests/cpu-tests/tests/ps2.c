#include <am.h>
#include <klib.h>
int main() {
  while (1) {
    unsigned char c = *(volatile unsigned char *)0x10011000;
    if (c) {
      printf("%d\n", (int)c);
      // 或者 printf("%02x\n", (unsigned)c);
    }
  }
}