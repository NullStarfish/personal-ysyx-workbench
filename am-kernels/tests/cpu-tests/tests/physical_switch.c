#define GPIO_BASE 0x10002000
#define GPIO_IN (*(volatile unsigned int *)(GPIO_BASE + 0x4))

#include <klib.h>

int main() {
    unsigned int last_status = GPIO_IN;
    printf("cur: %x\n", last_status);

    while (1) {
        unsigned int cur_status = GPIO_IN;
        if (cur_status != last_status) {
            printf("cur: %x\n", cur_status);
            last_status = cur_status;
        }
    }
}
