#include "am.h"
#include <klib.h>
#include <klib-macros.h>
#define UART_BASE 0x10000000L
#define UART_TX   0
int main() {
  //*(volatile char *)(UART_BASE + UART_TX) = 'A';
  //*(volatile char *)(UART_BASE + UART_TX) = '\n';
  putch('A');
  for (int i = 0; i < 10; i ++) {
    putch('a' + i);
  }
  putch('\n');
  putstr("abcdefg");
    return 0;
}