#ifndef AM_RISCV_YSYXSOC_BOOTLOADER_H
#define AM_RISCV_YSYXSOC_BOOTLOADER_H

#define BOOT_STAGE1_TEXT __attribute__((section(".text.stage1")))
#define BOOT_STAGE1_RODATA __attribute__((section(".rodata.stage1")))
#define BOOT_STAGE1_DATA __attribute__((section(".data.stage1")))
#define BOOT_STAGE1_BSS __attribute__((section(".bss.stage1")))

BOOT_STAGE1_TEXT int __am_bootloader_plugin(void);


#include <stdint.h>
#include "ysyxsoc.h"
static BOOT_STAGE1_TEXT void boot_uart_init(void) {
  volatile uint8_t *uart = (volatile uint8_t *)SERIAL_PORT;
  uart[1] = 0x00;
  uart[3] = 0x83;
  uart[1] = 0x00;
  uart[0] = 0x01;
  uart[3] = 0x03;
  uart[2] = 0xc7;
}

static BOOT_STAGE1_TEXT void boot_putch(char ch) {
  volatile uint8_t *uart = (volatile uint8_t *)SERIAL_PORT;
  while ((uart[5] & UART_LSR_THRE) == 0) {
  }
  uart[0] = (uint8_t)ch;
}

static BOOT_STAGE1_TEXT void boot_puts(const char *s) {
  while (*s != '\0') {
    boot_putch(*s++);
  }
}



static BOOT_STAGE1_TEXT void boot_halt(void) {
  asm volatile("ebreak");
  while (1) {
  }
}

#endif
