#include "am.h"
#include "klib.h"
// 假设 SRAM 范围
#define SRAM_BASE 0x0f000000
#define SRAM_END  0x0fffffff

// 栈区保护：假设给栈留出 4KB 空间，其余部分作为堆区进行测试
#define STACK_SIZE 0x1000 
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
    }

    // 2. 校验阶段
    for (uint32_t i = 0; i < len; i++) {
        uintptr_t addr = (uintptr_t)&p[i];
        uint8_t target = (uint8_t)(addr & mask);
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