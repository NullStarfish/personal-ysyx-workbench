#include <am.h>
#include <klib.h>
#include <klib-macros.h>

void puthex(uint32_t n) {
    char hex_chars[] = "0123456789abcdef";
    for (int i = 7; i >= 0; i--) {
        putch(hex_chars[(n >> (i * 4)) & 0xF]);
    }
}

void run_mul_div_sequence() {
    // --- Input Values ---
    volatile int32_t in_a = 100;
    volatile int32_t in_b = 7;
    volatile int32_t in_c = -20;
    volatile int32_t in_d = 3;
    volatile int32_t in_e = 50;

    // --- Output Variables ---
    volatile int32_t res_mul;
    volatile int32_t res_divu;
    volatile int32_t res_remu;
    volatile int32_t res_div_neg;
    volatile int32_t res_rem_neg;
    volatile int32_t res_combo;

    // --- 解决方案：将一个大的asm块拆分为多个小的块，以减轻寄存器压力 ---

    // Part 1: 处理 in_a 和 in_b
    asm volatile (
        "mul   t0, %[a], %[b]\n\t"        // 100 * 7 = 700
        "divu  t1, %[a], %[b]\n\t"        // 100 / 7 = 14
        "remu  t2, %[a], %[b]\n\t"        // 100 % 7 = 2
        
        "mv    %[r_mul], t0\n\t"
        "mv    %[r_divu], t1\n\t"
        "mv    %[r_remu], t2\n\t"
        
        : [r_mul]  "=&r" (res_mul),
          [r_divu] "=&r" (res_divu),
          [r_remu] "=&r" (res_remu)
        : [a] "r" (in_a), [b] "r" (in_b)
        : "t0", "t1", "t2"
    );

    // Part 2: 处理 in_c 和 in_d
    asm volatile (
        "div   t0, %[c], %[d]\n\t"        // -20 / 3 = -6
        "rem   t1, %[c], %[d]\n\t"        // -20 % 3 = -2
        
        "mv    %[r_div_neg], t0\n\t"
        "mv    %[r_rem_neg], t1\n\t"

        : [r_div_neg] "=&r" (res_div_neg),
          [r_rem_neg] "=&r" (res_rem_neg)
        : [c] "r" (in_c), [d] "r" (in_d)
        : "t0", "t1"
    );

    // Part 3: 处理组合运算
    asm volatile (
        "mul   t0, %[e], %[b]\n\t"        // 50 * 7 = 350
        "div   t0, t0, %[d]\n\t"          // 350 / 3 = 116
        
        "mv    %[r_combo], t0\n\t"

        : [r_combo] "=&r" (res_combo)
        : [e] "r" (in_e), [b] "r" (in_b), [d] "r" (in_d)
        : "t0"
    );


    // --- 打印结果 (保持不变) ---
    const char* s;
    s = "--- Raw Hex Results ---\n";
    for (int i = 0; s[i]; i++) putch(s[i]);

    s = "res_mul:     "; for (int i = 0; s[i]; i++) putch(s[i]);
    puthex(res_mul);
    s = " (Expected: 000002bc)\n"; for (int i = 0; s[i]; i++) putch(s[i]);

    s = "res_divu:    "; for (int i = 0; s[i]; i++) putch(s[i]);
    puthex(res_divu);
    s = " (Expected: 0000000e)\n"; for (int i = 0; s[i]; i++) putch(s[i]);

    s = "res_remu:    "; for (int i = 0; s[i]; i++) putch(s[i]);
    puthex(res_remu);
    s = " (Expected: 00000002)\n"; for (int i = 0; s[i]; i++) putch(s[i]);
    
    s = "res_div_neg: "; for (int i = 0; s[i]; i++) putch(s[i]);
    puthex(res_div_neg);
    s = " (Expected: fffffffa -> -6)\n"; for (int i = 0; s[i]; i++) putch(s[i]);

    s = "res_rem_neg: "; for (int i = 0; s[i]; i++) putch(s[i]);
    puthex(res_rem_neg);
    s = " (Expected: fffffffe -> -2)\n"; for (int i = 0; s[i]; i++) putch(s[i]);

    s = "res_combo:   "; for (int i = 0; s[i]; i++) putch(s[i]);
    puthex(res_combo);
    s = " (Expected: 00000074 -> 116)\n"; for (int i = 0; s[i]; i++) putch(s[i]);
    
    s = "\n"; for (int i = 0; s[i]; i++) putch(s[i]);
}

int main() {
    ioe_init();
    run_mul_div_sequence();
    return 0;
}