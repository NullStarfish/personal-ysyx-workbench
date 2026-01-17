#include "am.h"
#include <klib.h>
#include <klib-macros.h>
#include <stdint.h>


#define SPI_MASTER_BASE 0x10001000
#define TX0 0x00
#define RX0 0x00
#define DIVIDER 0x14
#define CTRL 0x10
#define SS 0x18
int main() {
    *(uint32_t*)(SPI_MASTER_BASE + TX0) = 0x5500;
    *(uint32_t*)(SPI_MASTER_BASE + DIVIDER) = 0;
    *(uint32_t*)(SPI_MASTER_BASE + SS) = 0x80;
    *(uint32_t*)(SPI_MASTER_BASE + CTRL) = 0x2310;
    while(*(volatile uint32_t*)(SPI_MASTER_BASE + CTRL) & 0x100);
    printf("received: %x\n", *(uint32_t*)(SPI_MASTER_BASE + RX0));
}
