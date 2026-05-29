#include "am.h"
#include "klib.h"

#ifdef __ARCH_RISCV32E_YSYXSOC
#include <stdint.h>
#include "bootloader.h"
#include "ysyxsoc.h"

#define SDRAM_TEST_START 0xa0000000u
#define SDRAM_TEST_SPAN  0x01000000u
#define SDRAM_SEQ_SIZE   0x00001000u
#define SDRAM_RAND_ITERS 1024u

static BOOT_STAGE1_RODATA const char msg_begin[] = "[boot-mem-test] begin\n";
static BOOT_STAGE1_RODATA const char msg_pass[] = "[boot-mem-test] pass\n";
static BOOT_STAGE1_RODATA const char msg_seq[] = "[boot-mem-test] sequential\n";
static BOOT_STAGE1_RODATA const char msg_random[] = "[boot-mem-test] random\n";
static BOOT_STAGE1_RODATA const char msg_interleave[] = "[boot-mem-test] interleave\n";
static BOOT_STAGE1_RODATA const char msg_overlay[] = "[boot-mem-test] overlay\n";
static BOOT_STAGE1_RODATA const char msg_fail[] = "[boot-mem-test] fail addr=0x";
static BOOT_STAGE1_RODATA const char msg_got[] = " got=0x";
static BOOT_STAGE1_RODATA const char msg_exp[] = " exp=0x";
static BOOT_STAGE1_RODATA const char msg_nl[] = "\n";
static BOOT_STAGE1_RODATA const char hex_digits[] = "0123456789abcdef";


static BOOT_STAGE1_TEXT void boot_puthex32(uint32_t value) {
  for (int shift = 28; shift >= 0; shift -= 4) {
    boot_putch(hex_digits[(value >> shift) & 0xf]);
  }
}


static BOOT_STAGE1_TEXT void boot_fail(uint32_t addr, uint32_t got, uint32_t exp) {
  boot_puts(msg_fail);
  boot_puthex32(addr);
  boot_puts(msg_got);
  boot_puthex32(got);
  boot_puts(msg_exp);
  boot_puthex32(exp);
  boot_puts(msg_nl);
  boot_halt();
}

static BOOT_STAGE1_TEXT uint32_t xorshift32(uint32_t x) {
  x ^= x << 13;
  x ^= x >> 17;
  x ^= x << 5;
  return x;
}

static BOOT_STAGE1_TEXT uint32_t sdram_rand_addr(uint32_t seed, uint32_t align) {
  uint32_t mask = ~(align - 1u);
  return SDRAM_TEST_START + (seed & (SDRAM_TEST_SPAN - align) & mask);
}

static BOOT_STAGE1_TEXT uint8_t pattern8(uint32_t addr) {
  return (uint8_t)(addr ^ (addr >> 8) ^ 0xa5u);
}

static BOOT_STAGE1_TEXT uint16_t pattern16(uint32_t addr) {
  return (uint16_t)(addr ^ (addr >> 11) ^ 0x5a5au);
}

static BOOT_STAGE1_TEXT uint32_t pattern32(uint32_t addr) {
  return addr ^ (addr << 7) ^ (addr >> 9) ^ 0x9e3779b9u;
}

static BOOT_STAGE1_TEXT void mem_test_seq8_stage1(void) {
  volatile uint8_t *p = (volatile uint8_t *)SDRAM_TEST_START;

  for (uint32_t i = 0; i < SDRAM_SEQ_SIZE; i++) {
    uint32_t addr = SDRAM_TEST_START + i;
    p[i] = pattern8(addr);
  }

  for (uint32_t i = 0; i < SDRAM_SEQ_SIZE; i++) {
    uint32_t addr = SDRAM_TEST_START + i;
    uint8_t exp = pattern8(addr);
    uint8_t got = p[i];
    if (got != exp) {
      boot_fail(addr, got, exp);
    }
  }
}

