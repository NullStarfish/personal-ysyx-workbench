#include "klib.h"
#include "am.h"
#include <klib-macros.h>

int main() {
    asm volatile (
        "lui a1, 0x12\n"
        "addi a1, a1, 0x345\n"
        "lui a0, 0xf001\n"
        "sb  a1, 1(a0)\n"      // 0x001(a0) 写成 1(a0) 即可
        "lb  a2, 1(a0)\n"
    );
    return 0;
}