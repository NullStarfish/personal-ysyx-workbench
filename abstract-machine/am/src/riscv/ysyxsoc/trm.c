#include <am.h>
#include <klib.h>
#include <klib-macros.h>
#include "../riscv.h"
#include "ysyxsoc.h"
#include "seg.h"

extern char _heap_start;
// 引入链接脚本中定义的符号
extern char _data_lma;     // MROM 中 data 的起始地址
extern char _data_vma;     // SRAM 中 data 的起始地址
extern char _data_vma_end; // SRAM 中 data 的结束地址
extern char _bss_start;
extern char _bss_end;


void ebreak();
int main(const char *args);

extern char _pmem_start;
#define PMEM_SIZE (32 * 1024 * 1024)
#define PMEM_END  ((uintptr_t)&_pmem_start + PMEM_SIZE)

Area heap = RANGE(&_heap_start, PMEM_END);
static const char mainargs[MAINARGS_MAX_LEN] = MAINARGS_PLACEHOLDER; // defined in CFLAGS



#define UART_LSR_THRE 0x20  // Transmitter Holding Register Empty

static bool init_uart = false;


void __am_uart_init_once(void) {
  if (init_uart) return;

  outb(SERIAL_PORT + 1, 0x00);
  outb(SERIAL_PORT + 3, 0x83);

  uint16_t divisor = 1;
  outb(SERIAL_PORT + 1, (divisor >> 8) & 0xff);
  outb(SERIAL_PORT + 0, divisor & 0xff);  // 你现在写成 &0x0f 了，最好改成 &0xff

  outb(SERIAL_PORT + 3, 0x03);
  outb(SERIAL_PORT + 2, 0xc7);

  init_uart = true;
}

void putch(char ch) {
  __am_uart_init_once();
  while ((inb(SERIAL_PORT + 5) & UART_LSR_THRE) == 0);
  outb(SERIAL_PORT, ch);
}

void halt(int code) {
  //while (1);
  ebreak();
  while (1);
}

void _trm_init() {
  unsigned int x;
  asm volatile ("csrr %0, 0xBC0" : "=r"(x));
  char *asc = (char*)&x;
  for (int i = 3; i >= 0; i --) {
    printf("%c", asc[i]);
  }
  printf("\n");

  asm volatile ("csrr %0, 0xBC1" : "=r"(x));
  printf("archid: %d\n", x);

  print_seg(x);
  
  __am_uart_init_once();


  // // 1. Data Relocation: 将 .data 从 MROM 复制到 SRAM
  // // ---------------------------------------------------------
  // char *src = &_data_lma;
  // char *dst = &_data_vma;
  // while(dst < &_data_vma_end) {
  //   *dst++ = *src++;
  // }

  // // 2. Clear BSS: 将 .bss 段清零 (通常 AM 的 start.S 可能做过，但这里做更保险)
  // // ---------------------------------------------------------
  // // 注意：如果你的 start.S 里已经清零了 BSS，这里可以省略，
  // // 但为了安全起见，建议保留。
  // dst = &_bss_start;
  // while(dst < &_bss_end) {
  //   *dst++ = 0;
  // }

  // 3. 执行 Main
  int ret = main(mainargs);
  halt(ret);
}

void ebreak() {
  asm volatile("ebreak");
}