static BOOT_STAGE1_TEXT void mem_test_seq16_stage1(void) {
  volatile uint16_t *p = (volatile uint16_t *)SDRAM_TEST_START;
  uint32_t count = SDRAM_SEQ_SIZE / sizeof(uint16_t);

  for (uint32_t i = 0; i < count; i++) {
    uint32_t addr = SDRAM_TEST_START + i * sizeof(uint16_t);
    p[i] = pattern16(addr);
  }

  for (uint32_t i = 0; i < count; i++) {
    uint32_t addr = SDRAM_TEST_START + i * sizeof(uint16_t);
    uint16_t exp = pattern16(addr);
    uint16_t got = p[i];
    if (got != exp) {
      boot_fail(addr, got, exp);
    }
  }
}

static BOOT_STAGE1_TEXT void mem_test_seq32_stage1(void) {
  volatile uint32_t *p = (volatile uint32_t *)SDRAM_TEST_START;
  uint32_t count = SDRAM_SEQ_SIZE / sizeof(uint32_t);

  for (uint32_t i = 0; i < count; i++) {
    uint32_t addr = SDRAM_TEST_START + i * sizeof(uint32_t);
    p[i] = pattern32(addr);
  }

  for (uint32_t i = 0; i < count; i++) {
    uint32_t addr = SDRAM_TEST_START + i * sizeof(uint32_t);
    uint32_t exp = pattern32(addr);
    uint32_t got = p[i];
    if (got != exp) {
      boot_fail(addr, got, exp);
    }
  }
}

static BOOT_STAGE1_TEXT void mem_test_random32_stage1(void) {
  uint32_t seed = 0x12345678u;

  for (uint32_t i = 0; i < SDRAM_RAND_ITERS; i++) {
    seed = xorshift32(seed);
    uint32_t addr = sdram_rand_addr(seed, 4);
    *(volatile uint32_t *)addr = pattern32(addr);
  }

  seed = 0x12345678u;
  for (uint32_t i = 0; i < SDRAM_RAND_ITERS; i++) {
    seed = xorshift32(seed);
    uint32_t addr = sdram_rand_addr(seed, 4);
    uint32_t exp = pattern32(addr);
    uint32_t got = *(volatile uint32_t *)addr;
    if (got != exp) {
      boot_fail(addr, got, exp);
    }
  }
}

static BOOT_STAGE1_TEXT void mem_test_interleave_stage1(void) {
  uint32_t seed = 0x87654321u;
  uint32_t prev_addr = 0;
  uint32_t prev_exp = 0;

  for (uint32_t i = 0; i < SDRAM_RAND_ITERS; i++) {
    if (i != 0) {
      uint32_t got = *(volatile uint32_t *)prev_addr;
      if (got != prev_exp) {
        boot_fail(prev_addr, got, prev_exp);
      }
    }

    seed = xorshift32(seed);
    uint32_t addr = sdram_rand_addr(seed, 4);
    uint32_t exp = pattern32(addr ^ i);
    *(volatile uint32_t *)addr = exp;

    prev_addr = addr;
    prev_exp = exp;
  }

  uint32_t got = *(volatile uint32_t *)prev_addr;
  if (got != prev_exp) {
    boot_fail(prev_addr, got, prev_exp);
  }
}

static BOOT_STAGE1_TEXT void mem_test_overlay_stage1(void) {
  volatile uint32_t *word = (volatile uint32_t *)(SDRAM_TEST_START + 0x2000u);
  volatile uint16_t *half = (volatile uint16_t *)word;
  volatile uint8_t *byte = (volatile uint8_t *)word;

  *word = 0x11223344u;
  if (*word != 0x11223344u) {
    boot_fail((uint32_t)word, *word, 0x11223344u);
  }

  half[0] = 0xa55au;
  if (*word != 0x1122a55au) {
    boot_fail((uint32_t)word, *word, 0x1122a55au);
  }

  half[1] = 0x5aa5u;
  if (*word != 0x5aa5a55au) {
    boot_fail((uint32_t)word, *word, 0x5aa5a55au);
  }

  byte[0] = 0xc3u;
  byte[1] = 0x3cu;
  byte[2] = 0x96u;
  byte[3] = 0x69u;
  if (*word != 0x69963cc3u) {
    boot_fail((uint32_t)word, *word, 0x69963cc3u);
  }

  for (uint32_t i = 0; i < 16; i++) {
    uint32_t addr = SDRAM_TEST_START + 0x2100u + i * 4u;
    *(volatile uint32_t *)addr = pattern32(addr);
    *(volatile uint8_t *)(addr + 1) = pattern8(addr + 1);
    *(volatile uint16_t *)(addr + 2) = pattern16(addr + 2);
    uint32_t exp = (pattern32(addr) & 0x000000ffu) |
                   ((uint32_t)pattern8(addr + 1) << 8) |
                   ((uint32_t)pattern16(addr + 2) << 16);
    uint32_t got = *(volatile uint32_t *)addr;
    if (got != exp) {
      boot_fail(addr, got, exp);
    }
  }
}

