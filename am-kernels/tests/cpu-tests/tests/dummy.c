#include <stdio.h>
int main() {
  asm volatile("ebreak");
  printf("now");
  return 0;
}
