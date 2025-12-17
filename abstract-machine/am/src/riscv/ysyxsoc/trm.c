#include <am.h>
#include <klib.h>
#include <klib-macros.h>
#include "../riscv.h"
#include "ysyxsoc.h"

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
#define PMEM_SIZE (128 * 1024 * 1024)
#define PMEM_END  ((uintptr_t)&_pmem_start + PMEM_SIZE)

Area heap = RANGE(&_heap_start, PMEM_END);
static const char mainargs[MAINARGS_MAX_LEN] = MAINARGS_PLACEHOLDER; // defined in CFLAGS



#define UART_LSR_THRE 0x20  // Transmitter Holding Register Empty

static bool init_uart = false;

// 简单的阻塞式发送，用于调试硬件问题
void putch_blocking(char ch) {
    // 1. 一直等待，直到发送保持寄存器为空 (FIFO Empty)
    // 如果卡在这里，说明波特率设置错误或时钟未使能
    while ((inb(SERIAL_PORT + 5) & UART_LSR_THRE) == 0 && (inb(SERIAL_PORT + 5) & 0x40) == 0 );
    
    // 2. 发送数据
    outb(SERIAL_PORT, ch);
}

void putch(char ch) {
  
    if (!init_uart) {
        // --- 初始化开始 ---
        
        // 1. 关闭中断 (防止初始化过程中触发中断)
        outb(SERIAL_PORT + 1, 0x00);

        // 2. 设置 DLAB=1 (允许访问波特率分频器)
        outb(SERIAL_PORT + 3, 0x83);

        // 3. 设置波特率
        // 注意：请根据你的实际时钟频率修改这里！
        // 如果是 QEMU/标准PC，Divisor 设为 1 (115200 baud)
        uint16_t divisor = 0x0001; 
        // 如果你需要 0x28B，请确保你确认过时钟频率
        // uint16_t divisor = 0x28B; 
        
        outb(SERIAL_PORT + 1, (divisor >> 8) & 0xFF); // High byte
        outb(SERIAL_PORT + 0, divisor & 0xFF);        // Low byte

        // 4. 设置 LCR (8位数据, 1停止位, 无校验) 并 **清除 DLAB**
        // 0x03 = 0000 0011 (DLAB=0)
        // 这一步至关重要！如果是 0xC0 会导致死锁。
        outb(SERIAL_PORT + 3, 0x03);

        // 5. 启用 FIFO, 清除 TX/RX FIFO, 设置触发阈值 14
        outb(SERIAL_PORT + 2, 0xC7);

        // 6. (可选) 启用中断
        // outb(SERIAL_PORT + 1, 0x01); // 仅启用接收中断

        init_uart = true;
        // --- 初始化结束 ---
    }

    // 使用简单的阻塞发送，先确保能打印出来
    putch_blocking(ch);
}

void halt(int code) {
  //while (1);
  ebreak();
  while (1);
}

void _trm_init() {
  // 1. Data Relocation: 将 .data 从 MROM 复制到 SRAM
  // ---------------------------------------------------------
  char *src = &_data_lma;
  char *dst = &_data_vma;
  while(dst < &_data_vma_end) {
    *dst++ = *src++;
  }

  // 2. Clear BSS: 将 .bss 段清零 (通常 AM 的 start.S 可能做过，但这里做更保险)
  // ---------------------------------------------------------
  // 注意：如果你的 start.S 里已经清零了 BSS，这里可以省略，
  // 但为了安全起见，建议保留。
  dst = &_bss_start;
  while(dst < &_bss_end) {
    *dst++ = 0;
  }

  // 3. 执行 Main
  int ret = main(mainargs);
  halt(ret);
}

void ebreak() {
  asm volatile("ebreak");
}