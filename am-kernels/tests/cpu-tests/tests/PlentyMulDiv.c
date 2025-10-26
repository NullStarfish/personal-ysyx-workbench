#include <am.h>
#include <klib.h>
#include <klib-macros.h>

// --- 辅助函数：打印32位十六进制数 ---
void puthex(uint32_t n) {
    char hex_chars[] = "0123456789abcdef";
    for (int i = 7; i >= 0; i--) {
        putch(hex_chars[(n >> (i * 4)) & 0xF]);
    }
}


// --- 辅助函数：格式化打印结果，减少重复代码 ---
void print_result(const char* name, uint32_t result, uint32_t expected) {
    putstr(name);
    putstr(": ");
    puthex(result);
    putstr(" (Expected: ");
    puthex(expected);
    putstr(")\n");
}

// =================================================================
// ---            您原始的测试序列 (保持不变)                 ---
// =================================================================
void run_mul_div_sequence() {
    putstr("--- Running Original Basic Test Sequence ---\n");
    // ... 您原始的代码完全不变 ...
    volatile int32_t in_a = 100, in_b = 7, in_c = -20, in_d = 3, in_e = 50;
    volatile int32_t res_mul, res_divu, res_remu, res_div_neg, res_rem_neg, res_combo;

    asm volatile (
        "mul   t0, %[a], %[b]\n\t"        // 100 * 7 = 700
        "divu  t1, %[a], %[b]\n\t"        // 100 / 7 = 14
        "remu  t2, %[a], %[b]\n\t"        // 100 % 7 = 2
        "mv    %[r_mul], t0\n\t"
        "mv    %[r_divu], t1\n\t"
        "mv    %[r_remu], t2\n\t"
        : [r_mul]  "=&r" (res_mul), [r_divu] "=&r" (res_divu), [r_remu] "=&r" (res_remu)
        : [a] "r" (in_a), [b] "r" (in_b) : "t0", "t1", "t2"
    );
    asm volatile (
        "div   t0, %[c], %[d]\n\t"        // -20 / 3 = -6
        "rem   t1, %[c], %[d]\n\t"        // -20 % 3 = -2
        "mv    %[r_div_neg], t0\n\t"
        "mv    %[r_rem_neg], t1\n\t"
        : [r_div_neg] "=&r" (res_div_neg), [r_rem_neg] "=&r" (res_rem_neg)
        : [c] "r" (in_c), [d] "r" (in_d) : "t0", "t1"
    );
    asm volatile (
        "mul   t0, %[e], %[b]\n\t"        // 50 * 7 = 350
        "div   t0, t0, %[d]\n\t"          // 350 / 3 = 116
        "mv    %[r_combo], t0\n\t"
        : [r_combo] "=&r" (res_combo)
        : [e] "r" (in_e), [b] "r" (in_b), [d] "r" (in_d) : "t0"
    );
    
    print_result("res_mul    ", res_mul, 0x000002bc);
    print_result("res_divu   ", res_divu, 0x0000000e);
    print_result("res_remu   ", res_remu, 0x00000002);
    print_result("res_div_neg", res_div_neg, 0xfffffffa);
    print_result("res_rem_neg", res_rem_neg, 0xfffffffe);
    print_result("res_combo  ", res_combo, 0x00000074);
    putstr("\n");
}

// =================================================================
// ---          新的高覆盖率综合测试序列 (新增)                 ---
// =================================================================
void run_comprehensive_tests() {
    putstr("--- Running Comprehensive Corner Case Tests ---\n");

    // --- 输入变量 ---
    volatile int32_t pos_large = 2000000000;  // 0x77359400
    volatile int32_t neg_large = -2000000000; // 0x88ca6c00
    volatile int32_t zero = 0;
    volatile int32_t one = 1;
    volatile int32_t neg_one = -1;
    volatile int32_t min_int = 0x80000000;

    // --- 输出变量 ---
    volatile int32_t r1, r2, r3, r4, r5, r6, r7, r8, r9, r10, r11, r12;

    // --- 1. 符号组合测试 ---
    putstr("\n--- Sign Combination Tests ---\n");
    asm volatile ("mul %0, %1, %2" : "=r"(r1) : "r"(pos_large), "r"(neg_one)); // pos * neg
    print_result("pos * neg_one", r1, 0x88ca6c00);
    asm volatile ("mul %0, %1, %2" : "=r"(r2) : "r"(neg_large), "r"(neg_one)); // neg * neg
    print_result("neg * neg_one", r2, 0x77359400);

    // --- 2. 零值和单位值测试 ---
    putstr("\n--- Zero and Identity Tests ---\n");
    asm volatile ("mul %0, %1, %2" : "=r"(r3) : "r"(pos_large), "r"(zero)); // mul by zero
    print_result("mul by zero  ", r3, 0x00000000);
    asm volatile ("div %0, %1, %2" : "=r"(r4) : "r"(zero), "r"(pos_large)); // zero div by val
    print_result("zero / val   ", r4, 0x00000000);
    asm volatile ("rem %0, %1, %2" : "=r"(r5) : "r"(neg_large), "r"(one)); // rem by one
    print_result("rem by one   ", r5, 0x00000000);
    
    // --- 3. 规范定义的特殊情况：除以零 ---
    putstr("\n--- Spec Test: Division by Zero ---\n");
    asm volatile ("divu %0, %1, %2" : "=r"(r6) : "r"(pos_large), "r"(zero));
    print_result("val / zero (divu)", r6, 0xffffffff);
    asm volatile ("remu %0, %1, %2" : "=r"(r7) : "r"(pos_large), "r"(zero));
    print_result("val % zero (remu)", r7, 0x77359400);

    // --- 4. 规范定义的特殊情况：MIN_INT 溢出 ---
    putstr("\n--- Spec Test: MIN_INT Overflow ---\n");
    asm volatile ("div %0, %1, %2" : "=r"(r8) : "r"(min_int), "r"(neg_one));
    print_result("min_int / -1 (div)", r8, 0x80000000); // Should overflow and result in min_int
    asm volatile ("rem %0, %1, %2" : "=r"(r9) : "r"(min_int), "r"(neg_one));
    print_result("min_int % -1 (rem)", r9, 0x00000000); // Remainder should be 0

    // --- 5. 高位乘法测试 ---
    putstr("\n--- High-Word Multiplication Tests ---\n");
    // mulh: signed(min_int) * signed(2) -> upper 32 bits should be -1
    asm volatile ("mulh %0, %1, %2" : "=r"(r10) : "r"(min_int), "r"(2));
    print_result("mulh(min_int, 2) ", r10, 0xffffffff);
    
    // mulhu: unsigned(max_uint) * unsigned(max_uint) -> upper 32 bits should be max_uint - 1
    asm volatile ("mulhu %0, %1, %2" : "=r"(r11) : "r"(neg_one), "r"(neg_one));
    print_result("mulhu(-1, -1)", r11, 0xfffffffe);
    
    // mulhsu: signed(-1) * unsigned(2) -> upper 32 bits should be -1
    asm volatile ("mulhsu %0, %1, %2" : "=r"(r12) : "r"(neg_one), "r"(2));
    print_result("mulhsu(-1, 2)", r12, 0xffffffff);

    putstr("\n");
}


int main() {
    ioe_init();

    // 运行两个测试序列
    run_mul_div_sequence();
    run_comprehensive_tests();

    // 在结束前打印一个成功的标志
    putstr("All tests finished.\n");

    return 0;
}