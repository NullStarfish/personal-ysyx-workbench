#ifndef AM_RISCV_NPC_H
#define AM_RISCV_NPC_H

#define PMEM_BASE       0xa0000000L
#define DEVICE_BASE     0x10000000L
#define MMIO_BASE       DEVICE_BASE

#define SERIAL_PORT     0x10000000L
#define UART_LSR        (SERIAL_PORT + 5)
#define UART_LSR_DR     0x01
#define UART_LSR_THRE   0x20

#define RTC_UP_ADDR     0x02000000L
#define RTC_ADDR        0x02000008L

#endif
