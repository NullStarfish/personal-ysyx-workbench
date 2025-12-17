#include "am.h"
#include <klib.h>
#define UART_BASE 0x10000000L
#define UART_TX   0
int main() {
  //*(volatile char *)(UART_BASE + UART_TX) = 'A';
  //*(volatile char *)(UART_BASE + UART_TX) = '\n';
  putch('A');
    return 0;
}