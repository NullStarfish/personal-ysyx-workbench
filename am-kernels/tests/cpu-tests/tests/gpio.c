#define GPIO_BASE 0x10002000
#include "am.h"
int main() {
    *(uint32_t*)(GPIO_BASE) = 0x0000FFFF;
}