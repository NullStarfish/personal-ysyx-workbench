#include "am.h"
#include <klib.h>
#include <klib-macros.h>

#define SPI_MASTER_BASE 0x10001000
#define TX0 0x00
#define RX0 0x00
#define DIVIDER 0x14
#define CTRL 0x10
#define SS 0x18

uint32_t flash_read(uint32_t addr) {
    *(uint32_t*)(SPI_MASTER_BASE + TX0) = 0x00;
    *(uint32_t*)(SPI_MASTER_BASE + TX0 + 0x04) = (0x03 << 24) | (addr & 0x00FFFFFF);
    *(uint32_t*)(SPI_MASTER_BASE + DIVIDER) = 0;
    *(uint32_t*)(SPI_MASTER_BASE + SS) = 0x01;
    *(uint32_t*)(SPI_MASTER_BASE + CTRL) = 0x2540;
    while(*(volatile uint32_t*)(SPI_MASTER_BASE + CTRL) & 0x100);
    return *(uint32_t*)(SPI_MASTER_BASE + RX0);
}





int main() {
    for (int i = 0; i < 2; i++) {
        int a = *(uint32_t*)(0x30000000 + 4*i);
        //int a = flash_read(0x30000000 + 4 * i);
        printf("flash : %d\n", a);

    }
}