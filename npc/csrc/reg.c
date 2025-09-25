#include <stdio.h>
#include <stdlib.h>
#include "reg.h"

// --- 由 C++ 桥接文件 (main.cpp) 提供的底层硬件接口 ---
uint32_t isa_reg_read_cpp(int reg_num);
uint32_t get_pc_cpp();
uint32_t isa_reg_str2val_cpp(const char *s, bool *success);
uint32_t isa_get_csrs(int csr_num);

void isa_reg_display() {
    const char* abi_names[32] = {
      "zero", "ra", "sp", "gp", "tp", "t0", "t1", "t2",
      "s0", "s1", "a0", "a1", "a2", "a3", "a4", "a5",
      "a6", "a7", "s2", "s3", "s4", "s5", "s6", "s7",
      "s8", "s9", "s10", "s11", "t3", "t4", "t5", "t6"
    };
    for (int i = 0; i < 32; i++) {
        printf("  $%-4s (x%-2d) = 0x%08x\n", abi_names[i], i, isa_reg_read_cpp(i));
    }
    printf("  $pc       = 0x%08x\n", get_pc_cpp());
    printf("$mstatus = 0x%08x\n", isa_get_csrs(0x300));
    printf("$mtvec  = 0x%08x\n", isa_get_csrs(0x305));
    printf("$mepc   = 0x%08x\n", isa_get_csrs(0x341));
    printf("$mcause = 0x%08x\n", isa_get_csrs(0x342));
}

uint32_t isa_reg_str2val(const char *s, bool *success) {
    // 调用 C++ 中的辅助函数来处理 map 和 string
    return isa_reg_str2val_cpp(s, success);
}
