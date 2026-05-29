#include <stdint.h>

#include "bootloader.h"

extern char _sdram_lma;
extern char _sdram_vma;
extern char _sdram_vma_end;
extern char _bss_start;
extern char _bss_end;
extern void _sdram_entry(void);

BOOT_STAGE1_TEXT __attribute__((weak))
int __am_bootloader_plugin(void) {
  return 0;
}

BOOT_STAGE1_TEXT __attribute__((noreturn))
void __am_bootloader_main(void) {
  if (__am_bootloader_plugin()) {
    boot_halt();
    while (1) {
    }
  }

  boot_uart_init();
  boot_puts("YSYXSOC Booting...\n");

  uint32_t *src = (uint32_t *)&_sdram_lma;
  uint32_t *dst = (uint32_t *)&_sdram_vma;
  uint32_t *end = (uint32_t *)&_sdram_vma_end;

  while (dst < end) {
    *dst++ = *src++;
  }

  dst = (uint32_t *)&_bss_start;
  end = (uint32_t *)&_bss_end;
  while (dst < end) {
    *dst++ = 0;
  }

  boot_puts("BOOT Completed.\n");

  _sdram_entry();

  while (1) {
  }
}