BOOT_STAGE1_TEXT int __am_bootloader_plugin(void) {
  boot_uart_init();
  boot_puts(msg_begin);
  boot_puts(msg_seq);
  mem_test_seq8_stage1();
  mem_test_seq16_stage1();
  mem_test_seq32_stage1();
  boot_puts(msg_random);
  mem_test_random32_stage1();
  boot_puts(msg_interleave);
  mem_test_interleave_stage1();
  boot_puts(msg_overlay);
  mem_test_overlay_stage1();
  boot_puts(msg_pass);
  boot_halt();
  return 1;
}

int main() {
  return 0;
}

#else
// 假设 SRAM 范围
#define SRAM_BASE 0xa0000000
#define SRAM_END  0xa00000ff

// 栈区保护：假设给栈留出 4KB 空间，其余部分作为堆区进行测试
#define STACK_SIZE 0x0000 
#define TEST_START SRAM_BASE
#define TEST_END   (SRAM_END - STACK_SIZE)

void mem_test_8() {
    uint8_t *p = (uint8_t *)TEST_START;
    uint32_t len = TEST_END - TEST_START;
    uint8_t mask = 0xFF;

    // 1. 写入阶段
    for (uint32_t i = 0; i < len; i++) {
        uintptr_t addr = (uintptr_t)&p[i];
        p[i] = (uint8_t)(addr & mask);
        printf("[mem-test] write %x\n", addr & mask);
    }

    // 2. 校验阶段
    for (uint32_t i = 0; i < len; i++) {
        uintptr_t addr = (uintptr_t)&p[i];
        uint8_t target = (uint8_t)(addr & mask);
        printf("[mem-test] read %x, target %x\n", p[i], target);
        assert(p[i] == target);
    }
}

void mem_test_16() {
    // 确保地址对齐到 2 字节
    uint16_t *p = (uint16_t *)TEST_START;
    uint32_t count = (TEST_END - TEST_START) / sizeof(uint16_t);
    uint16_t mask = 0xFFFF;

    for (uint32_t i = 0; i < count; i++) {
        uintptr_t addr = (uintptr_t)&p[i];
        p[i] = (uint16_t)(addr & mask);
    }

    for (uint32_t i = 0; i < count; i++) {
        uintptr_t addr = (uintptr_t)&p[i];
        assert(p[i] == (uint16_t)(addr & mask));
    }
}

void mem_test_32() {
    // 确保地址对齐到 4 字节
    uint32_t *p = (uint32_t *)TEST_START;
    uint32_t count = (TEST_END - TEST_START) / sizeof(uint32_t);
    uint32_t mask = 0xFFFFFFFF;

    for (uint32_t i = 0; i < count; i++) {
        uintptr_t addr = (uintptr_t)&p[i];
        p[i] = (uint32_t)(addr & mask);
    }

    for (uint32_t i = 0; i < count; i++) {
        uintptr_t addr = (uintptr_t)&p[i];
        assert(p[i] == (uint32_t)(addr & mask));
    }
}

int main() {
    // 依次执行不同位宽的测试
    // 注意：每次测试都会覆盖前一次的数据
    printf("test begin");
    mem_test_8();
    mem_test_16();
    mem_test_32();

    // 如果支持 64 位也可以加上 mem_test_64
    
    // 如果运行到这里没有触发 assert，说明测试通过
    return 0;
}
#endif
